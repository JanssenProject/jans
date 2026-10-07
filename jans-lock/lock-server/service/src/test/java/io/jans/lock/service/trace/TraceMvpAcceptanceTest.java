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

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.canon.CanonicalHashes;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.service.trace.testkit.TraceTestKeys;

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

	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final String GW_PRODUCER = "gateway-fleet-1/2.0.0";

	private static final String GW_KID = "gateway-fleet-1-2026-01";

	private static final String GW_INSTANCE = "gateway-001";

	private static final String GW_CHAIN = "chain-02JABC9Z0K";

	private static final String EXECUTION_AUTHORITY = "spiffe://example.org/agent/planner";

	private static final String TRACE_EXECUTION_ID = "exec-01JABCXYZQK8P5N9F2C7R3T4V6";

	private static final String CAPABILITY_ID = "invoke:payment-authorization";

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
		assertEquals(1, harness.store().findRecordsByCapability(DOMAIN, CAPABILITY_ID).size());
		assertEquals(1, harness.store().snapshot().size());
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
				.withField("other-producer/9.9.9", "producer").sign(wrongKeyPair.getPrivate());
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> harness.ingest(mismatchJson, ctx()));
		assertEquals(TraceErrorResponseType.PRODUCER_MISMATCH, ex.getErrorId());
		assertNeverRetrievable(mismatchRecordId);
	}

	private void assertNeverRetrievable(String recordId) {
		assertThrows(TraceValidationException.class, () -> harness.getRecord(DOMAIN, recordId, PRODUCER));
		ExecutionIdentity exec = new ExecutionIdentity(DOMAIN, EXECUTION_AUTHORITY, TRACE_EXECUTION_ID);
		assertTrue(harness.store().findRecordsByExecution(exec, 0, 100).isEmpty());
		assertTrue(harness.store().findRecordsByCapability(DOMAIN, CAPABILITY_ID).isEmpty());
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
		assertTrue(harness.store().findRecordsByCapability(DOMAIN, CAPABILITY_ID).isEmpty());
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

		String effectProducer = "effect-observer-1/1.0.0";
		KeyPair keyPairEffect = harness.registerKey(DOMAIN, effectProducer, "effect-observer-1-2026-01");
		harness.registerChain(DOMAIN, effectProducer, "effect-observer-001", "chain-03JABC9Z0K");
		String otherAuthority = "spiffe://example.org/agent/other-planner";
		harness.ingest(TraceRecordBuilder.runtimeEffect().genesis().executionAuthority(otherAuthority)
				.traceExecutionId(TRACE_EXECUTION_ID).sign(keyPairEffect.getPrivate()), ctx());

		TraceConflictException executionEx = assertThrows(TraceConflictException.class,
				() -> harness.getExecution(DOMAIN, TRACE_EXECUTION_ID, null, 0, 100));
		assertEquals(TraceErrorResponseType.AMBIGUOUS_IDENTIFIER, executionEx.getErrorId());
	}

}
