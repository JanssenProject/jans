/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.as.model.error.IErrorType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.canon.CanonicalHashes;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.crypto.Ed25519PublicKeys;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.service.trace.testkit.TraceTestKeys;
import io.jans.lock.service.ws.rs.trace.TraceRestWebServiceImpl;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * MVP acceptance suite (design §15): one test method per numbered criterion, each quoting the §15
 * sentence it proves, run against the real service stack (parser, validators, canonicalizer,
 * crypto, registries, correlation, receipt chain, ingestion, retrieval) wired by
 * {@link TraceTestHarness} over {@code InMemoryTraceStore} (task 22).
 *
 * <p>Deterministic: every ingest uses {@link TraceTestHarness#ctx}, whose {@code received_at_ms}
 * and {@link io.jans.lock.service.trace.parse.CommonAssertionValidator} clock both come from
 * {@link TraceTestHarness#setNow(long)} (default {@link TraceTestHarness#DEFAULT_NOW_MS}); no test
 * reads the system clock.
 */
class TraceMvpAcceptanceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String DOMAIN = "domain-1";

	private static final String OTHER_DOMAIN = "domain-2";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final String GW_PRODUCER = "gateway-fleet-1";

	private static final String GW_KID = "gateway-fleet-1-2026-01";

	private static final String GW_INSTANCE = "gateway-001";

	private static final String GW_CHAIN = "chain-02JABC9Z0K";

	private static final String EXECUTION_AUTHORITY = "spiffe://example.org/agent/planner";

	private static final String TRACE_EXECUTION_ID = "exec-01JABCXYZQK8P5N9F2C7R3T4V6";

	private static final String TOKEN_ISSUER = "https://accounts.example.org";

	private static final String TOKEN_JTI = "9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e";

	private TraceTestHarness harness;

	@BeforeEach
	void setUp() {
		harness = new TraceTestHarness();
	}

	// -- fixtures -----------------------------------------------------------------------------------

	private KeyPair registerPrimary() {
		KeyPair keyPair = harness.registerKey(DOMAIN, PRODUCER, KID);
		harness.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN);
		return keyPair;
	}

	private KeyPair registerGateway() {
		KeyPair keyPair = harness.registerKey(DOMAIN, GW_PRODUCER, GW_KID);
		harness.registerChain(DOMAIN, GW_PRODUCER, GW_INSTANCE, GW_CHAIN);
		return keyPair;
	}

	private TraceRequestContext ctx() {
		return harness.ctx(DOMAIN);
	}

	private static String contentDigestOf(String json) {
		JsonNode tree = readTree(json);
		return CanonicalHashes.sha256PrefixedOfJcs(tree);
	}

	private static ObjectNode readTree(String json) {
		try {
			return (ObjectNode) MAPPER.readTree(json);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static ArrayNode tokensOf(ObjectNode tree) {
		return (ArrayNode) tree.at("/trace/event/tokens");
	}

	private static void addToken(ObjectNode tree, String issuer, String jti) {
		ObjectNode token = tokensOf(tree).addObject();
		token.put("issuer", issuer);
		token.put("token_type", "access_token");
		token.put("jti", jti);
	}

	// -- criterion 1 ----------------------------------------------------------------------------

	/**
	 * §15.1: "A valid record from a client permitted to use the TRACE API is verified, stored
	 * unchanged, indexed, and assigned exactly one receipt sequence."
	 */
	@Test
	void criterion01_validRecord_verifiedStoredIndexedSingleReceipt() {
		KeyPair keyPair = registerPrimary();
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());

		AcceptanceResult result = harness.ingest(json, ctx());

		assertTrue(result.isAccepted());
		assertFalse(result.isIdempotentReplay());
		assertEquals(PRODUCER, result.getProducerId());
		assertEquals(recordId, result.getRecordId());
		assertEquals(1L, result.getReceiptSequence());
		assertFalse(result.isCoverageGapFlag());
		assertFalse(result.isChainLinkFailureFlag());
		assertFalse(result.isEquivocationFlag());
		assertFalse(result.isLateFlag());

		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(json, stored.getAssertionRaw());
		assertEquals(result.getContentDigest(), stored.getContentDigest());
		assertEquals(1, harness.store().findRecordsByExecution(stored.getExecution(), 0, 100).size());
		assertEquals(1, harness.store().snapshot().size());
	}

	// -- task 24: stable producer identity -------------------------------------------------------

	@Test
	void stableProducerId_legacyNameSemverProducer_stillAccepted() {
		String legacyProducer = PRODUCER + "/1.0.0";
		KeyPair keyPair = harness.registerKey(DOMAIN, legacyProducer, KID);
		harness.registerChain(DOMAIN, legacyProducer, INSTANCE, CHAIN);
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().producer(legacyProducer).recordId(recordId)
				.genesis().sign(keyPair.getPrivate());

		AcceptanceResult result = harness.ingest(json, ctx());

		assertTrue(result.isAccepted());
		assertEquals(legacyProducer, result.getProducerId());
		assertEquals(legacyProducer, harness.getRecord(DOMAIN, recordId, legacyProducer).getIdentity().getProducerId());
	}

	@Test
	void stableProducerId_producerVersionPresent_storedUnchangedInAssertion() {
		KeyPair keyPair = registerPrimary();
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.withField("1.4.2+build.7", "producer_version").sign(keyPair.getPrivate());

		AcceptanceResult result = harness.ingest(json, ctx());

		assertTrue(result.isAccepted());
		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(json, stored.getAssertionRaw());
		assertEquals("1.4.2+build.7", readTree(stored.getAssertionRaw()).get("producer_version").textValue());
	}

	@Test
	void stableProducerId_producerVersionNotString_rejectedInvalidAssertion() {
		KeyPair keyPair = registerPrimary();
		String json = TraceRecordBuilder.authorizationDecision().recordId(TraceRecordBuilder.freshRecordId())
				.genesis().withField(7L, "producer_version").sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(json, ctx()));

		assertEquals(TraceErrorResponseType.INVALID_ASSERTION, ex.getErrorId());
		assertEquals("type:producer_version", ex.getReason());
	}

	// -- criterion 15 --------------------------------------------------------------------------

	/**
	 * §15.15: a record whose {@code event_kind} is outside the signing key's authorized set is
	 * rejected exactly like an unknown key, and nothing about it is stored or indexed.
	 */
	@Test
	void criterion15_eventKindOutsideKeyScope_rejectedAsUnknownKeyAndNeverRetrievable() {
		KeyPair keyPair = harness.registerKey(DOMAIN, PRODUCER, KID, List.of("AUTHORIZATION_DECISION"));
		harness.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN);

		String allowedRecordId = TraceRecordBuilder.freshRecordId();
		String allowedJson = TraceRecordBuilder.authorizationDecision().recordId(allowedRecordId).genesis()
				.sign(keyPair.getPrivate());
		assertTrue(harness.ingest(allowedJson, ctx()).isAccepted());

		String deniedRecordId = TraceRecordBuilder.freshRecordId();
		String deniedJson = TraceRecordBuilder.capabilityInvoked().producer(PRODUCER).kid(KID)
				.producerInstanceId(INSTANCE).producerChainId(CHAIN).recordId(deniedRecordId).sequenceNumber(2L)
				.sign(keyPair.getPrivate());

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(deniedJson, ctx()));

		assertEquals(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, ex.getErrorId());
		assertEquals("event_kind_not_authorized", ex.getReason());
		assertThrows(TraceValidationException.class, () -> harness.getRecord(DOMAIN, deniedRecordId, PRODUCER));
		assertEquals(1, harness.store().snapshot().size());
		assertTrue(harness.getRecord(DOMAIN, allowedRecordId, PRODUCER).getIdentity().getRecordId()
				.equals(allowedRecordId));
	}

	// -- criterion 16 --------------------------------------------------------------------------

	/**
	 * §15.16: the stored verification block records the exact key and the admission decision at
	 * ingestion, and a later revocation or expiry never changes it.
	 */
	@Test
	void criterion16_verificationBlock_unchangedAfterRevocation() {
		KeyPair keyPair = registerPrimary();
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());
		harness.ingest(json, ctx());

		VerificationResult first = harness.getRecord(DOMAIN, recordId, PRODUCER).getVerification();
		assertTrue(first.isSignatureValid());
		assertEquals(KID, first.getKeyId());
		assertEquals(Boolean.TRUE, first.getKeyAuthorized());
		assertEquals(Ed25519PublicKeys.thumbprint(TraceTestKeys.toJwk(keyPair.getPublic())),
				first.getKeyThumbprint());

		harness.revokeKey(DOMAIN, PRODUCER, KID, harness.now());
		harness.setNow(harness.now() + 10_000L);

		VerificationResult afterRevocation = harness.getRecord(DOMAIN, recordId, PRODUCER).getVerification();
		assertEquals(first, afterRevocation);
	}

	@Test
	void criterion16_verificationBlock_unchangedAfterValidUntilPasses() {
		long validUntil = harness.now() + 60_000L;
		KeyPair keyPair = harness.registerKey(DOMAIN, PRODUCER, KID, 0L, validUntil);
		harness.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN);
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());
		harness.ingest(json, ctx());
		VerificationResult first = harness.getRecord(DOMAIN, recordId, PRODUCER).getVerification();

		harness.setNow(validUntil + 1L);

		assertEquals(first, harness.getRecord(DOMAIN, recordId, PRODUCER).getVerification());
		assertEquals(Boolean.TRUE, first.getKeyAuthorized());
	}

	@Test
	void criterion16_sameKidInTwoProducersAndDomains_differentThumbprints_stablePerKey() {
		KeyPair primary = registerPrimary();
		KeyPair otherProducerSameKid = harness.registerKey(DOMAIN, GW_PRODUCER, KID);
		harness.registerChain(DOMAIN, GW_PRODUCER, GW_INSTANCE, GW_CHAIN);
		KeyPair otherDomainSameKid = harness.registerKey(OTHER_DOMAIN, PRODUCER, KID);
		harness.registerChain(OTHER_DOMAIN, PRODUCER, INSTANCE, CHAIN);

		String primaryId = TraceRecordBuilder.freshRecordId();
		harness.ingest(TraceRecordBuilder.authorizationDecision().recordId(primaryId).genesis()
				.sign(primary.getPrivate()), ctx());
		String gatewayId = TraceRecordBuilder.freshRecordId();
		harness.ingest(TraceRecordBuilder.authorizationDecision().producer(GW_PRODUCER).kid(KID).recordId(gatewayId)
				.producerInstanceId(GW_INSTANCE).producerChainId(GW_CHAIN).genesis()
				.sign(otherProducerSameKid.getPrivate()), ctx());
		String otherDomainId = TraceRecordBuilder.freshRecordId();
		harness.ingest(TraceRecordBuilder.authorizationDecision().recordId(otherDomainId).genesis()
				.sign(otherDomainSameKid.getPrivate()), harness.ctx(OTHER_DOMAIN));

		String t1 = harness.getRecord(DOMAIN, primaryId, PRODUCER).getVerification().getKeyThumbprint();
		String t2 = harness.getRecord(DOMAIN, gatewayId, GW_PRODUCER).getVerification().getKeyThumbprint();
		String t3 = harness.getRecord(OTHER_DOMAIN, otherDomainId, PRODUCER).getVerification().getKeyThumbprint();

		assertNotEquals(t1, t2);
		assertNotEquals(t1, t3);
		assertNotEquals(t2, t3);
		assertEquals(t1, harness.getRecord(DOMAIN, primaryId, PRODUCER).getVerification().getKeyThumbprint());
	}

	// -- criterion 2 ----------------------------------------------------------------------------

	/**
	 * §15.2: "An invalid signature, unknown key, or mismatched producer identity never appears in
	 * normal retrieval."
	 */
	@Test
	void criterion02_rejectedRecord_neverRetrievableByAnyLookup() {
		registerPrimary();

		String invalidSignatureRecordId = TraceRecordBuilder.freshRecordId();
		KeyPair wrongKeyPair = TraceTestKeys.generateKeyPair();
		String invalidSignatureJson = TraceRecordBuilder.authorizationDecision().recordId(invalidSignatureRecordId)
				.genesis().sign(wrongKeyPair.getPrivate());
		assertThrows(TraceValidationException.class, () -> harness.ingest(invalidSignatureJson, ctx()));
		assertNeverRetrievable(invalidSignatureRecordId);

		String unknownKeyRecordId = TraceRecordBuilder.freshRecordId();
		String unknownKeyJson = TraceRecordBuilder.authorizationDecision().recordId(unknownKeyRecordId).genesis()
				.kid("no-such-kid").sign(wrongKeyPair.getPrivate());
		assertThrows(TraceValidationException.class, () -> harness.ingest(unknownKeyJson, ctx()));
		assertNeverRetrievable(unknownKeyRecordId);

		String mismatchRecordId = TraceRecordBuilder.freshRecordId();
		String mismatchJson = TraceRecordBuilder.authorizationDecision().recordId(mismatchRecordId).genesis()
				.withField("other-producer", "producer").sign(wrongKeyPair.getPrivate());
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(mismatchJson, ctx()));
		assertEquals(TraceErrorResponseType.PRODUCER_MISMATCH, ex.getErrorId());
		assertNeverRetrievable(mismatchRecordId);
	}

	private void assertNeverRetrievable(String recordId) {
		assertThrows(TraceValidationException.class, () -> harness.getRecord(DOMAIN, recordId, PRODUCER));
		ExecutionIdentity exec = new ExecutionIdentity(DOMAIN, EXECUTION_AUTHORITY, TRACE_EXECUTION_ID);
		assertTrue(harness.store().findRecordsByExecution(exec, 0, 100).isEmpty());
		TokenRef token = new TokenRef(TOKEN_ISSUER, "access_token", TOKEN_JTI, null);
		assertTrue(harness.store().findRecordsByToken(DOMAIN, token).isEmpty());
	}

	// -- criterion 3 ----------------------------------------------------------------------------

	/**
	 * §15.3: "Replaying identical content returns the original receipt without creating another
	 * entry."
	 */
	@Test
	void criterion03_identicalReplay_returnsOriginalReceiptSingleRow() {
		KeyPair keyPair = registerPrimary();
		String json = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPair.getPrivate());

		AcceptanceResult first = harness.ingest(json, ctx());
		AcceptanceResult second = harness.ingest(json, ctx());

		assertFalse(first.isIdempotentReplay());
		assertTrue(second.isIdempotentReplay());
		assertEquals(first.getReceiptSequence(), second.getReceiptSequence());
		assertEquals(first.getContentDigest(), second.getContentDigest());
		assertEquals(1, harness.store().snapshot().size());
	}

	// -- criterion 4 ----------------------------------------------------------------------------

	/**
	 * §15.4: "Reusing a record identity with changed content cannot overwrite the accepted
	 * record."
	 */
	@Test
	void criterion04_sameIdentityChangedContent_recordConflictFirstRowUnchanged() {
		KeyPair keyPair = registerPrimary();
		String recordId = TraceRecordBuilder.freshRecordId();
		String first = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());
		String resigned = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.withField("1.2.4", "trace", "policy", "policy_store_version").sign(keyPair.getPrivate());

		AcceptanceResult accepted = harness.ingest(first, ctx());
		TraceConflictException ex = assertThrows(TraceConflictException.class, () -> harness.ingest(resigned, ctx()));
		assertEquals(TraceErrorResponseType.RECORD_CONFLICT, ex.getErrorId());

		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(accepted.getContentDigest(), stored.getContentDigest());
		assertEquals(first, stored.getAssertionRaw());
		assertEquals(1, harness.store().snapshot().size());
		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

	// -- criterion 5 ----------------------------------------------------------------------------

	/**
	 * §15.5: "Two producers may use the same {@code record_id} without collision because
	 * {@code producer_id} scopes identity."
	 */
	@Test
	void criterion05_twoProducersSameRecordId_noCollision() {
		KeyPair keyPairA = registerPrimary();
		KeyPair keyPairB = registerGateway();
		String sharedRecordId = TraceRecordBuilder.freshRecordId();

		String jsonA = TraceRecordBuilder.authorizationDecision().recordId(sharedRecordId).genesis()
				.sign(keyPairA.getPrivate());
		String jsonB = TraceRecordBuilder.capabilityInvoked().recordId(sharedRecordId).genesis()
				.sign(keyPairB.getPrivate());

		AcceptanceResult resultA = harness.ingest(jsonA, ctx());
		AcceptanceResult resultB = harness.ingest(jsonB, ctx());

		assertNotEquals(resultA.getReceiptSequence(), resultB.getReceiptSequence());
		assertEquals(2, harness.store().snapshot().size());
		assertEquals(sharedRecordId, harness.getRecord(DOMAIN, sharedRecordId, PRODUCER).getIdentity().getRecordId());
		assertEquals(sharedRecordId,
				harness.getRecord(DOMAIN, sharedRecordId, GW_PRODUCER).getIdentity().getRecordId());
	}

	// -- criterion 6 ----------------------------------------------------------------------------

	/**
	 * §15.6: "Two issuers may use the same {@code jti} without collision because {@code issuer}
	 * scopes token identity."
	 */
	@Test
	void criterion06_twoIssuersSameJti_twoTokenKeysBothIndexed() {
		KeyPair keyPair = registerPrimary();
		ObjectNode tree = TraceRecordBuilder.authorizationDecision().genesis().tree();
		String recordId = tree.get("record_id").asText();
		String otherIssuer = "https://accounts.other.example.org";
		addToken(tree, otherIssuer, TOKEN_JTI);
		String json = TraceRecordBuilder.fromJson(tree.toString()).sign(keyPair.getPrivate());

		harness.ingest(json, ctx());

		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(2, stored.getTokenRefs().size());
		TokenRef first = new TokenRef(TOKEN_ISSUER, "access_token", TOKEN_JTI, null);
		TokenRef second = new TokenRef(otherIssuer, "access_token", TOKEN_JTI, null);
		assertEquals(1, harness.store().findRecordsByToken(DOMAIN, first).size());
		assertEquals(1, harness.store().findRecordsByToken(DOMAIN, second).size());
	}

	// -- criterion 7 ----------------------------------------------------------------------------

	/**
	 * §15.7: "A record may reference multiple tokens and all references are indexed."
	 */
	@Test
	void criterion07_recordWithThreeTokens_allThreeIndexed() {
		KeyPair keyPair = registerPrimary();
		ObjectNode tree = TraceRecordBuilder.authorizationDecision().genesis().tree();
		String recordId = tree.get("record_id").asText();
		String issuer2 = "https://accounts.second.example.org";
		String jti2 = "aa9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3f";
		String issuer3 = "https://accounts.third.example.org";
		String jti3 = "bb9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f40";
		addToken(tree, issuer2, jti2);
		addToken(tree, issuer3, jti3);
		String json = TraceRecordBuilder.fromJson(tree.toString()).sign(keyPair.getPrivate());

		harness.ingest(json, ctx());

		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(3, stored.getTokenRefs().size());
		assertEquals(1,
				harness.store().findRecordsByToken(DOMAIN, new TokenRef(TOKEN_ISSUER, "access_token", TOKEN_JTI, null))
						.size());
		assertEquals(1,
				harness.store().findRecordsByToken(DOMAIN, new TokenRef(issuer2, "access_token", jti2, null)).size());
		assertEquals(1,
				harness.store().findRecordsByToken(DOMAIN, new TokenRef(issuer3, "access_token", jti3, null)).size());
	}

	// -- criterion 8 ----------------------------------------------------------------------------

	/**
	 * §15.8: "A producer-chain gap is visible, and out-of-order arrival can resolve the gap
	 * without rewriting assertions."
	 */
	@Test
	void criterion08_gapThenOutOfOrderFill_gapVisibleThenCleared() {
		KeyPair keyPair = registerPrimary();
		String recordId1 = TraceRecordBuilder.freshRecordId();
		String json1 = TraceRecordBuilder.authorizationDecision().recordId(recordId1).genesis()
				.sign(keyPair.getPrivate());
		String prevHashForRecord2 = contentDigestOf(json1);

		String recordId2 = TraceRecordBuilder.freshRecordId();
		String json2 = TraceRecordBuilder.authorizationDecision().recordId(recordId2).sequenceNumber(2)
				.prevRecordHash(prevHashForRecord2).sign(keyPair.getPrivate());

		harness.ingest(json2, ctx());
		StoredTraceRecord afterGap = harness.getRecord(DOMAIN, recordId2, PRODUCER);
		assertTrue(afterGap.getFlags().isCoverageGap());
		assertFalse(afterGap.getFlags().isChainLinkFailure());

		harness.ingest(json1, ctx());
		StoredTraceRecord afterFill = harness.getRecord(DOMAIN, recordId2, PRODUCER);
		assertFalse(afterFill.getFlags().isCoverageGap());
		assertFalse(afterFill.getFlags().isChainLinkFailure());
		assertEquals(json2, afterFill.getAssertionRaw());
		assertEquals(afterGap.getContentDigest(), afterFill.getContentDigest());
	}

	// -- criterion 9 ----------------------------------------------------------------------------

	/**
	 * §15.9: "Equivocation at one producer-chain position is visible and no conflicting record
	 * is silently preferred."
	 */
	@Test
	void criterion09_equivocationAtOnePosition_bothStoredAndFlagged() {
		KeyPair keyPair = registerPrimary();
		String recordId1 = TraceRecordBuilder.freshRecordId();
		String json1 = TraceRecordBuilder.authorizationDecision().recordId(recordId1).genesis()
				.sign(keyPair.getPrivate());
		String recordId2 = TraceRecordBuilder.freshRecordId();
		String json2 = TraceRecordBuilder.authorizationDecision().recordId(recordId2).genesis()
				.sign(keyPair.getPrivate());

		harness.ingest(json1, ctx());
		harness.ingest(json2, ctx());

		StoredTraceRecord stored1 = harness.getRecord(DOMAIN, recordId1, PRODUCER);
		StoredTraceRecord stored2 = harness.getRecord(DOMAIN, recordId2, PRODUCER);
		assertTrue(stored1.getFlags().isEquivocation());
		assertTrue(stored2.getFlags().isEquivocation());
	}

	// -- criterion 10 ---------------------------------------------------------------------------

	/**
	 * §15.10: "Execution retrieval never crosses an evidence domain and never silently guesses
	 * among ambiguous authorities."
	 */
	@Test
	void criterion10_executionRetrieval_neverCrossesDomainsAmbiguousWithTwoAuthorities() {
		KeyPair keyPairDomainA = registerPrimary();
		harness.registerKey(OTHER_DOMAIN, PRODUCER, KID);
		harness.registerChain(OTHER_DOMAIN, PRODUCER, INSTANCE, CHAIN);
		KeyPair keyPairDomainB = harness.registerKey(OTHER_DOMAIN, PRODUCER, KID + "-b", 0L, null);

		String jsonA = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPairDomainA.getPrivate());
		harness.ingest(jsonA, ctx());

		String jsonB = TraceRecordBuilder.authorizationDecision().genesis().kid(KID + "-b")
				.sign(keyPairDomainB.getPrivate());
		harness.ingest(jsonB, harness.ctx(OTHER_DOMAIN));

		TraceRetrievalService.ExecutionResult isolated = harness.getExecution(DOMAIN, TRACE_EXECUTION_ID, null, 0,
				100);
		assertEquals(1, isolated.getRecords().size());
		assertEquals(EXECUTION_AUTHORITY, isolated.getExecutionAuthority());

		String secondAuthority = "spiffe://example.org/agent/other-planner";
		KeyPair keyPairGw = registerGateway();
		String secondAuthorityJson = TraceRecordBuilder.capabilityInvoked().genesis()
				.executionAuthority(secondAuthority).sign(keyPairGw.getPrivate());
		harness.ingest(secondAuthorityJson, ctx());

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> harness.getExecution(DOMAIN, TRACE_EXECUTION_ID, null, 0, 100));
		assertEquals(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, ex.getErrorId());
	}

	// -- criterion 11 ---------------------------------------------------------------------------

	/**
	 * §15.11: "The authorization decision and enforcement-point invocation can be retrieved
	 * under the same execution."
	 */
	@Test
	void criterion11_decisionAndInvocationSameExecution_bothReturnedInReceiptOrder() {
		KeyPair decisionKeyPair = registerPrimary();
		KeyPair invocationKeyPair = registerGateway();
		String decisionRecordId = TraceRecordBuilder.freshRecordId();
		String decisionJson = TraceRecordBuilder.authorizationDecision().recordId(decisionRecordId).genesis()
				.sign(decisionKeyPair.getPrivate());
		String invocationRecordId = TraceRecordBuilder.freshRecordId();
		String invocationJson = TraceRecordBuilder.capabilityInvoked().recordId(invocationRecordId).genesis()
				.sign(invocationKeyPair.getPrivate());

		harness.ingest(decisionJson, ctx());
		harness.ingest(invocationJson, ctx());

		TraceRetrievalService.ExecutionResult result = harness.getExecution(DOMAIN, TRACE_EXECUTION_ID,
				EXECUTION_AUTHORITY, 0, 100);
		assertEquals(2, result.getRecords().size());
		assertEquals(decisionRecordId, result.getRecords().get(0).getIdentity().getRecordId());
		assertEquals(invocationRecordId, result.getRecords().get(1).getIdentity().getRecordId());
	}

	// -- criterion 12 ---------------------------------------------------------------------------

	/**
	 * §15.12: "A failed atomic write leaves neither a visible record, receipt entry, nor index
	 * entry."
	 */
	@Test
	void criterion12_failedAtomicWrite_noRecordNoCommittedReceiptNoIndexHit() {
		KeyPair keyPair = registerPrimary();
		String json = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPair.getPrivate());
		harness.store().failNextInsertRecord();

		assertThrows(TraceStorageException.class, () -> harness.ingest(json, ctx()));

		assertTrue(harness.store().snapshot().isEmpty());
		assertNotEquals(TraceReceiptState.COMMITTED,
				harness.store().findReceipt(DOMAIN, 1L).orElseThrow().getReceiptStateEnum());
		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

	// -- criterion 13 ---------------------------------------------------------------------------

	/**
	 * §15.13: "A record lookup never returns a matching identifier from another evidence
	 * domain."
	 */
	@Test
	void criterion13_recordLookup_matchingIdentifierInAnotherDomain_notFound() {
		KeyPair keyPair = harness.registerKey(OTHER_DOMAIN, PRODUCER, KID);
		harness.registerChain(OTHER_DOMAIN, PRODUCER, INSTANCE, CHAIN);
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());

		harness.ingest(json, harness.ctx(OTHER_DOMAIN));

		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.getRecord(DOMAIN, recordId, PRODUCER));
		assertEquals(TraceErrorResponseType.RECORD_NOT_FOUND, ex.getErrorId());
	}

	// -- criterion 14 ---------------------------------------------------------------------------

	/**
	 * §15.14: "An unscoped record or execution lookup with several in-domain matches returns an
	 * ambiguity error rather than selecting one."
	 */
	@Test
	void criterion14_unscopedLookup_severalInDomainMatches_ambiguityError() {
		KeyPair keyPairA = registerPrimary();
		KeyPair keyPairB = registerGateway();
		String sharedRecordId = TraceRecordBuilder.freshRecordId();
		harness.ingest(
				TraceRecordBuilder.authorizationDecision().recordId(sharedRecordId).genesis().sign(keyPairA.getPrivate()),
				ctx());
		harness.ingest(
				TraceRecordBuilder.capabilityInvoked().recordId(sharedRecordId).genesis().sign(keyPairB.getPrivate()),
				ctx());

		TraceConflictException recordEx = assertThrows(TraceConflictException.class,
				() -> harness.getRecord(DOMAIN, sharedRecordId, null));
		assertEquals(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, recordEx.getErrorId());

		String effectProducer = "effect-observer-1";
		KeyPair keyPairEffect = harness.registerKey(DOMAIN, effectProducer, "effect-observer-1-2026-01");
		harness.registerChain(DOMAIN, effectProducer, "effect-observer-001", "chain-03JABC9Z0K");
		String otherAuthority = "spiffe://example.org/agent/other-planner";
		harness.ingest(TraceRecordBuilder.runtimeEffect().genesis().executionAuthority(otherAuthority)
				.traceExecutionId(TRACE_EXECUTION_ID).sign(keyPairEffect.getPrivate()), ctx());

		TraceConflictException executionEx = assertThrows(TraceConflictException.class,
				() -> harness.getExecution(DOMAIN, TRACE_EXECUTION_ID, null, 0, 100));
		assertEquals(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, executionEx.getErrorId());
	}

	// -- criterion 17 (bulk) -----------------------------------------------------------------------

	private TraceRestWebServiceImpl bulkEndpoint() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(harness.traceConfiguration());

		SubmitterIdentity identity = new SubmitterIdentity(TraceTestHarness.DEFAULT_CLIENT_ID, true);
		SubmitterIdentityService identityService = mock(SubmitterIdentityService.class);
		when(identityService.resolve(any())).thenReturn(identity);
		EvidenceDomainResolver domainResolver = mock(EvidenceDomainResolver.class);
		when(domainResolver.resolve(identity)).thenReturn(ctx());

		ErrorResponseFactory errorFactory = mock(ErrorResponseFactory.class);
		when(errorFactory.buildErrorResponse(any(IErrorType.class), anyString())).thenAnswer(inv -> {
			IErrorType type = inv.getArgument(0);
			DefaultErrorResponse body = new DefaultErrorResponse();
			body.setType(type);
			body.setErrorDescription(type.getParameter());
			body.setReason(inv.getArgument(1));
			return body;
		});
		when(errorFactory.traceException(any(TraceErrorResponseType.class), anyString())).thenAnswer(inv -> {
			TraceErrorResponseType type = inv.getArgument(0);
			ObjectNode body = MAPPER.createObjectNode();
			body.put("error", type.getParameter());
			body.put("reason", (String) inv.getArgument(1));
			return new WebApplicationException(Response.status(type.httpStatus()).entity(body).build());
		});
		when(errorFactory.traceException(any(TraceErrorResponseType.class), anyString(), any())).thenAnswer(inv -> {
			TraceErrorResponseType type = inv.getArgument(0);
			ObjectNode body = MAPPER.createObjectNode();
			body.put("error", type.getParameter());
			body.put("reason", (String) inv.getArgument(1));
			return new WebApplicationException(Response.status(type.httpStatus()).entity(body).build());
		});

		TraceRestWebServiceImpl impl = new TraceRestWebServiceImpl();
		setField(impl, "log", org.slf4j.LoggerFactory.getLogger(TraceRestWebServiceImpl.class));
		setField(impl, "appConfiguration", appConfiguration);
		setField(impl, "errorResponseFactory", errorFactory);
		setField(impl, "submitterIdentityService", identityService);
		setField(impl, "evidenceDomainResolver", domainResolver);
		setField(impl, "ingestionService", harness.ingestionService());
		setField(impl, "applicationAuditLogger", mock(ApplicationAuditLogger.class));
		return impl;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static JsonNode submitBulk(TraceRestWebServiceImpl endpoint, String arrayJson, int expectedStatus) {
		Response response = endpoint.submitBulkRecords(
				new ByteArrayInputStream(arrayJson.getBytes(StandardCharsets.UTF_8)), null, null);
		assertEquals(expectedStatus, response.getStatus());
		return MAPPER.valueToTree(response.getEntity());
	}

	/**
	 * §15.17: "A POST /api/v1/audit/trace/bulk batch with a mix of valid and invalid records accepts
	 * every valid record and rejects every invalid one on its own merits, each with its own
	 * per-record result and receipt; one record's failure never rejects an independently-valid record
	 * in the same batch ..."
	 */
	@Test
	void criterion17_bulkValidAndInvalidFromOneProducer_eachOnItsOwnMerits() {
		KeyPair keyPair = registerPrimary();
		KeyPair wrongKeyPair = TraceTestKeys.generateKeyPair();

		String id1 = TraceRecordBuilder.freshRecordId();
		String json1 = TraceRecordBuilder.authorizationDecision().recordId(id1).genesis().sign(keyPair.getPrivate());
		String badId = TraceRecordBuilder.freshRecordId();
		String badJson = TraceRecordBuilder.authorizationDecision().recordId(badId).sequenceNumber(2)
				.prevRecordHash(contentDigestOf(json1)).sign(wrongKeyPair.getPrivate());
		String id3 = TraceRecordBuilder.freshRecordId();
		String json3 = TraceRecordBuilder.authorizationDecision().recordId(id3).sequenceNumber(2)
				.prevRecordHash(contentDigestOf(json1)).sign(keyPair.getPrivate());
		String batch = "[" + json1 + "," + badJson + "," + json3 + "]";

		TraceRestWebServiceImpl endpoint = bulkEndpoint();
		JsonNode results = submitBulk(endpoint, batch, 207).get("results");

		assertEquals(3, results.size());
		JsonNode first = results.get(0);
		assertTrue(first.get("accepted").booleanValue());
		assertEquals(0, first.get("index").intValue());
		assertEquals(PRODUCER, first.get("producer_id").textValue());
		assertEquals(id1, first.get("record_id").textValue());
		assertEquals(1L, first.get("receipt_sequence").longValue());
		// DEFAULT_NOW_MS has a zero millisecond part: the fraction must still carry three digits
		assertEquals("2023-11-14T22:13:21.000Z", first.get("received_at").textValue());
		assertFalse(first.get("idempotent_replay").booleanValue());

		JsonNode rejected = results.get(1);
		assertFalse(rejected.get("accepted").booleanValue());
		assertEquals(1, rejected.get("index").intValue());
		assertEquals(PRODUCER, rejected.get("producer_id").textValue());
		assertEquals(badId, rejected.get("record_id").textValue());
		assertEquals("invalid_signature", rejected.get("error").get("error").textValue());
		assertFalse(rejected.has("receipt_sequence"));

		JsonNode third = results.get(2);
		assertTrue(third.get("accepted").booleanValue());
		assertEquals(id3, third.get("record_id").textValue());
		assertEquals(2L, third.get("receipt_sequence").longValue());

		assertEquals(2, harness.store().snapshot().size());
		assertThrows(TraceValidationException.class, () -> harness.getRecord(DOMAIN, badId, PRODUCER));
		assertEquals(json1, harness.getRecord(DOMAIN, id1, PRODUCER).getAssertionRaw());
		assertEquals(json3, harness.getRecord(DOMAIN, id3, PRODUCER).getAssertionRaw());

		JsonNode replay = submitBulk(endpoint, batch, 207).get("results");
		assertTrue(replay.get(0).get("idempotent_replay").booleanValue());
		assertEquals(1L, replay.get(0).get("receipt_sequence").longValue());
		assertFalse(replay.get(1).get("accepted").booleanValue());
		assertTrue(replay.get(2).get("idempotent_replay").booleanValue());
		assertEquals(2L, replay.get(2).get("receipt_sequence").longValue());
		assertEquals(2, harness.store().snapshot().size());
	}

	/**
	 * §15.17: "... and a batch mixing producers is rejected at the envelope level."
	 */
	@Test
	void criterion17_bulkMixingProducers_rejectedAtEnvelopeNothingStored() {
		KeyPair primaryKey = registerPrimary();
		KeyPair gatewayKey = registerGateway();
		String primaryId = TraceRecordBuilder.freshRecordId();
		String gatewayId = TraceRecordBuilder.freshRecordId();
		String primaryJson = TraceRecordBuilder.authorizationDecision().recordId(primaryId).genesis()
				.sign(primaryKey.getPrivate());
		String gatewayJson = TraceRecordBuilder.capabilityInvoked().recordId(gatewayId).genesis()
				.sign(gatewayKey.getPrivate());

		TraceRestWebServiceImpl endpoint = bulkEndpoint();
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> endpoint
				.submitBulkRecords(new ByteArrayInputStream(
						("[" + primaryJson + "," + gatewayJson + "]").getBytes(StandardCharsets.UTF_8)), null, null));

		assertEquals(400, ex.getResponse().getStatus());
		JsonNode body = MAPPER.valueToTree(ex.getResponse().getEntity());
		assertEquals("invalid_request", body.get("error").textValue());
		assertEquals("bulk_mixed_producers", body.get("reason").textValue());

		assertTrue(harness.store().snapshot().isEmpty());
		assertNeverRetrievable(primaryId);
		assertThrows(TraceValidationException.class, () -> harness.getRecord(DOMAIN, gatewayId, GW_PRODUCER));

		AcceptanceResult next = harness.ingest(TraceRecordBuilder.authorizationDecision().genesis()
				.sign(primaryKey.getPrivate()), ctx());
		assertEquals(1L, next.getReceiptSequence());
	}

	// -- task 30: chain registration modes and multi-chain instances -----------------------------

	private static final String SECOND_CHAIN = "chain-03JABC9Z0K";

	@Test
	void chainRegistrationMode_firstObserved_genesisOpensChainAndSecondChainStaysUnregistered() {
		harness.traceConfiguration().setChainRegistrationMode("FIRST_OBSERVED");
		KeyPair keyPair = harness.registerKey(DOMAIN, PRODUCER, KID);
		String genesisJson = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPair.getPrivate());

		AcceptanceResult accepted = harness.ingest(genesisJson, ctx());

		assertTrue(accepted.isAccepted());
		List<io.jans.lock.model.trace.entity.TraceChainEntry> chains = harness.store().findChains(DOMAIN, PRODUCER);
		assertEquals(1, chains.size());
		assertEquals(CHAIN, chains.get(0).getProducerChainId());
		assertEquals("first_observed_genesis", chains.get(0).getGenesisKind());
		assertEquals(TraceTestHarness.DEFAULT_CLIENT_ID, chains.get(0).getRegisteredBy());

		String otherChainSeq2 = TraceRecordBuilder.authorizationDecision().producerChainId(SECOND_CHAIN)
				.sequenceNumber(2).prevRecordHash(contentDigestOf(genesisJson)).sign(keyPair.getPrivate());
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(otherChainSeq2, ctx()));
		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
		assertEquals(1, harness.store().findChains(DOMAIN, PRODUCER).size());
	}

	@Test
	void chainRegistrationMode_firstObserved_replayedGenesisIsIdempotentAndModeSwitchKeepsRows() {
		harness.traceConfiguration().setChainRegistrationMode("FIRST_OBSERVED");
		KeyPair keyPair = harness.registerKey(DOMAIN, PRODUCER, KID);
		String genesisJson = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPair.getPrivate());
		harness.ingest(genesisJson, ctx());

		harness.traceConfiguration().setChainRegistrationMode("PRE_REGISTERED");

		assertTrue(harness.ingest(genesisJson, ctx()).isIdempotentReplay());
		String next = TraceRecordBuilder.authorizationDecision().sequenceNumber(2)
				.prevRecordHash(contentDigestOf(genesisJson)).sign(keyPair.getPrivate());
		assertTrue(harness.ingest(next, ctx()).isAccepted(), "the existing chain row stays registered");
		assertEquals("first_observed_genesis", harness.store().findChains(DOMAIN, PRODUCER).get(0).getGenesisKind());
		String otherChainGenesis = TraceRecordBuilder.authorizationDecision().producerChainId(SECOND_CHAIN).genesis()
				.sign(keyPair.getPrivate());
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(otherChainGenesis, ctx()));
		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
	}

	@Test
	void chainRegistrationMode_defaultPreRegistered_adminChainsListPreRegisteredGenesis() {
		registerPrimary();

		List<io.jans.lock.model.trace.entity.TraceChainEntry> chains = harness.store().findChains(DOMAIN, PRODUCER);

		assertEquals(1, chains.size());
		assertEquals("pre_registered_genesis", chains.get(0).getGenesisKind());
		assertEquals("PRE_REGISTERED", harness.traceConfiguration().getChainRegistrationMode());
	}

	@Test
	void chainLink_arbitraryObject_acceptedAndReturnedUnchanged() throws Exception {
		KeyPair keyPair = registerPrimary();
		JsonNode chainLink = MAPPER.readTree(
				"{\"opaque\":[1,{\"x\":null}],\"kind\":\"anything\",\"nested\":{\"a\":true}}");
		String recordId = TraceRecordBuilder.freshRecordId();
		String json = TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis()
				.withField(chainLink, "producer_chain", "chain_link").sign(keyPair.getPrivate());

		AcceptanceResult result = harness.ingest(json, ctx());

		assertTrue(result.isAccepted());
		StoredTraceRecord stored = harness.getRecord(DOMAIN, recordId, PRODUCER);
		assertEquals(json, stored.getAssertionRaw());
		assertEquals(chainLink, MAPPER.readTree(stored.getAssertionRaw()).at("/producer_chain/chain_link"));
	}

	@Test
	void multiChain_singleAndBulkSubmissionsInterleavedAcrossTwoChains_independentAndOrdered() {
		KeyPair keyPair = registerPrimary();
		harness.registerChain(DOMAIN, PRODUCER, INSTANCE, SECOND_CHAIN);

		String a1 = TraceRecordBuilder.authorizationDecision().genesis().sign(keyPair.getPrivate());
		String b1 = TraceRecordBuilder.authorizationDecision().producerChainId(SECOND_CHAIN).genesis()
				.sign(keyPair.getPrivate());
		String a2 = TraceRecordBuilder.authorizationDecision().sequenceNumber(2).prevRecordHash(contentDigestOf(a1))
				.sign(keyPair.getPrivate());
		// chain B position 3 skips position 2 inside the batch
		String b2 = TraceRecordBuilder.authorizationDecision().producerChainId(SECOND_CHAIN).sequenceNumber(2)
				.prevRecordHash(contentDigestOf(b1)).sign(keyPair.getPrivate());
		String b3 = TraceRecordBuilder.authorizationDecision().producerChainId(SECOND_CHAIN).sequenceNumber(3)
				.prevRecordHash(contentDigestOf(b2)).sign(keyPair.getPrivate());
		String a3 = TraceRecordBuilder.authorizationDecision().sequenceNumber(3).prevRecordHash(contentDigestOf(a2))
				.sign(keyPair.getPrivate());

		AcceptanceResult singleA1 = harness.ingest(a1, ctx());
		AcceptanceResult singleB1 = harness.ingest(b1, ctx());
		JsonNode results = submitBulk(bulkEndpoint(), "[" + a2 + "," + b3 + "," + a3 + "]", 207).get("results");

		assertEquals(1L, singleA1.getReceiptSequence());
		assertEquals(2L, singleB1.getReceiptSequence());
		assertFalse(singleA1.isCoverageGapFlag());
		assertFalse(singleB1.isCoverageGapFlag());
		assertTrue(results.get(0).get("accepted").booleanValue());
		assertFalse(results.get(0).get("coverage_gap_flag").booleanValue());
		assertTrue(results.get(1).get("accepted").booleanValue());
		assertTrue(results.get(1).get("coverage_gap_flag").booleanValue(), "chain B position 2 is missing");
		assertTrue(results.get(2).get("accepted").booleanValue());
		assertFalse(results.get(2).get("coverage_gap_flag").booleanValue(), "chain A is unaffected");
		assertEquals(3L, results.get(0).get("receipt_sequence").longValue());
		assertEquals(4L, results.get(1).get("receipt_sequence").longValue());
		assertEquals(5L, results.get(2).get("receipt_sequence").longValue());

		AcceptanceResult lateB2 = harness.ingest(b2, ctx());
		assertFalse(lateB2.isCoverageGapFlag());
		assertEquals(6L, lateB2.getReceiptSequence());
		assertFalse(harness.getRecord(DOMAIN, results.get(1).get("record_id").textValue(), PRODUCER).getFlags()
				.isCoverageGap());
		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

}
