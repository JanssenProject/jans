/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceBulkIngestionResponse;
import io.jans.lock.model.trace.api.TraceBulkResultEntry;
import io.jans.lock.model.trace.api.TraceErrorResponse;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceIngestionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.api.TraceVerificationResponse;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.error.TraceErrors;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.TraceIngestionService;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
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
 * verification/correlation/receipt-allocation/storage or read via {@link TraceRetrievalService}
 * (tasks 12, 21, 22), and map the outcome to the §11.1/§11.2/§11.3 response shapes. Every
 * {@link RuntimeException} raised by the TRACE packages is translated to the structured error body
 * by {@link TraceErrors#toWebApplicationException}.
 *
 * @author Yuriy Movchan
 */
@Dependent
public class TraceRestWebServiceImpl extends BaseResource implements TraceRestWebService {

	private static final String REASON_TRACE_DISABLED = "trace_disabled";

	private static final String REASON_NOT_ARRAY = "not_array";

	private static final String REASON_EMPTY_BULK_RECORDS = "empty_bulk_records";

	private static final String REASON_MAX_BULK_RECORDS_EXCEEDED = "max_bulk_records_exceeded";

	private static final String REASON_BULK_BODY_TOO_LARGE = "bulk_body_too_large";

	private static final String REASON_UNREADABLE_BODY = "unreadable_body";

	private static final String REASON_MALFORMED_UTF8 = "malformed_utf8";

	private static final String REASON_MALFORMED_JSON = "malformed_json";

	private static final String REASON_TRAILING_CONTENT = "trailing_content";

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
	private TraceRetrievalService retrievalService;

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
	public Response submitBulkRecords(InputStream body, HttpServletRequest request, SecurityContext sec) {
		AuditLogEntry auditLogEntry = new AuditLogEntry(getClientIpAddress(), AuditActionType.TRACE_BULK_WRITE);

		Response response = null;
		try {
			log.debug("Submitting TRACE bulk records");
			checkEnabled();

			TraceConfiguration config = appConfiguration.getTraceConfiguration();
			SubmitterIdentity identity = submitterIdentityService.resolve(getHttpRequest());
			TraceRequestContext context = evidenceDomainResolver.resolve(identity);

			List<String> elements = splitBulkElements(body, config);

			List<TraceBulkResultEntry> results = new ArrayList<>(elements.size());
			for (int index = 0; index < elements.size(); index++) {
				results.add(ingestOne(index, elements.get(index), context));
			}

			TraceBulkIngestionResponse dto = new TraceBulkIngestionResponse();
			dto.setResults(results);

			response = Response.status(Response.Status.ACCEPTED)
					.cacheControl(ServerUtil.cacheControlWithNoStoreTransformAndPrivate())
					.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).entity(dto).build();
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
			StoredTraceRecord record = retrievalService.findRecord(context.getEvidenceDomainId(), recordId, producerId);

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
			checlStart(start);
			requireMaxLength(traceExecutionId, MAX_EXECUTION_ID_LENGTH, "trace_execution_id");
			if (StringUtils.isNotBlank(executionAuthority)) {
				requireMaxLength(executionAuthority, MAX_EXECUTION_ID_LENGTH, "execution_authority");
			}
			requireCountInRange(count);
			TraceRequestContext context = resolveContext();
			String domainId = context.getEvidenceDomainId();
			TraceRetrievalService.ExecutionResult result = retrievalService.getExecution(domainId, traceExecutionId,
					executionAuthority, start, count);

			response = Response
					.ok(toExecutionResponse(result.getExecutionAuthority(), traceExecutionId, result.getRecords()))
					.cacheControl(ServerUtil.cacheControl(true)).header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	private void checlStart(int start) {
		if (start < 0) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "start_out_of_range");
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

	private AuditLogEntry newAuditLogEntry(AuditActionType actionType) {
		return new AuditLogEntry(getClientIpAddress(), actionType);
	}

	private String getClientIpAddress() {
		HttpServletRequest request = getHttpRequest();
		return request == null ? null : InetAddressUtility.getIpAddress(request);
	}

	// -- bulk ingestion (design decision D-16) -----------------------------------------------------

	/**
	 * Verifies and ingests one array element, never throwing: a failure of {@code ingestionService}
	 * becomes a per-item {@code error} entry instead of aborting the batch (D-16 continue-on-error).
	 *
	 * @param elementRaw the element's exact source text, sliced by {@link #splitBulkElements}; fed
	 *                   to the same ingestion pipeline a standalone submission would use, so
	 *                   raw-evidence immutability and the D-13 digest/signature hold per item
	 */
	private TraceBulkResultEntry ingestOne(int index, String elementRaw, TraceRequestContext context) {
		TraceBulkResultEntry entry = new TraceBulkResultEntry();
		entry.setIndex(index);
		try {
			InputStream slice = new ByteArrayInputStream(elementRaw.getBytes(StandardCharsets.UTF_8));
			AcceptanceResult result = ingestionService.ingest(slice, context);
			entry.setAccepted(true);
			entry.setRecord(toResponse(result));
		} catch (RuntimeException ex) {
			entry.setAccepted(false);
			entry.setError(toErrorDto(ex));
		}
		return entry;
	}

	private TraceErrorResponse toErrorDto(RuntimeException ex) {
		TraceErrors.Classification classification = TraceErrors.classify(ex);
		DefaultErrorResponse built = errorResponseFactory.buildErrorResponse(classification.getType(),
				classification.getReason());

		TraceErrorResponse dto = new TraceErrorResponse();
		dto.setError(built.getErrorCode());
		dto.setErrorDescription(built.getErrorDescription());
		dto.setReason(built.getReason());
		return dto;
	}

	/**
	 * Splits a {@code POST /audit/trace/bulk} body into the exact source text of each top-level
	 * array element (design decision D-16), so every element can be re-fed verbatim to the
	 * single-record pipeline. Whole-request failures only: not an array, wrong element count, body
	 * too large, malformed JSON/UTF-8 &mdash; a structurally invalid individual element (e.g. not a
	 * JSON object) is left for {@link #ingestOne} to report as that item's own error.
	 */
	private List<String> splitBulkElements(InputStream body, TraceConfiguration config) {
		String rawText = readBoundedUtf8(body, config.getMaxBulkRequestBytes());

		JsonFactory factory = new JsonFactory();
		try (JsonParser parser = factory.createParser(rawText)) {
			if (parser.nextToken() != JsonToken.START_ARRAY) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_NOT_ARRAY);
			}

			List<String> elements = new ArrayList<>();
			JsonToken token;
			while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
				long startOffset = parser.getTokenLocation().getCharOffset();
				parser.skipChildren();
				long endOffset = parser.getCurrentLocation().getCharOffset();
				elements.add(rawText.substring((int) startOffset, (int) endOffset));

				if (elements.size() > config.getMaxBulkRecords()) {
					throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST,
							REASON_MAX_BULK_RECORDS_EXCEEDED);
				}
			}
			if (token == null) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_MALFORMED_JSON);
			}
			if (elements.isEmpty()) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_EMPTY_BULK_RECORDS);
			}
			if (parser.nextToken() != null) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_TRAILING_CONTENT);
			}
			return elements;
		} catch (IOException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_MALFORMED_JSON, ex);
		}
	}

	private String readBoundedUtf8(InputStream body, long maxBytes) {
		@SuppressWarnings("deprecation")
		BoundedInputStream bounded = new BoundedInputStream(body, maxBytes + 1);
		byte[] bytes;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(maxBytes + 1, 1 << 16));
			IOUtils.copy(bounded, out);
			bytes = out.toByteArray();
		} catch (IOException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_UNREADABLE_BODY, ex);
		}
		if (bytes.length > maxBytes) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_BULK_BODY_TOO_LARGE);
		}

		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			return decoder.decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_MALFORMED_UTF8, ex);
		}
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
