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
import io.jans.lock.service.trace.TraceIssuers;
import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Validates the common fields every TRACE assertion must carry regardless of {@code event_kind}
 * (design §7.1) and fills in {@link ParsedAssertion}'s typed accessors. Event-kind-specific rules
 * (design §7.2) belong to the event-kind validators, not this class.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class CommonAssertionValidator {

	/** 2^53 - 1: the largest integer a NumericDate/sequence number may safely carry. */
	private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

	private static final int MAX_SHORT_STRING = 255;

	private static final int EXPECTED_SIGNATURE_BYTES = 64;

	/** RFC 4122 textual UUID form, any version/variant. */
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	/** ULID: 26 Crockford base32 characters (excludes I, L, O, U). */
	private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$",
			Pattern.CASE_INSENSITIVE);

	@Inject
	private AppConfiguration appConfiguration;

	private Clock clock = Clock.systemUTC();

	/**
	 * @throws TraceValidationException on any §7.1 violation; see the class javadoc of
	 *         {@link TraceValidationException} for the error-id/reason contract
	 */
	public void validate(ParsedAssertion assertion) {
		ObjectNode root = assertion.getRoot();

		List<String> reservedMembers = findReservedMembers(root);

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
		long maxFutureSkewSeconds = appConfiguration.getTraceConfiguration().getMaxFutureSkewSeconds();
		if (signedAt > nowSeconds + maxFutureSkewSeconds) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"signed_at_in_future");
		}

		String traceExecutionId = requireNonEmptyString(trace, "trace_execution_id", "trace.trace_execution_id",
				MAX_SHORT_STRING);
		String executionAuthority = requireNonEmptyString(trace, "execution_authority", "trace.execution_authority",
				MAX_SHORT_STRING);
		requireObject(trace, "event", "trace.event");
		JsonNode subject = trace.get("subject");
		validateOptionalTraceMembers(trace);
		validateOptionalSubjectMembers(subject);

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

		validateChainLink(producerChainNode, sequenceNumber);

		if (!TraceConstants.PRODUCER_PATTERN.matcher(producer).matches()) {
			throw new TraceValidationException(TraceValidationException.ERROR_PRODUCER_MISMATCH, "producer_format");
		}
		if (!producer.equals(producerChainProducerId)) {
			throw new TraceValidationException(TraceValidationException.ERROR_PRODUCER_MISMATCH,
					"producer_chain_mismatch");
		}

		validateProducerVersion(root);

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
		assertion.setReservedMembers(reservedMembers);
	}

	/**
	 * Type-checks the optional GovOps profile members of {@code trace} (profile §2). Event-kind
	 * rules and closed vocabularies deferred to the full design are not enforced here.
	 */
	private void validateOptionalTraceMembers(ObjectNode trace) {
		for (String field : new String[] { "execution_epoch_id", "previous_epoch_id", "parent_execution_id" }) {
			optionalNonEmptyString(trace, field, "trace." + field, MAX_SHORT_STRING);
		}

		JsonNode operationIds = trace.get("operation_ids");
		if (isPresent(operationIds)) {
			if (!operationIds.isArray()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:trace.operation_ids");
			}
			for (int i = 0; i < operationIds.size(); i++) {
				checkNonEmptyString(operationIds.get(i), "trace.operation_ids[" + i + "]", null);
			}
		}

		optionalNonEmptyString(trace, "session_id", "trace.session_id", null);
		if (isPresent(trace.get("session_id")) || isPresent(trace.get("session_issuer"))) {
			String sessionIssuer = requireNonEmptyString(trace, "session_issuer", "trace.session_issuer", null);
			if (!TraceIssuers.isValid(sessionIssuer)) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"invalid:trace.session_issuer");
			}
		}

		optionalNonEmptyString(trace, "evidence_origin", "trace.evidence_origin", null);
		optionalNonEmptyString(trace, "measurement_point", "trace.measurement_point", null);

		JsonNode recorder = trace.get("recorder");
		if (isPresent(recorder) && !recorder.isObject() && !recorder.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"type:trace.recorder");
		}

		optionalObject(trace, "attestation_ref", "trace.attestation_ref");

		JsonNode workloadAuthentication = optionalObject(trace, "workload_authentication",
				"trace.workload_authentication");
		if (workloadAuthentication != null) {
			for (String field : new String[] { "authentication_layer", "method", "authenticated_workload_id",
					"proof_ref" }) {
				optionalString(workloadAuthentication, field, "trace.workload_authentication." + field);
			}
		}

		JsonNode model = optionalObject(trace, "model", "trace.model");
		if (model != null) {
			for (String field : new String[] { "provider", "model_id", "version" }) {
				optionalString(model, field, "trace.model." + field);
			}
		}
	}

	/** Type-checks {@code trace.subject} (GovOps profile §3): an object whose sponsor/org members are string or object. */
	private void validateOptionalSubjectMembers(JsonNode subject) {
		if (!isPresent(subject)) {
			return;
		}
		if (!subject.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:trace.subject");
		}
		for (String field : new String[] { "human_sponsor", "delegating_org" }) {
			JsonNode node = subject.get(field);
			if (isPresent(node) && !node.isObject() && !node.isTextual()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:trace.subject." + field);
			}
		}
	}

	/**
	 * Type-checks {@code producer_chain.chain_link} (GovOps profile §5). The member exists only on a
	 * genesis ({@code sequence_number == 1}); anywhere else it is {@code invalid_genesis}. Full link
	 * validation against the prior chain stays deferred (MVP design §8).
	 */
	private void validateChainLink(ObjectNode producerChainNode, long sequenceNumber) {
		JsonNode chainLink = producerChainNode.get("chain_link");
		if (!isPresent(chainLink)) {
			return;
		}
		if (!chainLink.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"type:producer_chain.chain_link");
		}
		if (sequenceNumber != 1) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_GENESIS,
					"chain_link_on_non_genesis");
		}
		optionalNonEmptyString(chainLink, "prev_producer_chain_id", "producer_chain.chain_link.prev_producer_chain_id",
				null);
	}

	private boolean isPresent(JsonNode node) {
		return node != null && !node.isNull();
	}

	private void checkNonEmptyString(JsonNode node, String path, Integer maxLength) {
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		if (node.textValue().isEmpty()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "empty:" + path);
		}
		if (maxLength != null && node.textValue().length() > maxLength) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "length:" + path);
		}
	}

	private void optionalNonEmptyString(JsonNode parent, String field, String path, Integer maxLength) {
		JsonNode node = parent.get(field);
		if (isPresent(node)) {
			checkNonEmptyString(node, path, maxLength);
		}
	}

	private void optionalString(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (isPresent(node) && !node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
	}

	/** @return the member when present and an object, {@code null} when absent */
	private JsonNode optionalObject(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (!isPresent(node)) {
			return null;
		}
		if (!node.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		return node;
	}

	private void validateProducerVersion(ObjectNode root) {
		if (!root.has(TraceConstants.PRODUCER_VERSION_FIELD)) {
			return;
		}
		JsonNode node = root.get(TraceConstants.PRODUCER_VERSION_FIELD);
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"type:" + TraceConstants.PRODUCER_VERSION_FIELD);
		}
		int length = node.textValue().length();
		if (length < 1 || length > TraceConstants.MAX_PRODUCER_VERSION_LENGTH) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"length:" + TraceConstants.PRODUCER_VERSION_FIELD);
		}
	}

	/**
	 * Lists the paths of members Lock derives itself ({@link TraceConstants#DOMAIN_FIELD} and
	 * {@link TraceConstants#LOCK_DERIVED_MEMBERS}) found at the top level, in {@code trace},
	 * {@code trace.policy}, {@code trace.event} and in each {@code decision}/{@code invocation}
	 * fact (GovOps profile §12). Such members are ignored, never rejected: the record is flagged
	 * and Lock's own values are used. Members of other objects (parent edges, {@code tokens[]})
	 * are not reserved. A non-object container is skipped and left to the shape checks.
	 */
	private List<String> findReservedMembers(ObjectNode root) {
		List<String> found = new ArrayList<>();
		collectReservedMembers(root, "", found);
		JsonNode trace = root.get("trace");
		if (trace == null || !trace.isObject()) {
			return found;
		}
		collectReservedMembers(trace, "trace.", found);
		collectReservedMembersOfObject(trace.get("policy"), "trace.policy.", found);
		JsonNode event = trace.get("event");
		if (event == null || !event.isObject()) {
			return found;
		}
		collectReservedMembers(event, "trace.event.", found);
		for (String field : new String[] { "decision", "invocation" }) {
			collectReservedMembersOfObject(event.get(field), "trace.event." + field + ".", found);
		}
		for (String field : new String[] { "decisions", "invocations" }) {
			JsonNode entries = event.get(field);
			if (entries != null && entries.isArray()) {
				for (int i = 0; i < entries.size(); i++) {
					collectReservedMembersOfObject(entries.get(i), "trace.event." + field + "[" + i + "].", found);
				}
			}
		}
		return found;
	}

	private static void collectReservedMembersOfObject(JsonNode node, String pathPrefix, List<String> found) {
		if (node != null && node.isObject()) {
			collectReservedMembers(node, pathPrefix, found);
		}
	}

	private static void collectReservedMembers(JsonNode node, String pathPrefix, List<String> found) {
		if (node.has(TraceConstants.DOMAIN_FIELD)) {
			found.add(pathPrefix + TraceConstants.DOMAIN_FIELD);
		}
		for (String member : TraceConstants.LOCK_DERIVED_MEMBERS) {
			if (node.has(member)) {
				found.add(pathPrefix + member);
			}
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
