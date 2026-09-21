/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;

/**
 * Tests for {@link CommonAssertionValidator}: design §7.1 common-field validation. Each rule gets
 * a negative test producing the specified error id and reason, plus a positive test that the
 * design §7 example validates cleanly.
 */
class CommonAssertionValidatorTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Mock
	private AppConfiguration appConfiguration;

	@InjectMocks
	private CommonAssertionValidator validator;

	private TraceConfiguration traceConfiguration;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		traceConfiguration = new TraceConfiguration();
		org.mockito.Mockito.when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
	}

	private static ObjectNode loadFixture() throws IOException {
		try (InputStream is = CommonAssertionValidatorTest.class
				.getResourceAsStream("/trace/assertions/valid-authorization-decision.json")) {
			return (ObjectNode) MAPPER.readTree(is);
		}
	}

	private static ParsedAssertion assertionOf(ObjectNode root) {
		return new ParsedAssertion(root.toString(), root);
	}

	private static JsonNode at(ObjectNode root, String... path) {
		JsonNode node = root;
		for (String segment : path) {
			node = node.get(segment);
		}
		return node;
	}

	private static void remove(ObjectNode root, String... path) {
		if (path.length == 1) {
			root.remove(path[0]);
			return;
		}
		ObjectNode parent = (ObjectNode) at(root, java.util.Arrays.copyOf(path, path.length - 1));
		parent.remove(path[path.length - 1]);
	}

	private static void set(ObjectNode root, JsonNode value, String... path) {
		if (path.length == 1) {
			root.set(path[0], value);
			return;
		}
		ObjectNode parent = (ObjectNode) at(root, java.util.Arrays.copyOf(path, path.length - 1));
		parent.set(path[path.length - 1], value);
	}

	private TraceValidationException validateAndCapture(ObjectNode root) {
		return assertThrows(TraceValidationException.class, () -> validator.validate(assertionOf(root)));
	}

	// -- Positive path -----------------------------------------------------------------------

	@Test
	void testValidate_designExample_populatesTypedFields() throws IOException {
		ParsedAssertion assertion = assertionOf(loadFixture());

		validator.validate(assertion);

		assertEquals("cedarling-fleet-1/1.0.0", assertion.getProducer());
		assertEquals("cedarling-fleet-1-2026-01", assertion.getKid());
		assertEquals("9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41", assertion.getRecordId());
		assertEquals("tag:jans.io,2026:trace-v1", assertion.getEatProfile());
		assertEquals("AUTHORIZATION_DECISION", assertion.getEventKind());
		assertEquals(1781138542L, assertion.getSignedAt());
		assertEquals("exec-01JABCXYZQK8P5N9F2C7R3T4V6", assertion.getTraceExecutionId());
		assertEquals("spiffe://example.org/agent/planner", assertion.getExecutionAuthority());
		assertNotNull(assertion.getSubject());
		assertEquals("cedarling-fleet-1/1.0.0", assertion.getProducerChain().getProducerId());
		assertEquals("cedarling-001", assertion.getProducerChain().getProducerInstanceId());
		assertEquals("chain-01JABC9Z0K", assertion.getProducerChain().getProducerChainId());
		assertEquals(4821L, assertion.getProducerChain().getSequenceNumber());
		assertTrue(assertion.getProducerChain().getPrevRecordHash().startsWith("sha256:"));
		assertEquals(1, assertion.getParentRecordIds().size());
		assertEquals("issued_token", assertion.getParentRecordIds().get(0).getRelationshipType());
		assertNotNull(assertion.getSignature());
	}

	@Test
	void testValidate_ulidRecordId_accepted() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("01ARZ3NDEKTSV4RRFFQ69G5FAV"), "record_id");

		validator.validate(assertionOf(root));
	}

	@Test
	void testValidate_absentParentRecordIds_populatesEmptyList() throws IOException {
		ObjectNode root = loadFixture();
		remove(root, "parent_record_ids");

		ParsedAssertion assertion = assertionOf(root);
		validator.validate(assertion);

		assertTrue(assertion.getParentRecordIds().isEmpty());
	}

	@Test
	void testValidate_unknownTopLevelField_isPreservedNotRejected() throws IOException {
		ObjectNode root = loadFixture();
		root.set("extra_field", TextNode.valueOf("x"));

		ParsedAssertion assertion = assertionOf(root);
		validator.validate(assertion);

		assertTrue(assertion.getRoot().has("extra_field"));
	}

	@Test
	void testValidate_emptyParentRecordIdsArray_accepted() throws IOException {
		ObjectNode root = loadFixture();
		set(root, MAPPER.createArrayNode(), "parent_record_ids");

		ParsedAssertion assertion = assertionOf(root);
		validator.validate(assertion);

		assertTrue(assertion.getParentRecordIds().isEmpty());
	}

	// -- Required-field presence (generic "missing:<path>") ------------------------------------

	static Stream<Arguments> requiredFieldPaths() {
		return Stream.of(
				Arguments.of((Object) new String[] { "producer" }),
				Arguments.of((Object) new String[] { "kid" }),
				Arguments.of((Object) new String[] { "record_id" }),
				Arguments.of((Object) new String[] { "trace" }),
				Arguments.of((Object) new String[] { "trace", "eat_profile" }),
				Arguments.of((Object) new String[] { "trace", "event_kind" }),
				Arguments.of((Object) new String[] { "trace", "signed_at" }),
				Arguments.of((Object) new String[] { "trace", "trace_execution_id" }),
				Arguments.of((Object) new String[] { "trace", "execution_authority" }),
				Arguments.of((Object) new String[] { "trace", "event" }),
				Arguments.of((Object) new String[] { "producer_chain" }),
				Arguments.of((Object) new String[] { "producer_chain", "producer_id" }),
				Arguments.of((Object) new String[] { "producer_chain", "producer_instance_id" }),
				Arguments.of((Object) new String[] { "producer_chain", "producer_chain_id" }),
				Arguments.of((Object) new String[] { "producer_chain", "sequence_number" }),
				Arguments.of((Object) new String[] { "producer_chain", "prev_record_hash" }),
				Arguments.of((Object) new String[] { "signature" }));
	}

	@ParameterizedTest
	@MethodSource("requiredFieldPaths")
	void testValidate_missingRequiredField_throwsInvalidAssertionMissing(String[] path) throws IOException {
		ObjectNode root = loadFixture();
		remove(root, path);

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:" + String.join(".", path), ex.getReason());
	}

	// -- Specific-reason rules ------------------------------------------------------------------

	@Test
	void testValidate_producerNotSemver_throwsProducerMismatchFormat() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("not a valid producer"), "producer");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_PRODUCER_MISMATCH, ex.getErrorId());
		assertEquals("producer_format", ex.getReason());
	}

	@Test
	void testValidate_producerDoesNotMatchChainProducerId_throwsProducerMismatch() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("other-producer/2.0.0"), "producer");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_PRODUCER_MISMATCH, ex.getErrorId());
		assertEquals("producer_chain_mismatch", ex.getReason());
	}

	@Test
	void testValidate_recordIdNotUuidOrUlid_throwsInvalidAssertionRecordIdFormat() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("not-a-valid-record-id"), "record_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("record_id_format", ex.getReason());
	}

	@Test
	void testValidate_eatProfileMismatch_throwsUnsupportedProfile() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("tag:jans.io,2020:trace-v0"), "trace", "eat_profile");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_UNSUPPORTED_PROFILE, ex.getErrorId());
	}

	@Test
	void testValidate_eventKindUnsupported_throwsInvalidAssertion() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("SOMETHING_ELSE"), "trace", "event_kind");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("event_kind_unsupported", ex.getReason());
	}

	@Test
	void testValidate_signedAtNegative_throwsInvalidAssertionRange() throws IOException {
		ObjectNode root = loadFixture();
		set(root, MAPPER.getNodeFactory().numberNode(-1L), "trace", "signed_at");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("range:trace.signed_at", ex.getReason());
	}

	@Test
	void testValidate_signedAtAtLatenessBoundary_accepted() throws IOException {
		Instant now = Instant.parse("2026-06-11T00:00:00Z");
		validator.setClock(Clock.fixed(now, ZoneOffset.UTC));
		ObjectNode root = loadFixture();
		long boundary = now.getEpochSecond() + traceConfiguration.getLatenessThresholdSeconds();
		set(root, MAPPER.getNodeFactory().numberNode(boundary), "trace", "signed_at");

		validator.validate(assertionOf(root));
	}

	@Test
	void testValidate_signedAtPastLatenessBoundary_throwsSignedAtInFuture() throws IOException {
		Instant now = Instant.parse("2026-06-11T00:00:00Z");
		validator.setClock(Clock.fixed(now, ZoneOffset.UTC));
		ObjectNode root = loadFixture();
		long pastBoundary = now.getEpochSecond() + traceConfiguration.getLatenessThresholdSeconds() + 1;
		set(root, MAPPER.getNodeFactory().numberNode(pastBoundary), "trace", "signed_at");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("signed_at_in_future", ex.getReason());
	}

	@Test
	void testValidate_traceExecutionIdEmpty_throwsInvalidAssertionEmpty() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf(""), "trace", "trace_execution_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("empty:trace.trace_execution_id", ex.getReason());
	}

	@Test
	void testValidate_executionAuthorityTooLong_throwsInvalidAssertionLength() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("a".repeat(256)), "trace", "execution_authority");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("length:trace.execution_authority", ex.getReason());
	}

	@Test
	void testValidate_sequenceNumberZero_throwsInvalidAssertionRange() throws IOException {
		ObjectNode root = loadFixture();
		set(root, MAPPER.getNodeFactory().numberNode(0L), "producer_chain", "sequence_number");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("range:producer_chain.sequence_number", ex.getReason());
	}

	@Test
	void testValidate_prevRecordHashMalformed_throwsInvalidAssertion() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("not-a-hash"), "producer_chain", "prev_record_hash");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("prev_record_hash_format", ex.getReason());
	}

	@Test
	void testValidate_parentRecordIdsNotArray_throwsInvalidAssertion() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("not-an-array"), "parent_record_ids");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("parent_record_ids_format", ex.getReason());
	}

	@Test
	void testValidate_parentRecordIdEntryMissingField_throwsInvalidAssertion() throws IOException {
		ObjectNode root = loadFixture();
		ArrayNode parents = (ArrayNode) root.get("parent_record_ids");
		((ObjectNode) parents.get(0)).remove("producer_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:parent_record_ids[0].producer_id", ex.getReason());
	}

	@Test
	void testValidate_signatureNotBase64Url_throwsInvalidSignatureFormat() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("not base64url!!"), "signature");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_SIGNATURE, ex.getErrorId());
		assertEquals("signature_format", ex.getReason());
	}

	@Test
	void testValidate_signatureWrongLength_throwsInvalidSignatureLength() throws IOException {
		ObjectNode root = loadFixture();
		set(root, TextNode.valueOf("AAEC"), "signature");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_SIGNATURE, ex.getErrorId());
		assertEquals("signature_length", ex.getReason());
	}

	@Test
	void testValidate_topLevelEvidenceDomainId_throwsDomainFieldForbidden() throws IOException {
		ObjectNode root = loadFixture();
		root.set("evidence_domain_id", TextNode.valueOf("default"));

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_DOMAIN_FIELD_FORBIDDEN, ex.getErrorId());
	}

	@Test
	void testValidate_traceLevelEvidenceDomainId_throwsDomainFieldForbidden() throws IOException {
		ObjectNode root = loadFixture();
		((ObjectNode) root.get("trace")).set("evidence_domain_id", TextNode.valueOf("default"));

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_DOMAIN_FIELD_FORBIDDEN, ex.getErrorId());
	}

}
