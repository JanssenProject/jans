/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.parse.TraceAssertionParser;

/**
 * Tests for the {@code EventKindValidator} implementations and {@link EventKindValidatorRegistry}
 * (design §7.2 per-kind schema, §7.3 token references). Fixtures already pass §7.1 common
 * validation; each test either uses a fixture as-is or mutates its parsed tree and re-serializes
 * it before feeding it back through {@link TraceAssertionParser} + {@link CommonAssertionValidator}
 * — {@link ParsedAssertion}'s typed setters are package-private to {@code parse}, so a real
 * assertion for this package's tests can only come from that pipeline.
 */
class EventKindValidatorsTest extends BaseLockServiceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private AppConfiguration appConfiguration;

	private TraceAssertionParser parser;

	private CommonAssertionValidator commonValidator;

	private EventKindValidatorRegistry registry;

	@BeforeEach
	void setUp() {
		AppConfiguration mockConfig = org.mockito.Mockito.mock(AppConfiguration.class);
		TraceConfiguration traceConfiguration = new TraceConfiguration();
		org.mockito.Mockito.when(mockConfig.getTraceConfiguration()).thenReturn(traceConfiguration);
		this.appConfiguration = mockConfig;

		parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);

		registry = new EventKindValidatorRegistry();
	}

	private static ObjectNode loadFixture(String name) throws IOException {
		try (InputStream is = EventKindValidatorsTest.class
				.getResourceAsStream("/trace/assertions/" + name)) {
			return (ObjectNode) MAPPER.readTree(is);
		}
	}

	private ParsedAssertion parseAndCommonValidate(ObjectNode root) {
		InputStream body = new ByteArrayInputStream(root.toString().getBytes(StandardCharsets.UTF_8));
		ParsedAssertion assertion = parser.parse(body);
		commonValidator.validate(assertion);
		return assertion;
	}

	/** Path segments navigate object fields, or array indices (a segment parsing as an integer). */
	private static JsonNode child(JsonNode node, String segment) {
		return node.isArray() ? node.get(Integer.parseInt(segment)) : node.get(segment);
	}

	private static JsonNode at(ObjectNode root, String... path) {
		JsonNode node = root;
		for (String segment : path) {
			node = child(node, segment);
		}
		return node;
	}

	private static void remove(ObjectNode root, String... path) {
		JsonNode parent = at(root, Arrays.copyOf(path, path.length - 1));
		String last = path[path.length - 1];
		if (parent.isArray()) {
			((ArrayNode) parent).remove(Integer.parseInt(last));
		} else {
			((ObjectNode) parent).remove(last);
		}
	}

	private static void set(ObjectNode root, JsonNode value, String... path) {
		JsonNode parent = at(root, Arrays.copyOf(path, path.length - 1));
		String last = path[path.length - 1];
		if (parent.isArray()) {
			((ArrayNode) parent).set(Integer.parseInt(last), value);
		} else {
			((ObjectNode) parent).set(last, value);
		}
	}

	private TraceValidationException validateAndCapture(ObjectNode root) {
		return assertThrows(TraceValidationException.class, () -> registry.validate(parseAndCommonValidate(root)));
	}

	// -- Optional GovOps profile members ------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = { "govops_example_b.json", "govops_example_d.json",
			"authorization_decision_optional_members_valid.json" })
	void testOptionalGovopsMembers_profileFixtures_accepted(String fixture) throws IOException {
		registry.validate(parseAndCommonValidate(loadFixture(fixture)));
	}

	@Test
	void testOptionalGovopsMembers_absent_accepted() throws IOException {
		registry.validate(parseAndCommonValidate(loadFixture("authorization_decision_valid.json")));
	}

	@Test
	void testEnforcementMode_everyAllowedValue_accepted() throws IOException {
		for (String mode : new String[] { "enforce", "advisory", "silent", "declared" }) {
			ObjectNode root = loadFixture("authorization_decision_valid.json");
			set(root, new TextNode(mode), "trace", "policy", "enforcement_mode");
			registry.validate(parseAndCommonValidate(root));
		}
	}

	@Test
	void testChainLink_onGenesis_accepted() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_optional_members_valid.json");
		assertEquals(1, at(root, "producer_chain", "sequence_number").asInt());
		assertEquals("chain-01JABC8Y0J",
				at(root, "producer_chain", "chain_link", "prev_producer_chain_id").asText());
		registry.validate(parseAndCommonValidate(root));
	}

	@Test
	void testChainLink_atSequence7_throwsInvalidGenesis() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_optional_members_valid.json");
		set(root, new LongNode(7), "producer_chain", "sequence_number");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertEquals("chain_link_on_non_genesis", ex.getReason());
	}

	// -- AUTHORIZATION_DECISION --------------------------------------------------------------

	@Test
	void testAuthorizationDecision_designExample_populatesCorrelationInputs() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("authorization_decision_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("AUTHORIZATION_DECISION", result.getEventKind());
		assertEquals("spiffe://example.org/agent/planner", result.getExecutionAuthority());
		assertEquals("exec-01JABCXYZQK8P5N9F2C7R3T4V6", result.getTraceExecutionId());
		assertEquals(1, result.getTokenRefs().size());
		TokenRef tokenRef = result.getTokenRefs().get(0);
		assertEquals("https://accounts.example.org", tokenRef.getIssuer());
		assertEquals("9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e", tokenRef.getJti());
		assertNull(tokenRef.getFingerprint());
		assertEquals("chain-01JABC9Z0K", result.getChainPosition().getProducerChainId());
		assertEquals(1, result.getParents().size());
		assertTrue(result.getWarnings().isEmpty());
	}

	/**
	 * The design §7 example fixture validates as {@code AUTHORIZATION_DECISION} and yields the specified
	 * {@code TokenRef}.
	 */
	@Test
	void testAuthorizationDecision_task08DesignExampleFixture_validatesAsSpecified() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("valid-authorization-decision.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals(1, result.getTokenRefs().size());
		assertEquals("https://accounts.example.org", result.getTokenRefs().get(0).getIssuer());
		assertEquals("9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e", result.getTokenRefs().get(0).getJti());
	}

	@Test
	void testAuthorizationDecision_missingBundleHash_throwsMissingBundleHash() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "policy", "bundle_hash");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.policy.bundle_hash", ex.getReason());
	}

	@Test
	void testAuthorizationDecision_bundleHashWrongFormat_throwsInvalidBundleHash() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("not-a-digest"), "trace", "policy", "bundle_hash");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.policy.bundle_hash", ex.getReason());
	}

	@Test
	void testAuthorizationDecision_bundleHashGenericDigestForm_accepted() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("blake3:abcDEF123-_"), "trace", "policy", "bundle_hash");

		ParsedAssertion assertion = parseAndCommonValidate(root);

		registry.validate(assertion);
	}

	@Test
	void testAuthorizationDecision_outcomeNotAllowOrDeny_throwsInvalidOutcome() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("MAYBE"), "trace", "event", "decision", "outcome");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.decision.outcome", ex.getReason());
	}

	@Test
	void testAuthorizationDecision_missingRuntimePdpId_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "runtime", "pdp_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.runtime.pdp_id", ex.getReason());
	}

	// -- decision facts (AUTHORIZATION_DECISION) ----------------------------------------------

	private static ObjectNode fact(String idField, String idValue, String outcome) {
		ObjectNode fact = MAPPER.createObjectNode();
		if (idField != null) {
			fact.put(idField, idValue);
		}
		fact.put("action", "Acme::Action::\"Pay\"");
		fact.put("resource_type", "Acme::Payment");
		fact.put("outcome", outcome);
		return fact;
	}

	private static void replaceFacts(ObjectNode root, String singular, String plural, ArrayNode entries) {
		ObjectNode event = (ObjectNode) at(root, "trace", "event");
		event.remove(singular);
		event.set(plural, entries);
	}

	private void assertRejected(ObjectNode root, String expectedReason) {
		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals(expectedReason, ex.getReason());
	}

	@Test
	void testAuthorizationDecision_batchDecisions_accepted() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("authorization_decision_batch_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("AUTHORIZATION_DECISION", result.getEventKind());
		assertEquals(1, result.getTokenRefs().size());
	}

	@Test
	void testAuthorizationDecision_decisionsEmpty_throwsEmpty() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		replaceFacts(root, "decision", "decisions", MAPPER.createArrayNode());

		assertRejected(root, "empty:trace.event.decisions");
	}

	@Test
	void testAuthorizationDecision_decisionsDuplicateId_throwsDuplicate() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		ArrayNode entries = MAPPER.createArrayNode().add(fact("decision_id", "d-1", "ALLOW"))
				.add(fact("decision_id", "d-1", "DENY"));
		replaceFacts(root, "decision", "decisions", entries);

		assertRejected(root, "duplicate:trace.event.decisions[1].decision_id");
	}

	@Test
	void testAuthorizationDecision_decisionsEntryInvalidOutcome_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		replaceFacts(root, "decision", "decisions",
				MAPPER.createArrayNode().add(fact("decision_id", "d-1", "MAYBE")));

		assertRejected(root, "invalid:trace.event.decisions[0].outcome");
	}

	@Test
	void testAuthorizationDecision_decisionsEntryWithoutId_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		replaceFacts(root, "decision", "decisions", MAPPER.createArrayNode().add(fact(null, null, "ALLOW")));

		assertRejected(root, "missing:trace.event.decisions[0].decision_id");
	}

	@Test
	void testAuthorizationDecision_recordLevelOutcome_forbiddenInBothForms() throws IOException {
		ObjectNode singular = loadFixture("authorization_decision_valid.json");
		((ObjectNode) at(singular, "trace", "event")).put("outcome", "ALLOW");
		assertRejected(singular, "forbidden:trace.event.outcome");

		ObjectNode batch = loadFixture("authorization_decision_batch_valid.json");
		((ObjectNode) at(batch, "trace", "event")).put("outcome", "ALLOW");
		assertRejected(batch, "forbidden:trace.event.outcome");
	}

	@Test
	void testAuthorizationDecision_decisionAndDecisions_throwsForbidden() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		((ObjectNode) at(root, "trace", "event")).set("decisions",
				MAPPER.createArrayNode().add(fact("decision_id", "d-1", "ALLOW")));

		assertRejected(root, "forbidden:trace.event.decisions");
	}

	@Test
	void testAuthorizationDecision_neitherDecisionNorDecisions_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "event", "decision");

		assertRejected(root, "missing:trace.event.decision");
	}

	@Test
	void testAuthorizationDecision_factFieldProblems_rejected() throws IOException {
		ObjectNode noAction = loadFixture("authorization_decision_valid.json");
		remove(noAction, "trace", "event", "decision", "action");
		assertRejected(noAction, "missing:trace.event.decision.action");

		ObjectNode longAction = loadFixture("authorization_decision_valid.json");
		set(longAction, TextNode.valueOf("a".repeat(513)), "trace", "event", "decision", "action");
		assertRejected(longAction, "length:trace.event.decision.action");

		ObjectNode emptyType = loadFixture("authorization_decision_valid.json");
		set(emptyType, TextNode.valueOf(""), "trace", "event", "decision", "resource_type");
		assertRejected(emptyType, "empty:trace.event.decision.resource_type");

		ObjectNode resourceId = loadFixture("authorization_decision_valid.json");
		set(resourceId, TextNode.valueOf("Acme::Payment::\"p-1\""), "trace", "event", "decision", "resource_id");
		assertRejected(resourceId, "forbidden:trace.event.decision.resource_id");

		ObjectNode batchResourceId = loadFixture("authorization_decision_batch_valid.json");
		set(batchResourceId, TextNode.valueOf("x"), "trace", "event", "decisions", "0", "resource_id");
		assertRejected(batchResourceId, "forbidden:trace.event.decisions[0].resource_id");

		ObjectNode notObject = loadFixture("authorization_decision_valid.json");
		set(notObject, TextNode.valueOf("ALLOW"), "trace", "event", "decision");
		assertRejected(notObject, "type:trace.event.decision");
	}

	@Test
	void testAuthorizationDecision_capabilityIds_forbidden() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		((ObjectNode) at(root, "trace", "event")).set("capability_ids", MAPPER.createArrayNode()
				.add(MAPPER.createObjectNode().put("capability_id", "invoke:payment-authorization")
						.put("outcome", "ALLOW")));

		assertRejected(root, "forbidden:trace.event.capability_ids");
	}

	@Test
	void testEvent_unknownMembersInTraceEvent_stillAllowed() throws IOException {
		for (String fixture : new String[] { "authorization_decision_valid.json", "capability_invoked_valid.json",
				"runtime_effect_valid.json" }) {
			ObjectNode root = loadFixture(fixture);
			((ObjectNode) at(root, "trace", "event")).put("x_future_member", "kept");

			registry.validate(parseAndCommonValidate(root));
		}
	}

	// -- trace.subject (AUTHORIZATION_DECISION / CAPABILITY_INVOKED / RUNTIME_EFFECT) ------------

	@Test
	void testSubject_workloadIdRequiredForDecisionAndInvocation() throws IOException {
		for (String fixture : new String[] { "authorization_decision_valid.json", "capability_invoked_valid.json" }) {
			ObjectNode noWorkloadId = loadFixture(fixture);
			remove(noWorkloadId, "trace", "subject", "workload_id");
			assertRejected(noWorkloadId, "missing:trace.subject.workload_id");

			ObjectNode noSubject = loadFixture(fixture);
			remove(noSubject, "trace", "subject");
			assertRejected(noSubject, "missing:trace.subject.workload_id");

			ObjectNode emptyId = loadFixture(fixture);
			set(emptyId, TextNode.valueOf(""), "trace", "subject", "workload_id");
			assertRejected(emptyId, "empty:trace.subject.workload_id");

			ObjectNode longId = loadFixture(fixture);
			set(longId, TextNode.valueOf("w".repeat(256)), "trace", "subject", "workload_id");
			assertRejected(longId, "length:trace.subject.workload_id");

			ObjectNode longInstance = loadFixture(fixture);
			set(longInstance, TextNode.valueOf("w".repeat(256)), "trace", "subject", "workload_instance_id");
			assertRejected(longInstance, "length:trace.subject.workload_instance_id");

			ObjectNode notObject = loadFixture(fixture);
			set(notObject, TextNode.valueOf("spiffe://example.org/agent/planner"), "trace", "subject");
			assertRejected(notObject, "type:trace.subject");
		}
	}

	@Test
	void testSubject_workloadInstanceIdOptional_andMaxLengthAccepted() throws IOException {
		ObjectNode noInstance = loadFixture("authorization_decision_valid.json");
		remove(noInstance, "trace", "subject", "workload_instance_id");
		registry.validate(parseAndCommonValidate(noInstance));

		ObjectNode maxLength = loadFixture("capability_invoked_valid.json");
		set(maxLength, TextNode.valueOf("w".repeat(255)), "trace", "subject", "workload_id");
		set(maxLength, TextNode.valueOf("w".repeat(255)), "trace", "subject", "workload_instance_id");
		registry.validate(parseAndCommonValidate(maxLength));
	}

	@Test
	void testSubject_workloadIdNotEqualToExecutionAuthority_accepted() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("spiffe://example.org/agent/other"), "trace", "subject", "workload_id");

		registry.validate(parseAndCommonValidate(root));
	}

	// -- CAPABILITY_INVOKED ---------------------------------------------------------------------

	@Test
	void testCapabilityInvoked_designExample_populatesCorrelationInputs() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("capability_invoked_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("CAPABILITY_INVOKED", result.getEventKind());
		assertEquals(1, result.getTokenRefs().size());
	}

	@Test
	void testCapabilityInvoked_withCompletePolicy_accepted() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		set(root, at(loadFixture("authorization_decision_valid.json"), "trace", "policy").deepCopy(), "trace",
				"policy");

		registry.validate(parseAndCommonValidate(root));
	}

	@Test
	void testCapabilityInvoked_withIncompletePolicy_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		ObjectNode policy = MAPPER.createObjectNode();
		policy.put("policy_store_id", "x");
		set(root, policy, "trace", "policy");

		assertRejected(root, "missing:trace.policy.policy_store_version");
	}

	@Test
	void testCapabilityInvoked_missingEnforcementPointId_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		remove(root, "trace", "event", "enforcement_point_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.event.enforcement_point_id", ex.getReason());
	}

	@Test
	void testCapabilityInvoked_outcomeOutsideVocabulary_throwsInvalid() throws IOException {
		for (String outcome : new String[] { "ok", "INVOKED", "ALLOW", "success" }) {
			ObjectNode root = loadFixture("capability_invoked_valid.json");
			set(root, TextNode.valueOf(outcome), "trace", "event", "invocation", "outcome");

			assertRejected(root, "invalid:trace.event.invocation.outcome");
		}
	}

	@Test
	void testCapabilityInvoked_batchEntryOutcomeOutsideVocabulary_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		replaceFacts(root, "invocation", "invocations",
				MAPPER.createArrayNode().add(fact("invocation_id", "i-1", "ALLOW")));

		assertRejected(root, "invalid:trace.event.invocations[0].outcome");
	}

	@Test
	void testCapabilityInvoked_successAndFailureOutcomes_accepted() throws IOException {
		for (String outcome : new String[] { "SUCCESS", "FAILURE" }) {
			ObjectNode root = loadFixture("capability_invoked_valid.json");
			set(root, TextNode.valueOf(outcome), "trace", "event", "invocation", "outcome");

			registry.validate(parseAndCommonValidate(root));
		}
	}

	@Test
	void testDecision_denyAndAllowOnly_invocationOutcomeRejected() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("SUCCESS"), "trace", "event", "decision", "outcome");

		assertRejected(root, "invalid:trace.event.decision.outcome");
	}

	@Test
	void testOperationBindingDigests_wellFormed_accepted() throws IOException {
		String digest = "sha256:84fd9bac333ad79154348296204fa7f8c537a96e08983e5f73b3f5aca8e8edf7";
		ObjectNode decision = loadFixture("authorization_decision_valid.json");
		for (String field : new String[] { "request_digest", "input_digest", "tool_call_digest" }) {
			set(decision, TextNode.valueOf(digest), "trace", "event", field);
		}
		registry.validate(parseAndCommonValidate(decision));

		ObjectNode invocation = loadFixture("capability_invoked_valid.json");
		for (String field : new String[] { "request_digest", "tool_call_digest", "result_digest" }) {
			set(invocation, TextNode.valueOf(digest), "trace", "event", field);
		}
		registry.validate(parseAndCommonValidate(invocation));
	}

	@Test
	void testOperationBindingDigests_malformed_throwsInvalid() throws IOException {
		for (String field : new String[] { "request_digest", "input_digest", "tool_call_digest" }) {
			for (String bad : new String[] { "not-a-hash", "sha256:ABC", "md5:84fd9bac333ad79154348296204fa7f8" }) {
				ObjectNode root = loadFixture("authorization_decision_valid.json");
				set(root, TextNode.valueOf(bad), "trace", "event", field);
				assertRejected(root, "invalid:trace.event." + field);
			}
		}
		for (String field : new String[] { "request_digest", "tool_call_digest", "result_digest" }) {
			ObjectNode root = loadFixture("capability_invoked_valid.json");
			set(root, TextNode.valueOf("not-a-hash"), "trace", "event", field);
			assertRejected(root, "invalid:trace.event." + field);
		}
	}

	@Test
	void testOperationBindingDigests_nonString_throwsType() throws IOException {
		ObjectNode decision = loadFixture("authorization_decision_valid.json");
		((ObjectNode) at(decision, "trace", "event")).put("request_digest", 42);
		assertRejected(decision, "type:trace.event.request_digest");

		ObjectNode invocation = loadFixture("capability_invoked_valid.json");
		((ObjectNode) at(invocation, "trace", "event")).put("result_digest", 42);
		assertRejected(invocation, "type:trace.event.result_digest");
	}

	@Test
	void testCapabilityInvoked_batchInvocations_accepted() throws IOException {
		CorrelationInputs result = registry
				.validate(parseAndCommonValidate(loadFixture("capability_invoked_batch_valid.json")));

		assertEquals("CAPABILITY_INVOKED", result.getEventKind());
	}

	@Test
	void testCapabilityInvoked_invocationsEmptyOrDuplicateOrMalformed_rejected() throws IOException {
		ObjectNode empty = loadFixture("capability_invoked_valid.json");
		replaceFacts(empty, "invocation", "invocations", MAPPER.createArrayNode());
		assertRejected(empty, "empty:trace.event.invocations");

		ObjectNode duplicate = loadFixture("capability_invoked_valid.json");
		replaceFacts(duplicate, "invocation", "invocations", MAPPER.createArrayNode()
				.add(fact("invocation_id", "i-1", "SUCCESS")).add(fact("invocation_id", "i-1", "FAILURE")));
		assertRejected(duplicate, "duplicate:trace.event.invocations[1].invocation_id");

		ObjectNode noId = loadFixture("capability_invoked_valid.json");
		replaceFacts(noId, "invocation", "invocations", MAPPER.createArrayNode().add(fact(null, null, "SUCCESS")));
		assertRejected(noId, "missing:trace.event.invocations[0].invocation_id");

		ObjectNode notArray = loadFixture("capability_invoked_valid.json");
		remove(notArray, "trace", "event", "invocation");
		((ObjectNode) at(notArray, "trace", "event")).set("invocations", MAPPER.createObjectNode());
		assertRejected(notArray, "type:trace.event.invocations");
	}

	@Test
	void testCapabilityInvoked_recordLevelOutcomeAndBothForms_rejected() throws IOException {
		ObjectNode singular = loadFixture("capability_invoked_valid.json");
		((ObjectNode) at(singular, "trace", "event")).put("outcome", "SUCCESS");
		assertRejected(singular, "forbidden:trace.event.outcome");

		ObjectNode batch = loadFixture("capability_invoked_batch_valid.json");
		((ObjectNode) at(batch, "trace", "event")).put("outcome", "SUCCESS");
		assertRejected(batch, "forbidden:trace.event.outcome");

		ObjectNode both = loadFixture("capability_invoked_valid.json");
		((ObjectNode) at(both, "trace", "event")).set("invocations",
				MAPPER.createArrayNode().add(fact("invocation_id", "i-1", "SUCCESS")));
		assertRejected(both, "forbidden:trace.event.invocations");

		ObjectNode neither = loadFixture("capability_invoked_valid.json");
		remove(neither, "trace", "event", "invocation");
		assertRejected(neither, "missing:trace.event.invocation");
	}

	@Test
	void testCapabilityInvoked_capabilityIdsAndResourceId_forbidden() throws IOException {
		ObjectNode legacy = loadFixture("capability_invoked_valid.json");
		((ObjectNode) at(legacy, "trace", "event")).set("capability_ids", MAPPER.createArrayNode()
				.add(MAPPER.createObjectNode().put("capability_id", "invoke:payment-authorization")
						.put("outcome", "ALLOW")));
		assertRejected(legacy, "forbidden:trace.event.capability_ids");

		ObjectNode resourceId = loadFixture("capability_invoked_valid.json");
		set(resourceId, TextNode.valueOf("x"), "trace", "event", "invocation", "resource_id");
		assertRejected(resourceId, "forbidden:trace.event.invocation.resource_id");
	}

	// -- RUNTIME_EFFECT -------------------------------------------------------------------------

	@Test
	void testRuntimeEffect_designExample_populatesCorrelationInputsNoWarning() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("runtime_effect_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("RUNTIME_EFFECT", result.getEventKind());
		assertTrue(result.getWarnings().isEmpty());
	}

	@Test
	void testRuntimeEffect_withCapabilityIds_throwsForbidden() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		ArrayNode capabilityIds = MAPPER.createArrayNode();
		ObjectNode entry = MAPPER.createObjectNode();
		entry.put("capability_id", "invoke:payment-authorization");
		entry.put("outcome", "ALLOW");
		capabilityIds.add(entry);
		set(root, capabilityIds, "trace", "event", "capability_ids");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("forbidden:trace.event.capability_ids", ex.getReason());
	}

	@Test
	void testRuntimeEffect_withDecisionOrInvocationFacts_throwsForbidden() throws IOException {
		String[] singular = { "decision", "invocation" };
		for (String field : singular) {
			ObjectNode root = loadFixture("runtime_effect_valid.json");
			((ObjectNode) at(root, "trace", "event")).set(field, fact(null, null, "ALLOW"));
			assertRejected(root, "forbidden:trace.event." + field);
		}
		String[][] plural = { { "decisions", "decision_id" }, { "invocations", "invocation_id" } };
		for (String[] field : plural) {
			ObjectNode root = loadFixture("runtime_effect_valid.json");
			((ObjectNode) at(root, "trace", "event")).set(field[0],
					MAPPER.createArrayNode().add(fact(field[1], "x-1", "ALLOW")));
			assertRejected(root, "forbidden:trace.event." + field[0]);
		}
	}

	@Test
	void testRuntimeEffect_subjectOptional_validatedWhenPresent() throws IOException {
		registry.validate(parseAndCommonValidate(loadFixture("runtime_effect_valid.json")));
		registry.validate(parseAndCommonValidate(loadFixture("runtime_effect_with_subject_valid.json")));

		ObjectNode longId = loadFixture("runtime_effect_with_subject_valid.json");
		set(longId, TextNode.valueOf("w".repeat(256)), "trace", "subject", "workload_id");
		assertRejected(longId, "length:trace.subject.workload_id");

		ObjectNode notObject = loadFixture("runtime_effect_valid.json");
		((ObjectNode) at(notObject, "trace")).put("subject", "x");
		assertRejected(notObject, "type:trace.subject");

		ObjectNode noWorkloadIdIsFine = loadFixture("runtime_effect_with_subject_valid.json");
		remove(noWorkloadIdIsFine, "trace", "subject", "workload_id");
		registry.validate(parseAndCommonValidate(noWorkloadIdIsFine));
	}

	@Test
	void testRuntimeEffect_withPolicy_throwsForbidden() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		ObjectNode policy = MAPPER.createObjectNode();
		policy.put("policy_store_id", "x");
		set(root, policy, "trace", "policy");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("forbidden:trace.policy", ex.getReason());
	}

	@Test
	void testRuntimeEffect_missingOutcome_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		remove(root, "trace", "event", "outcome");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.event.outcome", ex.getReason());
	}

	@Test
	void testRuntimeEffect_invalidResultDigest_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		set(root, TextNode.valueOf("not-a-digest"), "trace", "event", "result_digest");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.result_digest", ex.getReason());
	}

	@Test
	void testRuntimeEffect_withoutProducedEffectParent_acceptedWithWarning() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		remove(root, "parent_record_ids");

		ParsedAssertion assertion = parseAndCommonValidate(root);
		CorrelationInputs result = registry.validate(assertion);

		assertEquals(1, result.getWarnings().size());
		assertEquals("runtime_effect_without_produced_effect_parent", result.getWarnings().get(0));
	}

	@Test
	void testRuntimeEffect_withUnrelatedParentOnly_acceptedWithWarning() throws IOException {
		ObjectNode root = loadFixture("runtime_effect_valid.json");
		set(root, TextNode.valueOf("unrelated"), "parent_record_ids", "0", "relationship_type");

		ParsedAssertion assertion = parseAndCommonValidate(root);
		CorrelationInputs result = registry.validate(assertion);

		assertTrue(result.getWarnings().contains("runtime_effect_without_produced_effect_parent"));
	}

	// -- tokens[] (common to every kind) ---------------------------------------------------------

	@Test
	void testTokens_bothJtiAndFingerprintPresent_throwsTokenRefIdentity() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("abcdefghijklmnopqrst"), "trace", "event", "tokens", "0", "fingerprint");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("token_ref_identity", ex.getReason());
	}

	@Test
	void testTokens_neitherJtiNorFingerprintPresent_throwsTokenRefIdentity() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "event", "tokens", "0", "jti");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("token_ref_identity", ex.getReason());
	}

	@Test
	void testTokens_fingerprintInsteadOfJti_accepted() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "event", "tokens", "0", "jti");
		set(root, TextNode.valueOf("abcdefghijklmnopqrst"), "trace", "event", "tokens", "0", "fingerprint");

		ParsedAssertion assertion = parseAndCommonValidate(root);
		CorrelationInputs result = registry.validate(assertion);

		assertEquals("abcdefghijklmnopqrst", result.getTokenRefs().get(0).getFingerprint());
		assertNull(result.getTokenRefs().get(0).getJti());
	}

	@Test
	void testTokens_fingerprintWrongFormat_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "event", "tokens", "0", "jti");
		set(root, TextNode.valueOf("!!!"), "trace", "event", "tokens", "0", "fingerprint");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.tokens[0].fingerprint", ex.getReason());
	}

	@Test
	void testTokens_issuerHttpNonLocalhost_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("http://accounts.example.org"), "trace", "event", "tokens", "0", "issuer");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.tokens[0].issuer", ex.getReason());
	}

	@Test
	void testTokens_issuerHttpLocalhost_accepted() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("http://localhost:8080"), "trace", "event", "tokens", "0", "issuer");

		ParsedAssertion assertion = parseAndCommonValidate(root);

		registry.validate(assertion);
	}

	@Test
	void testTokens_rawJwtAsJti_throwsRawTokenMaterial() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dGhpc2lzYXNpZ25hdHVyZQ"), "trace",
				"event", "tokens", "0", "jti");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("raw_token_material", ex.getReason());
	}

	@Test
	void testTokens_duplicateJtiDifferentIssuer_producesTwoDistinctTokenRefs() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		ArrayNode tokens = (ArrayNode) at(root, "trace", "event", "tokens");
		ObjectNode second = MAPPER.createObjectNode();
		second.put("issuer", "https://accounts.other.example.org");
		second.put("token_type", "access_token");
		second.put("jti", "9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e");
		tokens.add(second);

		ParsedAssertion assertion = parseAndCommonValidate(root);
		CorrelationInputs result = registry.validate(assertion);

		assertEquals(2, result.getTokenRefs().size());
		TokenRef first = result.getTokenRefs().get(0);
		TokenRef sameJtiDifferentIssuer = result.getTokenRefs().get(1);
		assertEquals(first.getJti(), sameJtiDifferentIssuer.getJti());
		assertFalse(first.getIssuer().equals(sameJtiDifferentIssuer.getIssuer()));
	}

	// -- registry ---------------------------------------------------------------------------------

	@Test
	void testRegistry_unregisteredKind_throwsIllegalState() throws IOException {
		EventKindValidatorRegistry emptyRegistry = new EventKindValidatorRegistry(java.util.Collections.emptyList());
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("authorization_decision_valid.json"));

		assertThrows(IllegalStateException.class, () -> emptyRegistry.validate(assertion));
	}

}
