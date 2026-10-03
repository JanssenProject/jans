/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.canon.JcsCanonicalizer;
import io.jans.lock.service.trace.crypto.Ed25519Capability;
import io.jans.lock.service.trace.crypto.Ed25519PublicKeys;
import io.jans.lock.service.trace.crypto.Ed25519TestKeys;
import io.jans.lock.service.trace.crypto.Ed25519Verifier;
import io.jans.lock.service.trace.crypto.StrictBase64Url;
import io.jans.lock.service.trace.error.TraceCryptoException;
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
 * Mocks are used only where the acceptance criteria require proving that a step did <em>not</em>
 * run.
 */
class TraceVerificationServiceTest extends BaseLockServiceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String DOMAIN = "domain-1";

	private static final String CLIENT_ID = "2200.abcd";

	private static final String NODE_ID = "node-1";

	/** The fixture's signed {@code producer}. */
	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	/** The fixture's {@code kid}. */
	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String RECORD_ID = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41";

	private static final long KEY_VALID_FROM_MS = 1_000L;

	private static final long RECEIVED_AT_MS = 1_800_000_000_000L;

	private InMemoryTraceStore store;

	private ProducerKeyRegistry keyRegistry;

	private TraceAssertionParser parser;

	private CommonAssertionValidator commonValidator;

	private Ed25519Capability capability;

	private TraceVerificationService service;

	private KeyPair keyPair;

	@BeforeAll
	static void installProvider() {
		Ed25519TestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(new TraceConfiguration());

		parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);

		store = new InMemoryTraceStore();
		keyRegistry = new ProducerKeyRegistry();
		setField(keyRegistry, "traceStore", store);
		setField(keyRegistry, "log", LoggerFactory.getLogger(ProducerKeyRegistry.class));

		capability = new Ed25519Capability();
		capability.probe();
		assertTrue(capability.isAvailable(), "Ed25519 must be available for these tests");

		service = newService(capability, keyRegistry, new Ed25519Verifier(), parser);

		keyPair = Ed25519TestKeys.generateKeyPair();
		keyRegistry.register(DOMAIN, PRODUCER, KID, Ed25519TestKeys.toJwk(keyPair.getPublic()), KEY_VALID_FROM_MS,
				null, CLIENT_ID, KEY_VALID_FROM_MS);
	}

	/**
	 * Wires the private {@code @Inject} fields by reflection (the service carries no test-seam
	 * setters), the same way {@code OrmTraceStores} and {@code EventKindValidatorsTest} do.
	 */
	private TraceVerificationService newService(Ed25519Capability ed25519Capability, ProducerKeyRegistry registry,
			Ed25519Verifier verifier, TraceAssertionParser assertionParser) {
		TraceVerificationService result = new TraceVerificationService();
		setField(result, "log", LoggerFactory.getLogger(TraceVerificationService.class));
		setField(result, "capability", ed25519Capability);
		setField(result, "parser", assertionParser);
		setField(result, "commonValidator", commonValidator);
		setField(result, "eventKindRegistry", new EventKindValidatorRegistry());
		setField(result, "keyRegistry", registry);
		setField(result, "verifier", verifier);
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
		assertEquals(Arrays.asList("invoke:payment-authorization"), verified.getInputs().getCapabilityIds());
		assertEquals(json, verified.getParsed().getRawText());

		assertTrue(verified.getVerification().isSignatureValid());
		assertEquals(KID, verified.getVerification().getKeyId());
		assertEquals(RECEIVED_AT_MS, verified.getVerification().getVerifiedAtMs());
		assertEquals(Ed25519PublicKeys.ALGORITHM, verified.getVerification().getAlgorithm());

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
	void testVerify_SignedRuntimeEffectWithUnrepresentableExtension_RejectedBeforeKeyLookup() throws Exception {
		String safeNumber = "\"observation_count\":9007199254740991";
		String unsafeNumber = "\"observation_count\":9007199254740993";
		SignedAssertionFactory fixture = SignedAssertionFactory.runtimeEffect().producer(PRODUCER).kid(KID)
				.withField(9007199254740991L, "trace", "event", "observation_count");

		String safeJson = fixture.sign(keyPair.getPrivate());
		assertTrue(service.verify(body(safeJson), ctx("*")).getVerification().isSignatureValid());

		ObjectNode unsafeRoot = fixture.tree();
		String unsafeInput = JcsCanonicalizer.canonicalize(JcsCanonicalizer.withoutField(unsafeRoot, "signature"))
				.replace(safeNumber, unsafeNumber);
		assertTrue(unsafeInput.contains(unsafeNumber));
		byte[] signature = Ed25519TestKeys.sign(keyPair.getPrivate(), unsafeInput.getBytes(StandardCharsets.UTF_8));
		unsafeRoot.put("signature", StrictBase64Url.encode(signature));
		String unsafeJson = MAPPER.writeValueAsString(unsafeRoot).replace(safeNumber, unsafeNumber);
		assertTrue(unsafeJson.contains(unsafeNumber));

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(unsafeJson), ctx("*")));
		assertEquals(TraceErrorResponseType.INVALID_REQUEST, ex.getErrorId());
		assertEquals("not_canonicalizable", ex.getReason());
	}

	@Test
	void testVerify_ExplicitProducerAllowlist_Verifies() {
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		VerifiedAssertion verified = service.verify(body(json), ctx("other/2.0.0", PRODUCER));

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
		String tampered = SignedAssertionFactory.tamperAfterSigning(json, "DENY", "trace", "event", "outcome");

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
		KeyPair otherKeyPair = Ed25519TestKeys.generateKeyPair();
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
		Ed25519Verifier mockVerifier = mock(Ed25519Verifier.class);
		TraceVerificationService guarded = newService(capability, mockRegistry, mockVerifier, parser);
		String json = SignedAssertionFactory.authorizationDecision().sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> guarded.verify(body(json), ctx("other-producer/2.0.0")));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
		verifyNoInteractions(mockRegistry, mockVerifier);
	}

	@Test
	void testVerify_ProducerNotAllowedAndEventKindInvalid_ForwardingCheckedFirst() {
		// The forwarding check (step 4) precedes event-kind validation (step 5): an assertion that
		// would fail §7.2 is still rejected as producer_not_allowed for an unbound producer.
		String json = SignedAssertionFactory.authorizationDecision().withoutField("trace", "policy", "bundle_hash")
				.sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.verify(body(json), ctx("other-producer/2.0.0")));

		assertEquals(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, ex.getErrorId());
	}

	@Test
	void testVerify_EventKindViolation_InvalidAssertionBeforeKeyLookup() {
		ProducerKeyRegistry mockRegistry = mock(ProducerKeyRegistry.class);
		TraceVerificationService guarded = newService(capability, mockRegistry, new Ed25519Verifier(), parser);
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
		TraceVerificationService guarded = newService(new Ed25519Capability(), keyRegistry, new Ed25519Verifier(),
				mockParser);
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
		TraceVerificationService guarded = newService(capability, keyRegistry, new Ed25519Verifier(), mockParser);

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> guarded.verify(body("{}"), ctx("*")));

		assertSame(original, ex);
	}

	// -- helpers ------------------------------------------------------------------------------------

	private static InputStream body(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	private static TraceRequestContext ctx(String... allowedProducerIds) {
		return new TraceRequestContext(CLIENT_ID, DOMAIN, Arrays.asList(allowedProducerIds), RECEIVED_AT_MS, NODE_ID);
	}

	/**
	 * The D-13 content digest computed without {@code CanonicalHashes}: plain JDK SHA-256 over the
	 * UTF-8 JCS form of the whole assertion, lower-case hex, {@code sha256:} prefix.
	 */
	private static String independentContentDigest(String json) throws Exception {
		byte[] canonical = JcsCanonicalizer.canonicalizeToUtf8(MAPPER.readTree(json));
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);
		return "sha256:" + Hex.encodeHexString(hash);
	}

}
