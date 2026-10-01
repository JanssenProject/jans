/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceIngestionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.api.TraceVerificationResponse;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceErrors;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.TraceIngestionService;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.ws.rs.base.BaseResource;
import io.jans.lock.util.ServerUtil;
import io.jans.net.InetAddressUtility;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * REST implementation for {@code POST /audit/trace} (design §11.1, TRACE MVP task 20) and for
 * {@code GET /audit/trace/records/{record_id}} / {@code GET /audit/trace/executions/{trace_execution_id}}
 * (design §11.2, §11.3, TRACE MVP task 21): resolve the submitting client and its evidence domain
 * (task 11), hand the raw body to {@link TraceIngestionService} (task 19) for
 * verification/correlation/receipt-allocation/storage or read directly from the {@link TraceStore}
 * (task 12), and map the outcome to the §11.1/§11.2/§11.3 response shapes. Every
 * {@link RuntimeException} raised by the TRACE packages is translated to the structured error body
 * by {@link TraceErrors#toWebApplicationException}.
 *
 * @author Yuriy Movchan
 */
@Dependent
public class TraceRestWebServiceImpl extends BaseResource implements TraceRestWebService {

	private static final String REASON_TRACE_DISABLED = "trace_disabled";

	private static final String REASON_RECORD_NOT_FOUND = "record_not_found";

	private static final String REASON_EXECUTION_NOT_FOUND = "execution_not_found";

	private static final String REASON_MULTIPLE_PRODUCERS = "multiple_producers";

	private static final String REASON_MULTIPLE_AUTHORITIES = "multiple_authorities";

	private static final int MAX_RECORD_ID_LENGTH = 128;

	private static final int MAX_EXECUTION_ID_LENGTH = 255;

	private static final int MIN_COUNT = 1;

	private static final int MAX_COUNT = 1000;

	private static final ObjectMapper ASSERTION_MAPPER = new ObjectMapper();

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ErrorResponseFactory errorResponseFactory;

	@Inject
	private SubmitterIdentityService submitterIdentityService;

	@Inject
	private EvidenceDomainResolver evidenceDomainResolver;

	@Inject
	private TraceIngestionService ingestionService;

	@Inject
	private TraceStore traceStore;

	@Inject
	private ApplicationAuditLogger applicationAuditLogger;

	@Override
	public Response submitRecord(InputStream body, HttpServletRequest request, SecurityContext sec) {
		AuditLogEntry auditLogEntry = new AuditLogEntry(getClientIpAddress(), AuditActionType.TRACE_WRITE);

		Response response = null;
		try {
			log.debug("Submitting TRACE record");
			checkEnabled();

			SubmitterIdentity identity = submitterIdentityService.resolve(getHttpRequest());
			TraceRequestContext context = evidenceDomainResolver.resolve(identity);

			AcceptanceResult result = ingestionService.ingest(body, context);

			response = Response.status(Response.Status.ACCEPTED)
					.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
					.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).entity(toResponse(result)).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response getRecord(String recordId, String producerId) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_RECORD_READ);

		Response response = null;
		try {
			log.debug("Retrieving TRACE record, recordId: {}", recordId);
			checkEnabled();
			requireMaxLength(recordId, MAX_RECORD_ID_LENGTH, "record_id");

			TraceRequestContext context = resolveContext();
			StoredTraceRecord record = findRecord(context.getEvidenceDomainId(), recordId, producerId);

			response = Response.ok(toRecordResponse(record)).cacheControl(ServerUtil.cacheControl(true))
					.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response getExecution(String traceExecutionId, String executionAuthority, int start, int count) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_EXECUTION_READ);

		Response response = null;
		try {
			log.debug("Retrieving TRACE execution, traceExecutionId: {}", traceExecutionId);
			checkEnabled();
			requireMaxLength(traceExecutionId, MAX_EXECUTION_ID_LENGTH, "trace_execution_id");
			if (StringUtils.isNotBlank(executionAuthority)) {
				requireMaxLength(executionAuthority, MAX_EXECUTION_ID_LENGTH, "execution_authority");
			}
			requireCountInRange(count);

			TraceRequestContext context = resolveContext();
			String domainId = context.getEvidenceDomainId();
			String authority = StringUtils.isNotBlank(executionAuthority) ? executionAuthority
					: resolveSoleAuthority(domainId, traceExecutionId);

			ExecutionIdentity exec = new ExecutionIdentity(domainId, authority, traceExecutionId);
			List<StoredTraceRecord> records = traceStore.findRecordsByExecution(exec, start, count);
			if (records.isEmpty() && start == 0) {
				throw new TraceValidationException(TraceErrorResponseType.EXECUTION_NOT_FOUND, REASON_EXECUTION_NOT_FOUND);
			}

			response = Response.ok(toExecutionResponse(authority, traceExecutionId, records))
					.cacheControl(ServerUtil.cacheControl(true)).header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	// -- request flow helpers --------------------------------------------------------------------

	private void checkEnabled() {
		if (!appConfiguration.getTraceConfiguration().isEnabled()) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_TRACE_DISABLED);
		}
	}

	private TraceRequestContext resolveContext() {
		SubmitterIdentity identity = submitterIdentityService.resolve(getHttpRequest());
		return evidenceDomainResolver.resolve(identity);
	}

	private void requireMaxLength(String value, int maxLength, String fieldName) {
		if (value != null && value.length() > maxLength) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "too_long:" + fieldName);
		}
	}

	private void requireCountInRange(int count) {
		if (count < MIN_COUNT || count > MAX_COUNT) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "count_out_of_range");
		}
	}

	private StoredTraceRecord findRecord(String domainId, String recordId, String producerId) {
		if (StringUtils.isNotBlank(producerId)) {
			RecordIdentity identity = new RecordIdentity(domainId, producerId, recordId);
			return traceStore.findRecord(identity).orElseThrow(
					() -> new TraceValidationException(TraceErrorResponseType.RECORD_NOT_FOUND, REASON_RECORD_NOT_FOUND));
		}

		List<StoredTraceRecord> candidates = traceStore.findRecordsByBareRecordId(domainId, recordId);
		if (candidates.isEmpty()) {
			throw new TraceValidationException(TraceErrorResponseType.RECORD_NOT_FOUND, REASON_RECORD_NOT_FOUND);
		}
		long distinctProducers = candidates.stream().map(r -> r.getIdentity().getProducerId()).distinct().count();
		if (distinctProducers > 1) {
			throw new TraceConflictException(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, REASON_MULTIPLE_PRODUCERS);
		}
		return candidates.get(0);
	}

	private String resolveSoleAuthority(String domainId, String traceExecutionId) {
		List<String> authorities = traceStore.findExecutionAuthorities(domainId, traceExecutionId);
		if (authorities.isEmpty()) {
			throw new TraceValidationException(TraceErrorResponseType.EXECUTION_NOT_FOUND, REASON_EXECUTION_NOT_FOUND);
		}
		if (authorities.size() > 1) {
			throw new TraceConflictException(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, REASON_MULTIPLE_AUTHORITIES);
		}
		return authorities.get(0);
	}

	private AuditLogEntry newAuditLogEntry(AuditActionType actionType) {
		return new AuditLogEntry(getClientIpAddress(), actionType);
	}

	private String getClientIpAddress() {
		HttpServletRequest request = getHttpRequest();
		return request == null ? null : InetAddressUtility.getIpAddress(request);
	}

	// -- DTO mapping ------------------------------------------------------------------------------

	private TraceAcceptanceResponse toResponse(AcceptanceResult result) {
		TraceAcceptanceResponse dto = new TraceAcceptanceResponse();
		dto.setAccepted(result.isAccepted());
		dto.setProducerId(result.getProducerId());
		dto.setRecordId(result.getRecordId());
		dto.setContentDigest(result.getContentDigest());
		dto.setReceiptSequence(result.getReceiptSequence());
		dto.setReceivedAt(Instant.ofEpochMilli(result.getReceivedAtMs()).toString());
		dto.setIdempotentReplay(result.isIdempotentReplay());
		dto.setCoverageGapFlag(result.isCoverageGapFlag());
		dto.setChainLinkFailureFlag(result.isChainLinkFailureFlag());
		dto.setEquivocationFlag(result.isEquivocationFlag());
		dto.setLateFlag(result.isLateFlag());
		return dto;
	}

	private TraceExecutionResponse toExecutionResponse(String executionAuthority, String traceExecutionId,
			List<StoredTraceRecord> records) {
		TraceExecutionResponse dto = new TraceExecutionResponse();
		dto.setExecutionAuthority(executionAuthority);
		dto.setTraceExecutionId(traceExecutionId);
		dto.setRecords(records.stream().map(this::toRecordResponse).collect(Collectors.toList()));
		return dto;
	}

	private TraceRecordResponse toRecordResponse(StoredTraceRecord record) {
		TraceRecordResponse dto = new TraceRecordResponse();
		dto.setProducerId(record.getIdentity().getProducerId());
		dto.setRecordId(record.getIdentity().getRecordId());
		dto.setAssertion(parseAssertion(record.getAssertionRaw()));
		dto.setContentDigest(record.getContentDigest());
		dto.setVerification(toVerificationResponse(record.getVerification()));
		dto.setIngestion(toIngestionResponse(record.getReceipt(), record.getFlags()));
		return dto;
	}

	private TraceVerificationResponse toVerificationResponse(VerificationResult verification) {
		TraceVerificationResponse dto = new TraceVerificationResponse();
		dto.setSignatureValid(verification.isSignatureValid());
		dto.setKeyId(verification.getKeyId());
		dto.setVerifiedAt(Instant.ofEpochMilli(verification.getVerifiedAtMs()).toString());
		dto.setAlgorithm(verification.getAlgorithm());
		return dto;
	}

	private TraceIngestionResponse toIngestionResponse(ReceiptEntry receipt, IngestionFlags flags) {
		TraceIngestionResponse dto = new TraceIngestionResponse();
		dto.setReceiptSequence(receipt.getReceiptSequence());
		dto.setReceivedAt(Instant.ofEpochMilli(receipt.getReceivedAtMs()).toString());
		dto.setPrevReceiptHash(receipt.getPrevReceiptHash());
		dto.setReceiptHash(receipt.getReceiptHash());
		dto.setCoverageGapFlag(flags.isCoverageGap());
		dto.setChainLinkFailureFlag(flags.isChainLinkFailure());
		dto.setEquivocationFlag(flags.isEquivocation());
		dto.setLateFlag(flags.isLate());
		return dto;
	}

	private JsonNode parseAssertion(String assertionRaw) {
		try {
			return ASSERTION_MAPPER.readTree(assertionRaw);
		} catch (IOException ex) {
			throw new TraceStorageException("assertion_parse_failure", "Stored assertion failed to re-parse", ex);
		}
	}

}
