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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.TokenRef;
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
class EventKindValidatorsTest {

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

	private static void setField(Object target, String fieldName, Object value) {
		try {
			java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
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
		ParsedAssertion assertion = parseAndCommonValidate(root);
		return assertThrows(TraceValidationException.class, () -> registry.validate(assertion));
	}

	// -- AUTHORIZATION_DECISION --------------------------------------------------------------

	@Test
	void testAuthorizationDecision_designExample_populatesCorrelationInputs() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("authorization_decision_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("AUTHORIZATION_DECISION", result.getEventKind());
		assertEquals("spiffe://example.org/agent/planner", result.getExecutionAuthority());
		assertEquals("exec-01JABCXYZQK8P5N9F2C7R3T4V6", result.getTraceExecutionId());
		assertEquals(Arrays.asList("invoke:payment-authorization"), result.getCapabilityIds());
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
	 * Acceptance criterion 1, literally: task 08's own design §7 example fixture (not this task's
	 * near-copy) validates as {@code AUTHORIZATION_DECISION} and yields the specified
	 * {@code capabilityIds} and {@code TokenRef}.
	 */
	@Test
	void testAuthorizationDecision_task08DesignExampleFixture_validatesAsSpecified() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("valid-authorization-decision.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals(Arrays.asList("invoke:payment-authorization"), result.getCapabilityIds());
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
		set(root, TextNode.valueOf("MAYBE"), "trace", "event", "outcome");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.outcome", ex.getReason());
	}

	@Test
	void testAuthorizationDecision_missingRuntimePdpId_throwsMissing() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		remove(root, "trace", "runtime", "pdp_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.runtime.pdp_id", ex.getReason());
	}

	// -- capability_ids[] (shared by AUTHORIZATION_DECISION / CAPABILITY_INVOKED) --------------

	@Test
	void testCapabilityIds_empty_throwsEmpty() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, MAPPER.createArrayNode(), "trace", "event", "capability_ids");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("empty:trace.event.capability_ids", ex.getReason());
	}

	@Test
	void testCapabilityIds_entryInvalidOutcome_throwsInvalid() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		set(root, TextNode.valueOf("MAYBE"), "trace", "event", "capability_ids", "0", "outcome");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("invalid:trace.event.capability_ids[0].outcome", ex.getReason());
	}

	@Test
	void testCapabilityIds_capabilityIdTooLong_throwsLength() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		StringBuilder tooLong = new StringBuilder();
		for (int i = 0; i < 513; i++) {
			tooLong.append('a');
		}
		set(root, TextNode.valueOf(tooLong.toString()), "trace", "event", "capability_ids", "0", "capability_id");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("length:trace.event.capability_ids[0].capability_id", ex.getReason());
	}

	@Test
	void testCapabilityIds_duplicateCapabilityId_deduplicated() throws IOException {
		ObjectNode root = loadFixture("authorization_decision_valid.json");
		ArrayNode capabilityIds = (ArrayNode) at(root, "trace", "event", "capability_ids");
		ObjectNode duplicate = MAPPER.createObjectNode();
		duplicate.put("capability_id", "invoke:payment-authorization");
		duplicate.put("outcome", "ALLOW");
		capabilityIds.add(duplicate);

		ParsedAssertion assertion = parseAndCommonValidate(root);
		CorrelationInputs result = registry.validate(assertion);

		assertEquals(Arrays.asList("invoke:payment-authorization"), result.getCapabilityIds());
	}

	// -- CAPABILITY_INVOKED ---------------------------------------------------------------------

	@Test
	void testCapabilityInvoked_designExample_populatesCorrelationInputs() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("capability_invoked_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("CAPABILITY_INVOKED", result.getEventKind());
		assertEquals(Arrays.asList("invoke:payment-authorization"), result.getCapabilityIds());
		assertEquals(1, result.getTokenRefs().size());
	}

	@Test
	void testCapabilityInvoked_withPolicy_throwsForbiddenPolicy() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		ObjectNode policy = MAPPER.createObjectNode();
		policy.put("policy_store_id", "x");
		set(root, policy, "trace", "policy");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("forbidden:trace.policy", ex.getReason());
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
	void testCapabilityInvoked_outcomeTooLong_throwsLength() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		StringBuilder tooLong = new StringBuilder();
		for (int i = 0; i < 65; i++) {
			tooLong.append('a');
		}
		set(root, TextNode.valueOf(tooLong.toString()), "trace", "event", "outcome");

		TraceValidationException ex = validateAndCapture(root);

		assertEquals(TraceValidationException.ERROR_INVALID_ASSERTION, ex.getErrorId());
		assertEquals("length:trace.event.outcome", ex.getReason());
	}

	@Test
	void testCapabilityInvoked_anyNonEmptyOutcome_accepted() throws IOException {
		ObjectNode root = loadFixture("capability_invoked_valid.json");
		set(root, TextNode.valueOf("some-custom-outcome"), "trace", "event", "outcome");

		ParsedAssertion assertion = parseAndCommonValidate(root);

		registry.validate(assertion);
	}

	// -- RUNTIME_EFFECT -------------------------------------------------------------------------

	@Test
	void testRuntimeEffect_designExample_populatesCorrelationInputsNoWarning() throws IOException {
		ParsedAssertion assertion = parseAndCommonValidate(loadFixture("runtime_effect_valid.json"));

		CorrelationInputs result = registry.validate(assertion);

		assertEquals("RUNTIME_EFFECT", result.getEventKind());
		assertTrue(result.getCapabilityIds().isEmpty());
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
