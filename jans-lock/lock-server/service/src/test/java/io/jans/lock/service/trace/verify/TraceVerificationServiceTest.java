/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.verify;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.lock.trace.crypto.bc.JcsCanonicalizer;
import io.jans.lock.trace.crypto.bc.Ed25519Capability;
import io.jans.lock.trace.crypto.bc.Ed25519PublicKeys;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;
import io.jans.lock.trace.crypto.bc.Ed25519Verifier;
import io.jans.lock.trace.crypto.bc.JavaTraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.SignedAssertionFactory;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;

/**
 * Tests for {@link TraceVerificationService}: design §5 steps 3–6 wired with the real parser,
 * validators, canonicalizer and an {@link InMemoryTraceStore}-backed {@link ProducerKeyRegistry}.
 * Mocks are used only where a test must prove that a step did <em>not</em>
 * run.
 */
class TraceVerificationServiceTest extends BaseLockServiceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String DOMAIN = "domain-1";

	private static final String CLIENT_ID = "2200.abcd";

	private static final String NODE_ID = "node-1";

	/** The fixture's signed {@code producer}. */
	private static final String PRODUCER = "cedarling-fleet-1";

	/** The fixture's {@code kid}. */
	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String RECORD_ID = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41";

	private static final long KEY_VALID_FROM_MS = 1_000L;

	private static final long RECEIVED_AT_MS = 1_800_000_000_000L;

	private InMemoryTraceStore store;

	private ProducerKeyRegistry keyRegistry;

	private TraceAssertionParser parser;

	private CommonAssertionValidator commonValidator;

	private TraceCryptoProvider cryptoProvider;

	private TraceVerificationService service;

	private KeyPair keyPair;

	@BeforeAll
	static void installProvider() {
		TraceTestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(new TraceConfiguration());

		parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		cryptoProvider = TraceTestCrypto.javaProvider();
		assertTrue(cryptoProvider.isAvailable(), "Ed25519 must be available for these tests");
		setField(parser, "cryptoProvider", cryptoProvider);

		commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);

		store = new InMemoryTraceStore();
		keyRegistry = new ProducerKeyRegistry();
		setField(keyRegistry, "traceStore", store);
		setField(keyRegistry, "log", LoggerFactory.getLogger(ProducerKeyRegistry.class));
		setField(keyRegistry, "cryptoProvider", cryptoProvider);

		service = newService(cryptoProvider, keyRegistry, parser);

		keyPair = TraceTestKeys.generateKeyPair();
		keyRegistry.register(DOMAIN, PRODUCER, KID, TraceTestKeys.toJwk(keyPair.getPublic()),
				new ArrayList<>(TraceConstants.EVENT_KINDS), KEY_VALID_FROM_MS, null, CLIENT_ID, KEY_VALID_FROM_MS);
	}

	/**
	 * Wires the private {@code @Inject} fields by reflection (the service carries no test-seam
	 * setters), the same way {@code OrmTraceStores} and {@code EventKindValidatorsTest} do.
	 */
	private TraceVerificationService newService(TraceCryptoProvider provider, ProducerKeyRegistry registry,
			TraceAssertionParser assertionParser) {
		TraceVerificationService result = new TraceVerificationService();
		setField(result, "log", LoggerFactory.getLogger(TraceVerificationService.class));
		setField(result, "cryptoProvider", provider);
		setField(result, "parser", assertionParser);
		setField(result, "commonValidator", commonValidator);
		setField(result, "eventKindRegistry", new EventKindValidatorRegistry());
		setField(result, "keyRegistry", registry);
		return result;
	}

	// -- positive -------------------------------------------------------------------------------

	@Test
	void testVerify_ValidSignedAssertion_ReturnsVerifiedAssertion() throws Exception {
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		VerifiedAssertion verified = service.verify(body(json), ctx("*"));

		assertEquals(PRODUCER, verified.getProducer());
		assertEquals(RECORD_ID, verified.getRecordId());
		assertEquals(DOMAIN, verified.getIdentity().getDomainId());
		assertEquals(PRODUCER, verified.getIdentity().getProducerId());
		assertEquals(RECORD_ID, verified.getIdentity().getRecordId());
		assertEquals("AUTHORIZATION_DECISION", verified.getInputs().getEventKind());
		assertEquals(json, verified.getParsed().getRawText());

		assertTrue(verified.getVerification().isSignatureValid());
		assertEquals(KID, verified.getVerification().getKeyId());
		assertEquals(RECEIVED_AT_MS, verified.getVerification().getVerifiedAtMs());
		assertEquals(Ed25519PublicKeys.ALGORITHM, verified.getVerification().getAlgorithm());
		assertEquals(Ed25519PublicKeys.thumbprint(TraceTestKeys.toJwk(keyPair.getPublic())),
				verified.getVerification().getKeyThumbprint());
		assertEquals(Boolean.TRUE, verified.getVerification().getKeyAuthorized());

		assertEquals(independentContentDigest(json), verified.getContentDigest());
	}

	@Test
	void testVerify_EachEventKindFixture_Verifies() {
		List<SignedAssertionFactory> fixtures = Arrays.asList(SignedAssertionFactory.authorizationDecision(),
				SignedAssertionFactory.capabilityInvoked(), SignedAssertionFactory.runtimeEffect());

		for (SignedAssertionFactory fixture : fixtures) {
			// The three fixtures name different producers/kids; point them all at the registered key.
			String json = fixture.producer(PRODUCER).kid(KID).sign(keyPair.getPrivate());

			VerifiedAssertion verified = service.verify(body(json), ctx("*"));

			assertTrue(verified.getVerification().isSignatureValid());
			assertEquals(fixture.tree().path("trace").path("event_kind").textValue(),
					verified.getInputs().getEventKind());
		}
	}

	@Test
	void testVerify_EventKindOutsideKeyScope_UnknownProducerKeyBeforeSignature() {
		String adOnlyKid = "ad-only-kid";
		KeyPair adOnlyKey = TraceTestKeys.generateKeyPair();
		keyRegistry.register(DOMAIN, PRODUCER, adOnlyKid, TraceTestKeys.toJwk(adOnlyKey.getPublic()),
				List.of(TraceConstants.EVENT_KIND_AUTHORIZATION_DECISION), KEY_VALID_FROM_MS, null, CLIENT_ID,
				KEY_VALID_FROM_MS);

		String inScope = SignedAssertionFactory.authorizationDecision().kid(adOnlyKid).sign(adOnlyKey.getPrivate());
		assertEquals("AUTHORIZATION_DECISION",
				service.verify(body(inScope), ctx("*")).getInputs().getEventKind());

		// Signed by the wrong private key: if the scope check ran after the signature check this
		// would be invalid_signature, not unknown_producer_key.
		String outOfScope = SignedAssertionFactory.capabilityInvoked().producer(PRODUCER).kid(adOnlyKid)
				.sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(outOfScope), ctx("*")));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals("event_kind_not_authorized", ex.getReason());
	}

	@Test
	void testVerify_ExplicitProducerAllowlist_Verifies() {
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		VerifiedAssertion verified = service.verify(body(json), ctx("other-producer", PRODUCER));

		assertEquals(PRODUCER, verified.getProducer());
	}

	// -- canonicalization properties ------------------------------------------------------------

	@Test
	void testVerify_ReorderedMembersAndWhitespace_SameDigestSameSignature() throws Exception {
		String compact = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());
		String reordered = SignedAssertionFactory.reorderAndPrettyPrint(compact);
		assertNotEquals(compact, reordered);

		VerifiedAssertion fromCompact = service.verify(body(compact), ctx("*"));
		VerifiedAssertion fromReordered = service.verify(body(reordered), ctx("*"));

		assertEquals(fromCompact.getContentDigest(), fromReordered.getContentDigest());
		assertEquals(independentContentDigest(reordered), fromReordered.getContentDigest());
		assertEquals(fromCompact.getParsed().getSignature(), fromReordered.getParsed().getSignature());
		assertEquals(compact, fromCompact.getParsed().getRawText());
		assertEquals(reordered, fromReordered.getParsed().getRawText());
	}

	@Test
	void testVerify_TwoDifferentAssertions_DifferentDigests() {
		String first = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());
		String second = SignedAssertionFactory.authorizationDecision()
				.recordId("0e2a3c58-8b6d-4f37-9d15-6c1a2b3c4d5e").sign(keyPair.getPrivate());

		VerifiedAssertion firstVerified = service.verify(body(first), ctx("*"));
		VerifiedAssertion secondVerified = service.verify(body(second), ctx("*"));

		assertNotEquals(firstVerified.getContentDigest(), secondVerified.getContentDigest());
	}

	// -- negative matrix ------------------------------------------------------------------------

	@Test
	void testVerify_TamperedFieldAfterSigning_InvalidSignature() {
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());
		String tampered = SignedAssertionFactory.tamperAfterSigning(json, "DENY", "trace", "event", "decision", "outcome");

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(tampered), ctx("*")));

		assertEquals(TraceErrorResponseType.INVALID_SIGNATURE, ex.getErrorId());
		assertEquals(TraceVerificationService.REASON_SIGNATURE_MISMATCH, ex.getReason());
	}

	@Test
	void testVerify_SignatureOverFullAssertionIncludingSignature_InvalidSignature() {
		String json = SignedAssertionFactory.authorizationDecision().signWrongScope(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("*")));

		assertEquals(TraceErrorResponseType.INVALID_SIGNATURE, ex.getErrorId());
		assertEquals(TraceVerificationService.REASON_SIGNATURE_MISMATCH, ex.getReason());
	}

	@Test
	void testVerify_SignedWithDifferentKey_InvalidSignature() {
		KeyPair otherKeyPair = TraceTestKeys.generateKeyPair();
		String json = SignedAssertionFactory.authorizationDecision().sign(otherKeyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("*")));

		assertEquals(TraceErrorResponseType.INVALID_SIGNATURE, ex.getErrorId());
		assertEquals(TraceVerificationService.REASON_SIGNATURE_MISMATCH, ex.getReason());
	}

	@Test
	void testVerify_UnknownKid_UnknownProducerKey() {
		String json = SignedAssertionFactory.authorizationDecision().kid("unknown-kid").sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("*")));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
	}

	@Test
	void testVerify_RevokedKey_KeyNotValid() {
		keyRegistry.revoke(DOMAIN, PRODUCER, KID, RECEIVED_AT_MS - 1);
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("*")));

		assertEquals(TraceErrorResponseType.KEY_NOT_VALID, ex.getErrorId());
		assertEquals("revoked", ex.getReason());
	}

	@Test
	void testVerify_KeyRegisteredInOtherDomain_UnknownProducerKey() {
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());
		TraceRequestContext otherDomain = new TraceRequestContext(CLIENT_ID, "domain-2",
				Collections.singletonList("*"), RECEIVED_AT_MS, NODE_ID);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), otherDomain));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
	}

	@Test
	void testVerify_ProducerNotAllowed_NoKeyLookupAndNoCrypto() {
		ProducerKeyRegistry mockRegistry = mock(ProducerKeyRegistry.class);
		TraceCryptoProvider spyProvider = spy(cryptoProvider);
		TraceVerificationService guarded = newService(spyProvider, mockRegistry, parser);
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> guarded.verify(body(json), ctx("other-producer")));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
		verifyNoInteractions(mockRegistry);
		verify(spyProvider, never()).verifyAndDigest(any(), any(), any());
	}

	@Test
	void testVerify_ProducerNotAllowedAndEventKindInvalid_ForwardingCheckedFirst() {
		// The forwarding check (step 4) precedes event-kind validation (step 5): an assertion that
		// would fail §7.2 is still rejected as producer_not_allowed for an unbound producer.
		String json = SignedAssertionFactory.authorizationDecision().withoutField("trace", "policy", "bundle_hash")
				.sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("other-producer")));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
	}

	@Test
	void testVerify_EventKindViolation_InvalidAssertionBeforeKeyLookup() {
		ProducerKeyRegistry mockRegistry = mock(ProducerKeyRegistry.class);
		TraceVerificationService guarded = newService(cryptoProvider, mockRegistry, parser);
		String json = SignedAssertionFactory.authorizationDecision().withoutField("trace", "policy", "bundle_hash")
				.sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> guarded.verify(body(json), ctx("*")));

		assertEquals(TraceErrorResponseType.INVALID_ASSERTION, ex.getErrorId());
		assertEquals("missing:trace.policy.bundle_hash", ex.getReason());
		verifyNoInteractions(mockRegistry);
	}

	@Test
	void testVerify_MalformedJson_InvalidRequest() {
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body("{not json"), ctx("*")));

		assertEquals(TraceErrorResponseType.INVALID_REQUEST, ex.getErrorId());
	}

	@Test
	void testVerify_CapabilityUnavailable_CryptoUnavailableBeforeParsing() {
		TraceAssertionParser mockParser = mock(TraceAssertionParser.class);
		// A never-probed capability reports Ed25519 as unavailable.
		TraceVerificationService guarded = newService(
				new JavaTraceCryptoProvider(new Ed25519Capability(), new Ed25519Verifier()), keyRegistry, mockParser);
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		TraceCryptoException ex = assertThrows(TraceCryptoException.class,
				() -> guarded.verify(body(json), ctx("*")));

		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, ex.getReason());
		verifyNoInteractions(mockParser);
	}

	@Test
	void testVerify_ValidationExceptionPropagatesUnchanged() {
		TraceAssertionParser mockParser = mock(TraceAssertionParser.class);
		TraceValidationException original = new TraceValidationException(TraceErrorResponseType.INVALID_REQUEST,
				"body_too_large");
		when(mockParser.parse(any(InputStream.class))).thenThrow(original);
		TraceVerificationService guarded = newService(cryptoProvider, keyRegistry, mockParser);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> guarded.verify(body("{}"), ctx("*")));

		assertSame(original, ex);
	}

	// -- assertion_raw / assertion fidelity -------------------------------------------------------------

	/**
	 * Signs a fixture that carries Unicode and number members under unreserved top-level names.
	 * The signature covers the JCS form, so any textual spelling of the same values must verify.
	 */
	private String signedFidelityAssertion() throws Exception {
		ObjectNode extra = (ObjectNode) MAPPER.readTree("{\"x_emoji\":\"\uD83D\uDE00\",\"x_nfc\":\"\u00e9\","
				+ "\"x_nfd\":\"e\u0301\",\"x_sep\":\"a\u2028b\u2029c\",\"x_ctl\":\"a\\u0000b\\u001fc\\u007fd\","
				+ "\"x_exp\":1e2,\"x_frac\":1.5,\"x_zero\":0,\"x_big_exp\":1e21,\"x_small\":0.000001,\"x_tiny\":1e-7,"
				+ "\"x_safe\":9007199254740991,\"x_over\":9007199254740993,"
				+ "\"x_huge\":123456789012345678901234567890,\"x_arr\":[3,1,2,{\"b\":1,\"a\":[]}]}");
		SignedAssertionFactory factory = SignedAssertionFactory.authorizationDecision();
		extra.fields().forEachRemaining(member -> factory.withField(member.getValue(), member.getKey()));
		return factory.sign(keyPair.getPrivate());
	}

	/** Every character above U+007F written as a {@code \\uXXXX} escape (UTF-16 code units). */
	private static String asciiEscaped(String json) {
		StringBuilder out = new StringBuilder();
		for (char c : json.toCharArray()) {
			if (c > 0x7f) {
				out.append(String.format("\\u%04x", (int) c));
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	@Test
	void testVerify_EscapedLiteralAndReSpelledNumbers_VerifyWithSameDigestAndRawPreserved() throws Exception {
		String compact = signedFidelityAssertion();
		assertTrue(compact.contains("\"x_exp\":100.0"), compact);
		assertTrue(compact.contains("\"x_zero\":0,"), compact);
		assertTrue(compact.contains("\"x_frac\":1.5"), compact);

		String escaped = asciiEscaped(compact);
		String respelled = compact.replace("\"x_exp\":100.0", "\"x_exp\":1E+2").replace("\"x_zero\":0,", "\"x_zero\":-0,")
				.replace("\"x_frac\":1.5", "\"x_frac\":15e-1");
		String reordered = SignedAssertionFactory.reorderAndPrettyPrint(compact);
		assertNotEquals(compact, escaped);
		assertNotEquals(compact, respelled);

		VerifiedAssertion first = service.verify(body(compact), ctx("*"));
		for (String variant : new String[] { escaped, respelled, reordered, asciiEscaped(respelled) }) {
			VerifiedAssertion other = service.verify(body(variant), ctx("*"));

			assertTrue(other.getVerification().isSignatureValid());
			assertEquals(first.getContentDigest(), other.getContentDigest());
			assertEquals(variant, other.getParsed().getRawText());
			assertArrayEquals(variant.getBytes(StandardCharsets.UTF_8),
					other.getParsed().getRawText().getBytes(StandardCharsets.UTF_8));
		}
		assertEquals(independentContentDigest(escaped), first.getContentDigest());
	}

	@Test
	void testVerify_ReEmittedAssertion_JcsAndDigestEqualOriginal() throws Exception {
		String compact = signedFidelityAssertion();
		for (String received : new String[] { compact, asciiEscaped(compact) }) {
			VerifiedAssertion verified = service.verify(body(received), ctx("*"));

			// The response hop: ASSERTION_MAPPER re-parses the stored text, the JSON provider writes
			// the tree, and a client parses that body.
			JsonNode assertion = MAPPER.readTree(verified.getParsed().getRawText());
			JsonNode wire = MAPPER.readTree(MAPPER.writeValueAsString(assertion));

			assertEquals(JcsCanonicalizer.canonicalize(verified.getParsed().getRoot()), JcsCanonicalizer.canonicalize(wire));
			assertEquals(verified.getContentDigest(), CanonicalHashes.sha256PrefixedOfJcs(wire));
			assertEquals(verified.getContentDigest(), CanonicalHashes.sha256PrefixedOfJcs(assertion));
		}
	}

	@Test
	void testVerify_FidelityAssertion_JcsFormsOfNumbersAndStrings() throws Exception {
		VerifiedAssertion verified = service.verify(body(signedFidelityAssertion()), ctx("*"));

		String jcs = JcsCanonicalizer.canonicalize(verified.getParsed().getRoot());

		assertTrue(jcs.contains("\"x_exp\":100,"), jcs);
		assertTrue(jcs.contains("\"x_frac\":1.5,"), jcs);
		assertTrue(jcs.endsWith("\"x_zero\":0}"), jcs);
		assertTrue(jcs.contains("\"x_big_exp\":1e+21,"), jcs);
		assertTrue(jcs.contains("\"x_small\":0.000001,"), jcs);
		assertTrue(jcs.contains("\"x_tiny\":1e-7,"), jcs);
		assertTrue(jcs.contains("\"x_safe\":9007199254740991,"), jcs);
		// Documented limit: integers above 2^53-1 keep their exact digits; an ECMAScript verifier
		// would print 9007199254740992 here.
		assertTrue(jcs.contains("\"x_over\":9007199254740993,"), jcs);
		assertTrue(jcs.contains("\"x_huge\":123456789012345678901234567890,"), jcs);
		assertTrue(jcs.contains("\"x_ctl\":\"a\\u0000b\\u001fc\u007fd\""), jcs);
		assertTrue(jcs.contains("\"x_sep\":\"a\u2028b\u2029c\""), jcs);
		assertTrue(jcs.contains("\"x_arr\":[3,1,2,{\"a\":[],\"b\":1}]"), jcs);
	}

	// -- helpers ------------------------------------------------------------------------------------

	private static InputStream body(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	private static TraceRequestContext ctx(String... allowedProducerIds) {
		return new TraceRequestContext(CLIENT_ID, DOMAIN, Arrays.asList(allowedProducerIds), RECEIVED_AT_MS, NODE_ID);
	}

	/**
	 * The content digest computed without {@code CanonicalHashes}: plain JDK SHA-256 over the
	 * UTF-8 JCS form of the whole assertion, lower-case hex, {@code sha256:} prefix.
	 */
	private static String independentContentDigest(String json) throws Exception {
		byte[] canonical = JcsCanonicalizer.canonicalizeToUtf8(MAPPER.readTree(json));
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);
		return "sha256:" + Hex.encodeHexString(hash);
	}

}
