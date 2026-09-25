/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.crypto.StrictBase64Url;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates the common fields every TRACE assertion must carry regardless of {@code event_kind}
 * (design §7.1) and fills in {@link ParsedAssertion}'s typed accessors. Event-kind-specific rules
 * (design §7.2) are task 09's job, not this class's.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class CommonAssertionValidator {

	/** 2^53 - 1: the largest integer a NumericDate/sequence number may safely carry. */
	private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

	private static final int MAX_SHORT_STRING = 255;

	private static final int EXPECTED_SIGNATURE_BYTES = 64;

	/** {@code name/semver}, design D-6/§7.1: producer id up to 128 chars, then a semver. */
	private static final Pattern PRODUCER_PATTERN = Pattern
			.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}/[0-9]+\\.[0-9]+\\.[0-9]+([-+][0-9A-Za-z.-]+)?$");

	/** RFC 4122 textual UUID form, any version/variant. */
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	/** ULID: 26 Crockford base32 characters (excludes I, L, O, U). */
	private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$",
			Pattern.CASE_INSENSITIVE);

	@Inject
	private AppConfiguration appConfiguration;

	/** Test seam: {@link Instant#now(Clock)} is used to evaluate {@code signed_at_in_future}. */
	private Clock clock = Clock.systemUTC();

	void setClock(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @throws TraceValidationException on any §7.1 violation; see the class javadoc of
	 *         {@link TraceValidationException} for the error-id/reason contract
	 */
	public void validate(ParsedAssertion assertion) {
		ObjectNode root = assertion.getRoot();

		rejectDomainField(root);

		ObjectNode trace = requireObject(root, "trace", "trace");
		ObjectNode producerChainNode = requireObject(root, "producer_chain", "producer_chain");

		String producer = requireNonEmptyString(root, "producer", "producer", null);
		String kid = requireNonEmptyString(root, "kid", "kid", null);
		String recordId = requireNonEmptyString(root, "record_id", "record_id", null);
		if (!isUuid(recordId) && !isUlid(recordId)) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "record_id_format");
		}

		String eatProfile = requireNonEmptyString(trace, "eat_profile", "trace.eat_profile", null);
		if (!TraceConstants.EAT_PROFILE.equals(eatProfile)) {
			throw new TraceValidationException(TraceValidationException.ERROR_UNSUPPORTED_PROFILE,
					"eat_profile_mismatch");
		}

		String eventKind = requireNonEmptyString(trace, "event_kind", "trace.event_kind", null);
		if (!TraceConstants.EVENT_KINDS.contains(eventKind)) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"event_kind_unsupported");
		}

		long signedAt = requireIntegralInRange(trace, "signed_at", "trace.signed_at", 0, MAX_SAFE_INTEGER);
		long nowSeconds = Instant.now(clock).getEpochSecond();
		long latenessThresholdSeconds = appConfiguration.getTraceConfiguration().getLatenessThresholdSeconds();
		if (signedAt > nowSeconds + latenessThresholdSeconds) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"signed_at_in_future");
		}

		String traceExecutionId = requireNonEmptyString(trace, "trace_execution_id", "trace.trace_execution_id",
				MAX_SHORT_STRING);
		String executionAuthority = requireNonEmptyString(trace, "execution_authority", "trace.execution_authority",
				MAX_SHORT_STRING);
		requireObject(trace, "event", "trace.event");
		JsonNode subject = trace.get("subject");

		String producerChainProducerId = requireNonEmptyString(producerChainNode, "producer_id",
				"producer_chain.producer_id", null);
		String producerInstanceId = requireNonEmptyString(producerChainNode, "producer_instance_id",
				"producer_chain.producer_instance_id", null);
		String producerChainId = requireNonEmptyString(producerChainNode, "producer_chain_id",
				"producer_chain.producer_chain_id", null);
		long sequenceNumber = requireIntegralInRange(producerChainNode, "sequence_number",
				"producer_chain.sequence_number", 1, MAX_SAFE_INTEGER);
		String prevRecordHash = requireNonEmptyString(producerChainNode, "prev_record_hash",
				"producer_chain.prev_record_hash", null);
		if (!TraceConstants.HASH_PATTERN.matcher(prevRecordHash).matches()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"prev_record_hash_format");
		}

		if (!PRODUCER_PATTERN.matcher(producer).matches()) {
			throw new TraceValidationException(TraceValidationException.ERROR_PRODUCER_MISMATCH, "producer_format");
		}
		if (!producer.equals(producerChainProducerId)) {
			throw new TraceValidationException(TraceValidationException.ERROR_PRODUCER_MISMATCH,
					"producer_chain_mismatch");
		}

		List<ParsedAssertion.ParentRecordId> parentRecordIds = parseParentRecordIds(root);

		String signature = requireNonEmptyString(root, "signature", "signature", null);
		byte[] signatureBytes;
		try {
			signatureBytes = StrictBase64Url.decode(signature);
		} catch (IllegalArgumentException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_SIGNATURE, "signature_format",
					ex);
		}
		if (signatureBytes.length != EXPECTED_SIGNATURE_BYTES) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_SIGNATURE, "signature_length");
		}

		assertion.setProducer(producer);
		assertion.setKid(kid);
		assertion.setRecordId(recordId);
		assertion.setEatProfile(eatProfile);
		assertion.setEventKind(eventKind);
		assertion.setSignedAt(signedAt);
		assertion.setTraceExecutionId(traceExecutionId);
		assertion.setExecutionAuthority(executionAuthority);
		assertion.setSubject(subject);
		assertion.setProducerChain(new ParsedAssertion.ProducerChain(producerChainProducerId, producerInstanceId,
				producerChainId, sequenceNumber, prevRecordHash));
		assertion.setParentRecordIds(parentRecordIds);
		assertion.setSignature(signature);
	}

	private void rejectDomainField(ObjectNode root) {
		if (root.has(TraceConstants.DOMAIN_FIELD) || root.path("trace").has(TraceConstants.DOMAIN_FIELD)) {
			throw new TraceValidationException(TraceValidationException.ERROR_DOMAIN_FIELD_FORBIDDEN,
					TraceConstants.DOMAIN_FIELD);
		}
	}

	private List<ParsedAssertion.ParentRecordId> parseParentRecordIds(ObjectNode root) {
		JsonNode node = root.get("parent_record_ids");
		if (node == null || node.isNull()) {
			return Collections.emptyList();
		}
		if (!node.isArray()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"parent_record_ids_format");
		}
		List<ParsedAssertion.ParentRecordId> result = new ArrayList<>(node.size());
		for (int i = 0; i < node.size(); i++) {
			JsonNode entry = node.get(i);
			String path = "parent_record_ids[" + i + "]";
			if (!entry.isObject()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:" + path);
			}
			String producerId = requireNonEmptyString(entry, "producer_id", path + ".producer_id", null);
			String recordId = requireNonEmptyString(entry, "record_id", path + ".record_id", null);
			String relationshipType = requireNonEmptyString(entry, "relationship_type", path + ".relationship_type",
					null);
			result.add(new ParsedAssertion.ParentRecordId(producerId, recordId, relationshipType));
		}
		return result;
	}

	private JsonNode requireField(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (node == null || node.isNull()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "missing:" + path);
		}
		return node;
	}

	private ObjectNode requireObject(JsonNode parent, String field, String path) {
		JsonNode node = requireField(parent, field, path);
		if (!node.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		return (ObjectNode) node;
	}

	/**
	 * @param maxLength inclusive upper bound on character length, or {@code null} for no extra
	 *        bound beyond "non-empty"
	 */
	private String requireNonEmptyString(JsonNode parent, String field, String path, Integer maxLength) {
		JsonNode node = requireField(parent, field, path);
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		String value = node.textValue();
		if (value.isEmpty()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "empty:" + path);
		}
		if (maxLength != null && value.length() > maxLength) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "length:" + path);
		}
		return value;
	}

	private long requireIntegralInRange(JsonNode parent, String field, String path, long min, long max) {
		JsonNode node = requireField(parent, field, path);
		if (!node.isIntegralNumber() || !node.canConvertToLong()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		long value = node.longValue();
		if (value < min || value > max) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "range:" + path);
		}
		return value;
	}

	private boolean isUuid(String value) {
		return UUID_PATTERN.matcher(value).matches();
	}

	private boolean isUlid(String value) {
		return value.length() == 26 && ULID_PATTERN.matcher(value).matches();
	}

}
