/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.SignedAssertionFactory;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.lock.service.trace.validate.CorrelationInputs;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;
import io.jans.lock.service.trace.verify.VerifiedAssertion;

/**
 * Tests for {@link TraceCorrelationService}: producer-chain pre-registration/genesis checks,
 * predecessor/peer/successor flag computation and the post-insert neighbor updates (design §8, §9).
 *
 * <p>{@link VerifiedAssertion}s are built with the real {@link TraceAssertionParser},
 * {@link CommonAssertionValidator} and {@link EventKindValidatorRegistry} over
 * {@link SignedAssertionFactory} fixture text — no signature verification is involved, since
 * {@code CommonAssertionValidator} checks only the signature's format, not its cryptographic
 * validity, and {@link TraceCorrelationService} never touches the signature at all.
 */
class TraceCorrelationServiceTest extends BaseLockServiceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String DOMAIN = "domain-1";

	private static final String CLIENT_ID = "2200.abcd";

	private static final String NODE_ID = "node-1";

	private static final String REGISTERED_BY = "2200.admin";

	/** The {@code capabilityInvoked} fixture's producer-chain identity. */
	private static final String PRODUCER = "gateway-fleet-1";

	private static final String INSTANCE = "gateway-001";

	private static final String CHAIN = "chain-02JABC9Z0K";

	private static final long RECEIVED_AT_MS = 1_800_000_000_000L;

	private InMemoryTraceStore store;

	private ProducerChainRegistry chainRegistry;

	private TraceAssertionParser parser;

	private CommonAssertionValidator commonValidator;

	private EventKindValidatorRegistry eventKindRegistry;

	private TraceConfiguration traceConfiguration;

	private TraceCorrelationService service;

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		traceConfiguration = new TraceConfiguration();
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);
		setField(parser, "cryptoProvider", TraceTestCrypto.javaProvider());

		commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);

		eventKindRegistry = new EventKindValidatorRegistry();

		store = new InMemoryTraceStore();

		chainRegistry = new ProducerChainRegistry();
		setField(chainRegistry, "traceStore", store);

		service = new TraceCorrelationService();
		setField(service, "log", LoggerFactory.getLogger(TraceCorrelationService.class));
		setField(service, "store", store);
		setField(service, "chainRegistry", chainRegistry);
		setField(service, "appConfiguration", appConfiguration);
		setField(service, "traceKeys", TraceTestCrypto.traceKeys());
		setField(service, "policyMismatchCheck", PolicyMismatchChecks.none(appConfiguration));
	}

	// -- fixtures ---------------------------------------------------------------------------------

	private static ChainIdentity chainIdentity() {
		return new ChainIdentity(DOMAIN, PRODUCER, INSTANCE, CHAIN);
	}

	private void registerChain() {
		chainRegistry.register(chainIdentity(), REGISTERED_BY, RECEIVED_AT_MS);
	}

	private static TraceRequestContext context() {
		return context(RECEIVED_AT_MS);
	}

	private static TraceRequestContext context(long receivedAtMs) {
		return new TraceRequestContext(CLIENT_ID, DOMAIN, Collections.singletonList("*"), receivedAtMs, NODE_ID);
	}

	private VerifiedAssertion buildVerified(String json, TraceRequestContext ctx) {
		ParsedAssertion parsed = parser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		commonValidator.validate(parsed);
		CorrelationInputs inputs = eventKindRegistry.validate(parsed);
		String contentDigest = CanonicalHashes.sha256PrefixedOfJcs(parsed.getRoot());
		VerificationResult verification = new VerificationResult(true, parsed.getKid(), ctx.getReceivedAtMs(),
				"Ed25519");
		RecordIdentity identity = new RecordIdentity(ctx.getEvidenceDomainId(), parsed.getProducer(),
				parsed.getRecordId());
		return new VerifiedAssertion(parsed, inputs, contentDigest, verification, identity);
	}

	private static String recordId() {
		return UUID.randomUUID().toString();
	}

	private StoredTraceRecord insertRecord(String recordId, String contentDigest, long sequenceNumber,
			String prevRecordHash, IngestionFlags flags) throws DuplicateEntryException {
		StoredTraceRecord stored = StoredTraceRecordFactory.builder().domainId(DOMAIN).producerId(PRODUCER)
				.producerInstanceId(INSTANCE).producerChainId(CHAIN).recordId(recordId).contentDigest(contentDigest)
				.sequenceNumber(sequenceNumber).prevRecordHash(prevRecordHash).coverageGap(flags.isCoverageGap())
				.chainLinkFailure(flags.isChainLinkFailure()).equivocation(flags.isEquivocation())
				.late(flags.isLate()).build();
		store.insertRecord(stored);
		return stored;
	}

	private static String digest(String tag) {
		return "sha256:" + String.format("%064d", 0).substring(tag.length()) + tag;
	}

	private String addSecondToken(String json, String jti, String issuer) throws IOException {
		ObjectNode root = (ObjectNode) MAPPER.readTree(json);
		ArrayNode tokens = (ArrayNode) root.at("/trace/event/tokens");
		ObjectNode second = MAPPER.createObjectNode();
		second.put("issuer", issuer);
		second.put("token_type", "access_token");
		second.put("jti", jti);
		tokens.add(second);
		return MAPPER.writeValueAsString(root);
	}

	// -- plan: chain pre-registration and genesis ---------------------------

	@Test
	void testPlan_UnregisteredChain_ChainNotRegistered() {
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
	}

	@Test
	void testPlan_NonGenesisSequenceWithSentinel_InvalidGenesis() {
		registerChain();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(5)
				.prevRecordHash(TraceConstants.ZERO_HASH).unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
	}

	// -- plan: policy store release check ------------------------------------

	private static final String SERVED_STORE = "https://example.org/policy-stores/payments";

	private void servePolicyStore(String version, String bundleHash) {
		AppConfiguration checkConfiguration = mock(AppConfiguration.class);
		when(checkConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
		setField(service, "policyMismatchCheck", PolicyMismatchChecks.serving(checkConfiguration,
				new PolicyMismatchChecks.StaticPolicyDownloadService().archive(SERVED_STORE, version, bundleHash)));
	}

	@Test
	void testPlan_DecisionNamesServedStoreWithOtherBundleHash_FlagsPolicyMismatch() {
		registerChain();
		servePolicyStore("1.2.3", "sha256:" + "f".repeat(64));
		String json = SignedAssertionFactory.authorizationDecision().producer(PRODUCER).producerInstanceId(INSTANCE)
				.producerChainId(CHAIN).recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertTrue(plan.getFlags().isPolicyMismatch());
	}

	@Test
	void testPlan_DecisionNamesServedStoreWithSameRelease_NoPolicyMismatch() {
		registerChain();
		servePolicyStore("1.2.3", "sha256:84fd9bac333ad79154348296204fa7f8c537a96e08983e5f73b3f5aca8e8edf7");
		String json = SignedAssertionFactory.authorizationDecision().producer(PRODUCER).producerInstanceId(INSTANCE)
				.producerChainId(CHAIN).recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isPolicyMismatch());
	}

	@Test
	void testPlan_RejectPolicyMismatch_RejectsBeforeFirstObservedRegistration() {
		traceConfiguration.setChainRegistrationMode("FIRST_OBSERVED");
		traceConfiguration.setRejectPolicyMismatch(true);
		servePolicyStore("1.2.3", "sha256:" + "f".repeat(64));
		String json = SignedAssertionFactory.authorizationDecision().producer(PRODUCER).producerInstanceId(INSTANCE)
				.producerChainId(CHAIN).recordId(recordId()).genesis().unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.INVALID_ASSERTION, ex.getErrorId());
		assertEquals(PolicyMismatchCheck.REASON_POLICY_MISMATCH, ex.getReason());
		assertFalse(store.findChain(chainIdentity()).isPresent());
	}

	// -- plan: chain registration mode -------------------------------------

	@Test
	void testPlan_FirstObservedGenesisOnUnregisteredChain_RegistersChainAndPlans() {
		traceConfiguration.setChainRegistrationMode("FIRST_OBSERVED");
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isCoverageGap());
		TraceChainEntry chain = store.findChain(chainIdentity()).orElseThrow();
		assertEquals(TraceChainEntry.GENESIS_KIND_FIRST_OBSERVED, chain.getGenesisKind());
		assertEquals(CLIENT_ID, chain.getRegisteredBy());
		assertEquals(RECEIVED_AT_MS, chain.getRegisteredAtMs());
	}

	@Test
	void testPlan_FirstObservedModeIsCaseInsensitive() {
		traceConfiguration.setChainRegistrationMode(" first_observed ");
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();

		service.plan(buildVerified(json, context()), context());

		assertTrue(store.findChain(chainIdentity()).isPresent());
	}

	@Test
	void testPlan_FirstObservedNonGenesisOnUnregisteredChain_ChainNotRegistered() {
		traceConfiguration.setChainRegistrationMode("FIRST_OBSERVED");
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(2)
				.prevRecordHash(digest("1")).unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
		assertFalse(store.findChain(chainIdentity()).isPresent());
	}

	@Test
	void testPlan_FirstObservedGenesisWithoutSentinel_InvalidGenesisNothingRegistered() {
		traceConfiguration.setChainRegistrationMode("FIRST_OBSERVED");
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(1)
				.prevRecordHash(digest("1")).unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.INVALID_GENESIS, ex.getErrorId());
		assertFalse(store.findChain(chainIdentity()).isPresent());
	}

	@Test
	void testPlan_UnknownModeValue_TreatedAsPreRegisteredAndWarnedOnce() {
		Logger log = mock(Logger.class);
		setField(service, "log", log);
		traceConfiguration.setChainRegistrationMode("SOMETHING_ELSE");
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		for (int i = 0; i < 2; i++) {
			TraceRequestContext ctx = context();
			TraceValidationException ex = assertThrows(TraceValidationException.class,
					() -> service.plan(verified, ctx));
			assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
		}

		assertFalse(store.findChain(chainIdentity()).isPresent());
		verify(log, times(1)).warn(anyString(), eq("SOMETHING_ELSE"), any());
	}

	@Test
	void testPlan_DefaultModeUnregisteredGenesis_ChainNotRegisteredNothingRegistered() {
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());

		TraceRequestContext ctx = context();
		TraceValidationException ex = assertThrows(TraceValidationException.class,
				() -> service.plan(verified, ctx));

		assertEquals(TraceErrorResponseType.CHAIN_NOT_REGISTERED, ex.getErrorId());
		assertFalse(store.findChain(chainIdentity()).isPresent());
	}

	@Test
	void testPlan_ChainLinkMember_PassesValidationUnchanged() throws IOException {
		registerChain();
		ObjectNode root = (ObjectNode) MAPPER.readTree(
				SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson());
		ObjectNode chainLink = ((ObjectNode) root.get("producer_chain")).putObject("chain_link");
		chainLink.put("opaque", "anything");
		chainLink.putArray("list").add(1).add("two");
		String json = MAPPER.writeValueAsString(root);

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isCoverageGap());
		assertFalse(plan.getFlags().isChainLinkFailure());
	}

	// -- plan: predecessor / coverage gap / chain-link failure -------------------------------------

	@Test
	void testPlan_Genesis_NoCoverageGapNoChainLinkFailure() {
		registerChain();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isCoverageGap());
		assertFalse(plan.getFlags().isChainLinkFailure());
	}

	@Test
	void testPlan_NonGenesisNoPredecessor_CoverageGapTrueChainLinkFailureFalse() {
		registerChain();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(2)
				.prevRecordHash(digest("1")).unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertTrue(plan.getFlags().isCoverageGap());
		assertFalse(plan.getFlags().isChainLinkFailure());
	}

	@Test
	void testPlan_PredecessorPresentWrongDigest_ChainLinkFailureTrueCoverageGapFalse() throws DuplicateEntryException {
		registerChain();
		String predecessorDigest = digest("1");
		insertRecord(recordId(), predecessorDigest, 1L, TraceConstants.ZERO_HASH, new IngestionFlags(false, false,
				false, false));

		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(2)
				.prevRecordHash(digest("2")).unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isCoverageGap());
		assertTrue(plan.getFlags().isChainLinkFailure());
	}

	@Test
	void testPlan_PredecessorPresentMatchingDigest_NoFlags() throws DuplicateEntryException {
		registerChain();
		String predecessorDigest = digest("1");
		insertRecord(recordId(), predecessorDigest, 1L, TraceConstants.ZERO_HASH, new IngestionFlags(false, false,
				false, false));

		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).sequenceNumber(2)
				.prevRecordHash(predecessorDigest).unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertFalse(plan.getFlags().isCoverageGap());
		assertFalse(plan.getFlags().isChainLinkFailure());
	}

	// -- plan: equivocation --------------------------------------------------

	@Test
	void testPlan_EquivocationPeerAtSamePosition_FlaggedWithPeerIdentity() throws DuplicateEntryException {
		registerChain();
		String peerRecordId = recordId();
		insertRecord(peerRecordId, digest("1"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));

		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertTrue(plan.getFlags().isEquivocation());
		assertEquals(1, plan.getEquivocationPeers().size());
		assertEquals(new RecordIdentity(DOMAIN, PRODUCER, peerRecordId), plan.getEquivocationPeers().get(0));
	}

	// -- plan: successors captured for afterInsert -------------------------------------------------

	@Test
	void testPlan_SuccessorAtNextSequence_Captured() throws DuplicateEntryException {
		registerChain();
		String successorRecordId = recordId();
		StoredTraceRecord successor = insertRecord(successorRecordId, digest("2"), 2L, digest("placeholder"),
				new IngestionFlags(true, false, false, false));

		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertEquals(1, plan.getSuccessors().size());
		assertEquals(successor.getIdentity(), plan.getSuccessors().get(0).getIdentity());
	}

	// -- plan: index projection ---------------------------------------------------------------------

	@Test
	void testPlan_TwoTokenRefsSameJtiDifferentIssuer_TwoDistinctTokenKeys() throws IOException {
		registerChain();
		String baseJson = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis().unsignedJson();
		String json = addSecondToken(baseJson, "9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e", "https://other.example.org");

		CorrelationPlan plan = service.plan(buildVerified(json, context()), context());

		assertEquals(2, plan.getTokenKeys().size());
	}

	// -- plan: lateness wiring (exhaustive matrix in ProducerChainRulesTest) ---

	@Test
	void testPlan_SignedAtOlderThanConfiguredThreshold_LateTrue() {
		registerChain();
		traceConfiguration.setLatenessThresholdSeconds(10);
		long nowSeconds = Instant.now().getEpochSecond();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis()
				.signedAt(nowSeconds - 1000).unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context(nowSeconds * 1000)), context(nowSeconds * 1000));

		assertTrue(plan.getFlags().isLate());
	}

	@Test
	void testPlan_SignedAtWithinConfiguredThreshold_LateFalse() {
		registerChain();
		traceConfiguration.setLatenessThresholdSeconds(1000);
		long nowSeconds = Instant.now().getEpochSecond();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId()).genesis()
				.signedAt(nowSeconds - 10).unsignedJson();

		CorrelationPlan plan = service.plan(buildVerified(json, context(nowSeconds * 1000)), context(nowSeconds * 1000));

		assertFalse(plan.getFlags().isLate());
	}

	// -- afterInsert: successor coverage-gap clearing and chain-link failure --

	@Test
	void testAfterInsert_OutOfOrderArrival_SuccessorCoverageGapClearedAndLinkFailureSetOnMismatch() throws DuplicateEntryException {
		registerChain();
		String successorRecordId = recordId();
		insertRecord(successorRecordId, digest("2"), 2L, digest("mismatch"),
				new IngestionFlags(true, false, false, false));

		String recordId1 = recordId();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId1).genesis().unsignedJson();
		VerifiedAssertion verified1 = buildVerified(json, context());
		CorrelationPlan plan1 = service.plan(verified1, context());

		StoredTraceRecord stored1 = insertRecord(recordId1, verified1.getContentDigest(), 1L,
				TraceConstants.ZERO_HASH, plan1.getFlags());

		service.afterInsert(stored1, plan1);

		StoredTraceRecord successorAfter = store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, successorRecordId))
				.orElseThrow(AssertionError::new);
		assertFalse(successorAfter.getFlags().isCoverageGap());
		assertTrue(successorAfter.getFlags().isChainLinkFailure());
	}

	@Test
	void testAfterInsert_OutOfOrderArrival_SuccessorCoverageGapClearedAndLinkIntactOnMatch() throws DuplicateEntryException {
		registerChain();
		String successorRecordId = recordId();

		String recordId1 = recordId();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId1).genesis().unsignedJson();
		VerifiedAssertion verified1 = buildVerified(json, context());

		insertRecord(successorRecordId, digest("2"), 2L, verified1.getContentDigest(),
				new IngestionFlags(true, false, false, false));

		CorrelationPlan plan1 = service.plan(verified1, context());

		StoredTraceRecord stored1 = insertRecord(recordId1, verified1.getContentDigest(), 1L,
				TraceConstants.ZERO_HASH, plan1.getFlags());

		service.afterInsert(stored1, plan1);

		StoredTraceRecord successorAfter = store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, successorRecordId))
				.orElseThrow(AssertionError::new);
		assertFalse(successorAfter.getFlags().isCoverageGap());
		assertFalse(successorAfter.getFlags().isChainLinkFailure());
	}

	// -- afterInsert: equivocation ------------------------------------------

	@Test
	void testAfterInsert_Equivocation_BothRecordsFlaggedAfterSecondInsert() throws DuplicateEntryException {
		registerChain();

		String recordId1 = recordId();
		String json1 = SignedAssertionFactory.capabilityInvoked().recordId(recordId1).genesis().unsignedJson();
		VerifiedAssertion verified1 = buildVerified(json1, context());
		CorrelationPlan plan1 = service.plan(verified1, context());
		assertFalse(plan1.getFlags().isEquivocation(), "store is empty when the first record is planned");
		StoredTraceRecord stored1 = insertRecord(recordId1, verified1.getContentDigest(), 1L,
				TraceConstants.ZERO_HASH, plan1.getFlags());
		service.afterInsert(stored1, plan1);

		String recordId2 = recordId();
		String json2 = SignedAssertionFactory.capabilityInvoked().recordId(recordId2).genesis().unsignedJson();
		VerifiedAssertion verified2 = buildVerified(json2, context());
		CorrelationPlan plan2 = service.plan(verified2, context());
		assertTrue(plan2.getFlags().isEquivocation());
		StoredTraceRecord stored2 = insertRecord(recordId2, verified2.getContentDigest(), 1L, TraceConstants.ZERO_HASH,
				plan2.getFlags());

		service.afterInsert(stored2, plan2);

		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, recordId1)).get().getFlags().isEquivocation());
		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, recordId2)).get().getFlags().isEquivocation());
	}

	// -- afterInsert: peer landing between plan() and insert (re-query of the chain position) --

	@Test
	void testAfterInsert_PeerInsertedBetweenPlanAndInsert_BothRowsFlaggedViaReQuery() throws DuplicateEntryException {
		registerChain();

		String recordId1 = recordId();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId1).genesis().unsignedJson();
		VerifiedAssertion verified1 = buildVerified(json, context());
		CorrelationPlan plan1 = service.plan(verified1, context());
		assertFalse(plan1.getFlags().isEquivocation(), "store is empty when record1 is planned");

		// A peer lands at the same chain position after plan() but before this node's own
		// insertRecord() -- plan1's pre-insert snapshot never saw it.
		String peerRecordId = recordId();
		StoredTraceRecord peer = insertRecord(peerRecordId, digest("1"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));

		StoredTraceRecord stored1 = insertRecord(recordId1, verified1.getContentDigest(), 1L,
				TraceConstants.ZERO_HASH, plan1.getFlags());

		service.afterInsert(stored1, plan1);

		assertTrue(store.findRecord(stored1.getIdentity()).get().getFlags().isEquivocation(),
				"the just-inserted row's own equivocation flag must also be corrected");
		assertTrue(store.findRecord(peer.getIdentity()).get().getFlags().isEquivocation());
	}

	// -- afterInsert: concurrent updates to the same peer, no lost update ---

	@Test
	void testAfterInsert_ConcurrentUpdatesToSamePeer_BothSucceedViaRetry_NoLostUpdate() throws Exception {
		registerChain();
		String peerRecordId = recordId();
		StoredTraceRecord peer = insertRecord(peerRecordId, digest("1"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));
		RecordIdentity peerIdentity = peer.getIdentity();

		String insertedId1 = recordId();
		String insertedId2 = recordId();
		StoredTraceRecord inserted1 = insertRecord(insertedId1, digest("2"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));
		StoredTraceRecord inserted2 = insertRecord(insertedId2, digest("3"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));

		ChainPosition position = new ChainPosition(chainIdentity(), 1L);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, "authority-1", "exec-1");
		CorrelationPlan plan1 = new CorrelationPlan(new IngestionFlags(false, false, false, false), position,
				execution, Collections.emptyList(), Collections.singletonList(peerIdentity), Collections.emptyList(),
				Collections.emptyList());
		CorrelationPlan plan2 = new CorrelationPlan(new IngestionFlags(false, false, false, false), position,
				execution, Collections.emptyList(), Collections.singletonList(peerIdentity), Collections.emptyList(),
				Collections.emptyList());

		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<?>> futures = new ArrayList<>();
			futures.add(executor.submit(() -> service.afterInsert(inserted1, plan1)));
			futures.add(executor.submit(() -> service.afterInsert(inserted2, plan2)));
			executor.shutdown();
			assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
			for (Future<?> future : futures) {
				future.get();
			}
		} finally {
			executor.shutdownNow();
		}

		assertTrue(store.findRecord(peerIdentity).get().getFlags().isEquivocation(),
				"neither concurrent writer's correction may be lost to the other");
	}

	// -- afterInsert: best-effort, never propagates a neighbor-update failure ----------------

	@Test
	void testAfterInsert_NeighborUpdateThrows_LoggedAndOtherNeighborStillUpdated() throws DuplicateEntryException {
		registerChain();
		String failingPeerId = recordId();
		String okPeerId = recordId();
		insertRecord(failingPeerId, digest("1"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));
		insertRecord(okPeerId, digest("2"), 1L, TraceConstants.ZERO_HASH,
				new IngestionFlags(false, false, false, false));

		ThrowingOnceTraceStore throwingStore = new ThrowingOnceTraceStore(store,
				new RecordIdentity(DOMAIN, PRODUCER, failingPeerId));
		setField(service, "store", throwingStore);

		String recordId3 = recordId();
		String json = SignedAssertionFactory.capabilityInvoked().recordId(recordId3).genesis().unsignedJson();
		VerifiedAssertion verified = buildVerified(json, context());
		CorrelationPlan plan = service.plan(verified, context());
		StoredTraceRecord stored = insertRecord(recordId3, verified.getContentDigest(), 1L, TraceConstants.ZERO_HASH,
				plan.getFlags());

		service.afterInsert(stored, plan);

		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, okPeerId)).get().getFlags().isEquivocation());
	}

	/**
	 * Delegates every call to the wrapped store except {@code updateRecordFlags} for one specific
	 * identity, which throws once — used to prove {@link TraceCorrelationService#afterInsert} logs
	 * and continues rather than propagating a neighbor-update failure.
	 */
	private static final class ThrowingOnceTraceStore extends InMemoryTraceStore {

		private final InMemoryTraceStore delegate;

		private final RecordIdentity failingIdentity;

		private boolean thrown;

		ThrowingOnceTraceStore(InMemoryTraceStore delegate, RecordIdentity failingIdentity) {
			this.delegate = delegate;
			this.failingIdentity = failingIdentity;
		}

		@Override
		public boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags, long expectedVersion) {
			if (!thrown && failingIdentity.equals(id)) {
				thrown = true;
				throw new TraceStorageException("test_forced_failure", "forced for test");
			}
			return delegate.updateRecordFlags(id, flags, expectedVersion);
		}

		@Override
		public java.util.Optional<StoredTraceRecord> findRecord(RecordIdentity id) {
			return delegate.findRecord(id);
		}

		@Override
		public List<StoredTraceRecord> findRecordsByChainPosition(io.jans.lock.service.trace.model.ChainPosition position) {
			return delegate.findRecordsByChainPosition(position);
		}

		@Override
		public void insertRecord(StoredTraceRecord traceRecord) throws DuplicateEntryException {
			delegate.insertRecord(traceRecord);
		}

	}

}
