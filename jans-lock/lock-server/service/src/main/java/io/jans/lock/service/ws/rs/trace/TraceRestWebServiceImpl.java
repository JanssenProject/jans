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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
import com.fasterxml.jackson.core.StreamReadConstraints;
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
import io.jans.lock.model.trace.api.TraceRecordSearchResponse;
import io.jans.lock.model.trace.api.TraceVerificationResponse;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.trace.crypto.TraceTime;
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
import io.jans.lock.service.trace.model.PolicyRecordQuery;
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
 * REST implementation for {@code POST /audit/trace} (design §11.1) and for
 * {@code GET /audit/trace/records/{record_id}} / {@code GET /audit/trace/executions/{trace_execution_id}}
 * (design §11.2, §11.3) and the policy store release search {@code GET /audit/trace/records}: resolve the submitting client and its evidence domain,
 * hand the raw body to {@link TraceIngestionService} for
 * verification/correlation/receipt-allocation/storage or read via {@link TraceRetrievalService},
 * and map the outcome to the §11.1/§11.2/§11.3 response shapes. Every
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

	private static final String REASON_BULK_MIXED_PRODUCERS = "bulk_mixed_producers";

	private static final String FIELD_PRODUCER = "producer";

	private static final String FIELD_RECORD_ID = "record_id";

	private static final int STATUS_MULTI_STATUS = 207;

	private static final String REASON_UNREADABLE_BODY = "unreadable_body";

	private static final String REASON_MALFORMED_UTF8 = "malformed_utf8";

	private static final String REASON_MALFORMED_JSON = "malformed_json";

	private static final String REASON_TRAILING_CONTENT = "trailing_content";

	private static final int MAX_RECORD_ID_LENGTH = 128;

	private static final int MAX_EXECUTION_ID_LENGTH = 255;

	private static final int MAX_POLICY_FIELD_LENGTH = 255;

	private static final String REASON_MISSING_POLICY_FILTER = "missing:policy_store_id";

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
			List<ElementIdentifiers> identifiers = scanElementIdentifiers(elements, config);
			requireSingleProducer(identifiers);

			List<TraceBulkResultEntry> results = new ArrayList<>(elements.size());
			for (int index = 0; index < elements.size(); index++) {
				results.add(ingestOne(index, elements.get(index), identifiers.get(index), context));
			}

			TraceBulkIngestionResponse dto = new TraceBulkIngestionResponse();
			dto.setResults(results);

			response = Response.status(STATUS_MULTI_STATUS)
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
			requireMaxLength(recordId, MAX_RECORD_ID_LENGTH, FIELD_RECORD_ID);

			TraceRequestContext context = resolveContext();
			StoredTraceRecord stored = retrievalService.findRecord(context.getEvidenceDomainId(), recordId, producerId);

			response = Response.ok(toRecordResponse(stored)).cacheControl(ServerUtil.cacheControl(true))
					.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	@Override
	public Response searchRecords(String policyStoreId, String policyStoreVersion, String bundleHash,
			String receivedFrom, String receivedTo, int start, int count) {
		AuditLogEntry auditLogEntry = newAuditLogEntry(AuditActionType.TRACE_RECORD_READ);

		Response response = null;
		try {
			log.debug("Searching TRACE records, policyStoreId: {}, policyStoreVersion: {}, bundleHash: {}",
					policyStoreId, policyStoreVersion, bundleHash);
			checkEnabled();
			checlStart(start);
			requireCountInRange(count);
			String storeId = StringUtils.trimToNull(policyStoreId);
			String version = StringUtils.trimToNull(policyStoreVersion);
			String hash = StringUtils.trimToNull(bundleHash);
			if (storeId == null && hash == null) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, REASON_MISSING_POLICY_FILTER);
			}
			requireMaxLength(storeId, MAX_POLICY_FIELD_LENGTH, "policy_store_id");
			requireMaxLength(version, MAX_POLICY_FIELD_LENGTH, "policy_store_version");
			requireMaxLength(hash, MAX_POLICY_FIELD_LENGTH, "bundle_hash");
			Long fromMs = parseTimestamp(receivedFrom, "received_from");
			Long toMs = parseTimestamp(receivedTo, "received_to");
			if (fromMs != null && toMs != null && fromMs > toMs) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "invalid:received_to");
			}

			TraceRequestContext context = resolveContext();
			List<StoredTraceRecord> records = retrievalService.findRecordsByPolicy(context.getEvidenceDomainId(),
					new PolicyRecordQuery(storeId, version, hash, fromMs, toMs), start, count);

			TraceRecordSearchResponse dto = new TraceRecordSearchResponse();
			dto.setRecords(records.stream().map(this::toRecordResponse).collect(Collectors.toList()));
			response = Response.ok(dto).cacheControl(ServerUtil.cacheControl(true))
					.header(ServerUtil.PRAGMA, ServerUtil.NO_CACHE).build();
			return response;
		} catch (RuntimeException ex) {
			throw TraceErrors.toWebApplicationException(ex, errorResponseFactory);
		} finally {
			applicationAuditLogger.log(auditLogEntry, getResponseResult(response));
		}
	}

	private static Long parseTimestamp(String value, String fieldName) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value.trim()).toInstant().toEpochMilli();
		} catch (DateTimeParseException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST, "invalid:" + fieldName, ex);
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

	// -- bulk ingestion -----------------------------------------------------

	/**
	 * Verifies and ingests one array element, never throwing: a failure of {@code ingestionService}
	 * becomes a per-item {@code error} entry instead of aborting the batch (continue-on-error).
	 *
	 * @param elementRaw the element's exact source text, sliced by {@link #splitBulkElements}; fed
	 *                   to the same ingestion pipeline a standalone submission would use, so
	 *                   raw-evidence immutability and the digest and signature checks hold per item
	 * @param identifiers the identifiers pre-scanned from the element, echoed on a rejected entry
	 */
	private TraceBulkResultEntry ingestOne(int index, String elementRaw, ElementIdentifiers identifiers,
			TraceRequestContext context) {
		TraceBulkResultEntry entry = new TraceBulkResultEntry();
		entry.setIndex(index);
		try {
			InputStream slice = new ByteArrayInputStream(elementRaw.getBytes(StandardCharsets.UTF_8));
			AcceptanceResult result = ingestionService.ingest(slice, context);
			entry.setAccepted(true);
			applyAcceptance(entry, toResponse(result));
		} catch (RuntimeException ex) {
			entry.setAccepted(false);
			entry.setProducerId(identifiers.producer);
			entry.setRecordId(identifiers.recordId);
			entry.setError(toErrorDto(ex));
		}
		return entry;
	}

	private void applyAcceptance(TraceBulkResultEntry entry, TraceAcceptanceResponse acceptance) {
		entry.setProducerId(acceptance.getProducerId());
		entry.setRecordId(acceptance.getRecordId());
		entry.setContentDigest(acceptance.getContentDigest());
		entry.setReceiptSequence(acceptance.getReceiptSequence());
		entry.setReceivedAt(acceptance.getReceivedAt());
		entry.setIdempotentReplay(acceptance.isIdempotentReplay());
		entry.setCoverageGapFlag(acceptance.isCoverageGapFlag());
		entry.setChainLinkFailureFlag(acceptance.isChainLinkFailureFlag());
		entry.setEquivocationFlag(acceptance.isEquivocationFlag());
		entry.setLateFlag(acceptance.isLateFlag());
		entry.setPolicyMismatchFlag(acceptance.isPolicyMismatchFlag());
	}

	/**
	 * Reads the top-level string {@code producer} and {@code record_id} of each element without
	 * building a tree or re-serializing it (the element text itself is never altered). The
	 * scan streams the element with the same nesting-depth, string-length and duplicate-key limits
	 * the single-record parser applies, and skips elements longer than {@code maxRequestBytes}
	 * characters (they fail on their own in the ingestion pipeline). An element that is not a JSON object, or
	 * that does not scan cleanly under those limits, yields no identifiers and fails individually.
	 */
	private List<ElementIdentifiers> scanElementIdentifiers(List<String> elements, TraceConfiguration config) {
		StreamReadConstraints constraints = StreamReadConstraints.builder().maxNestingDepth(config.getMaxJsonDepth())
				.maxStringLength(config.getMaxStringLength()).build();
		JsonFactory factory = JsonFactory.builder().streamReadConstraints(constraints).build();
		factory.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

		List<ElementIdentifiers> identifiers = new ArrayList<>(elements.size());
		for (String element : elements) {
			identifiers.add(element.length() > config.getMaxRequestBytes() ? ElementIdentifiers.NONE
					: scanElementIdentifiers(factory, element));
		}
		return identifiers;
	}

	private ElementIdentifiers scanElementIdentifiers(JsonFactory factory, String element) {
		String producer = null;
		String recordId = null;
		try (JsonParser parser = factory.createParser(element)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				return ElementIdentifiers.NONE;
			}
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String name = parser.currentName();
				JsonToken value = parser.nextToken();
				if (value == JsonToken.VALUE_STRING && FIELD_PRODUCER.equals(name)) {
					producer = parser.getText();
				} else if (value == JsonToken.VALUE_STRING && FIELD_RECORD_ID.equals(name)) {
					recordId = parser.getText();
				} else {
					parser.skipChildren();
				}
			}
		} catch (IOException ex) {
			return ElementIdentifiers.NONE;
		}
		return new ElementIdentifiers(producer, recordId);
	}

	/**
	 * Rejects the whole request when the batch names more than one distinct top-level
	 * {@code producer} (design §11.2), before any element is ingested. Elements without a
	 * readable producer are not counted.
	 */
	private void requireSingleProducer(List<ElementIdentifiers> identifiers) {
		String first = null;
		for (ElementIdentifiers element : identifiers) {
			if (element.producer == null) {
				continue;
			}
			if (first == null) {
				first = element.producer;
			} else if (!first.equals(element.producer)) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST,
						REASON_BULK_MIXED_PRODUCERS);
			}
		}
	}

	private static final class ElementIdentifiers {

		private static final ElementIdentifiers NONE = new ElementIdentifiers(null, null);

		private final String producer;

		private final String recordId;

		private ElementIdentifiers(String producer, String recordId) {
			this.producer = producer;
			this.recordId = recordId;
		}

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
	 * array element, so every element can be re-fed verbatim to the
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
		dto.setReceivedAt(TraceTime.formatMillis(result.getReceivedAtMs()));
		dto.setIdempotentReplay(result.isIdempotentReplay());
		dto.setCoverageGapFlag(result.isCoverageGapFlag());
		dto.setChainLinkFailureFlag(result.isChainLinkFailureFlag());
		dto.setEquivocationFlag(result.isEquivocationFlag());
		dto.setLateFlag(result.isLateFlag());
		dto.setPolicyMismatchFlag(result.isPolicyMismatchFlag());
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

	private TraceRecordResponse toRecordResponse(StoredTraceRecord stored) {
		TraceRecordResponse dto = new TraceRecordResponse();
		dto.setProducerId(stored.getIdentity().getProducerId());
		dto.setRecordId(stored.getIdentity().getRecordId());
		dto.setAssertion(parseAssertion(stored.getAssertionRaw()));
		dto.setAssertionRaw(stored.getAssertionRaw());
		dto.setContentDigest(stored.getContentDigest());
		dto.setVerification(toVerificationResponse(stored.getVerification()));
		dto.setIngestion(toIngestionResponse(stored.getReceipt(), stored.getFlags(),
				retrievalService.findReceiptState(stored)));
		return dto;
	}

	private TraceVerificationResponse toVerificationResponse(VerificationResult verification) {
		TraceVerificationResponse dto = new TraceVerificationResponse();
		dto.setSignatureValid(verification.isSignatureValid());
		dto.setKeyId(verification.getKeyId());
		dto.setVerifiedAt(TraceTime.formatMillis(verification.getVerifiedAtMs()));
		dto.setAlgorithm(verification.getAlgorithm());
		dto.setKeyThumbprint(verification.getKeyThumbprint());
		dto.setKeyAuthorized(verification.getKeyAuthorized());
		return dto;
	}

	private TraceIngestionResponse toIngestionResponse(ReceiptEntry receipt, IngestionFlags flags,
			String receiptState) {
		TraceIngestionResponse dto = new TraceIngestionResponse();
		dto.setReceiptSequence(receipt.getReceiptSequence());
		dto.setReceivedAt(TraceTime.formatMillis(receipt.getReceivedAtMs()));
		dto.setPrevReceiptHash(receipt.getPrevReceiptHash());
		dto.setReceiptHash(receipt.getReceiptHash());
		dto.setReceiptState(receiptState);
		dto.setCoverageGapFlag(flags.isCoverageGap());
		dto.setChainLinkFailureFlag(flags.isChainLinkFailure());
		dto.setEquivocationFlag(flags.isEquivocation());
		dto.setLateFlag(flags.isLate());
		dto.setPolicyMismatchFlag(flags.isPolicyMismatch());
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
