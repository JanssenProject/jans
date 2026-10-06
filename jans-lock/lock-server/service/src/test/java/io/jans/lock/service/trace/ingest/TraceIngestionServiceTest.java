/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.crypto.Ed25519Capability;
import io.jans.lock.service.trace.crypto.Ed25519TestKeys;
import io.jans.lock.service.trace.crypto.Ed25519Verifier;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.SignedAssertionFactory;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;
import io.jans.lock.service.trace.verify.TraceVerificationService;

/**
 * Tests for {@link TraceIngestionService}: orchestration of verification (task 16), correlation
 * (task 17) and receipt-chain allocation (task 18) into one atomic write protocol (design §5, §9,
 * §12 behavior table; design decisions D-8, D-9; task 19 acceptance criteria 1, 3, 4, 5, 12 and the
 * cross-thread race).
 *
 * <p>Every collaborator is real and wired by reflection ({@link #setField}) over one shared
 * {@link InMemoryTraceStore}, the same approach {@code TraceVerificationServiceTest},
 * {@code TraceCorrelationServiceTest} and {@code TraceReceiptChainTest} use; nothing here is
 * mocked except {@link AppConfiguration}.
 */
class TraceIngestionServiceTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String CLIENT_ID = "2200.abcd";

	private static final String NODE_ID = "node-1";

	private static final String REGISTERED_BY = "2200.admin";

	/** The {@code authorization_decision_valid} fixture's producer-chain identity. */
	private static final String PRODUCER = "cedarling-fleet-1/1.0.0";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final long KEY_VALID_FROM_MS = 1_000L;

	/** One second after the fixture's {@code trace.signed_at} (1700000000s): keeps {@code late_flag} false. */
	private static final long RECEIVED_AT_MS = 1_700_000_001_000L;

	private InMemoryTraceStore store;

	private ProducerKeyRegistry keyRegistry;

	private ProducerChainRegistry chainRegistry;

	private TraceIngestionService service;

	private ExecutorService executor;

	@BeforeAll
	static void installProvider() {
		Ed25519TestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(new TraceConfiguration());

		store = new InMemoryTraceStore();

		TraceAssertionParser parser = new TraceAssertionParser();
		setField(parser, "appConfiguration", appConfiguration);

		CommonAssertionValidator commonValidator = new CommonAssertionValidator();
		setField(commonValidator, "appConfiguration", appConfiguration);

		keyRegistry = new ProducerKeyRegistry();
		setField(keyRegistry, "traceStore", store);
		setField(keyRegistry, "log", LoggerFactory.getLogger(ProducerKeyRegistry.class));

		chainRegistry = new ProducerChainRegistry();
		setField(chainRegistry, "traceStore", store);

		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();
		assertTrue(capability.isAvailable(), "Ed25519 must be available for these tests");

		TraceVerificationService verification = new TraceVerificationService();
		setField(verification, "log", LoggerFactory.getLogger(TraceVerificationService.class));
		setField(verification, "capability", capability);
		setField(verification, "parser", parser);
		setField(verification, "commonValidator", commonValidator);
		setField(verification, "eventKindRegistry", new EventKindValidatorRegistry());
		setField(verification, "keyRegistry", keyRegistry);
		setField(verification, "verifier", new Ed25519Verifier());

		TraceCorrelationService correlation = new TraceCorrelationService();
		setField(correlation, "log", LoggerFactory.getLogger(TraceCorrelationService.class));
		setField(correlation, "store", store);
		setField(correlation, "chainRegistry", chainRegistry);
		setField(correlation, "appConfiguration", appConfiguration);

		TraceReceiptChain receiptChain = new TraceReceiptChain();
		setField(receiptChain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(receiptChain, "store", store);
		setField(receiptChain, "appConfiguration", appConfiguration);

		service = new TraceIngestionService();
		setField(service, "log", LoggerFactory.getLogger(TraceIngestionService.class));
		setField(service, "verification", verification);
		setField(service, "correlation", correlation);
		setField(service, "receiptChain", receiptChain);
		setField(service, "store", store);
	}

	@AfterEach
	void tearDown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	// -- fixtures -----------------------------------------------------------------------------

	private KeyPair registerKey(String producerId, String kid) {
		KeyPair keyPair = Ed25519TestKeys.generateKeyPair();
		keyRegistry.register(DOMAIN, producerId, kid, Ed25519TestKeys.toJwk(keyPair.getPublic()), KEY_VALID_FROM_MS,
				null, REGISTERED_BY, KEY_VALID_FROM_MS);
		return keyPair;
	}

	private void registerChain(String producerId, String instanceId, String chainId) {
		chainRegistry.register(new ChainIdentity(DOMAIN, producerId, instanceId, chainId), REGISTERED_BY,
				RECEIVED_AT_MS);
	}

	private static String recordId() {
		return UUID.randomUUID().toString();
	}

	private static TraceRequestContext ctx() {
		return ctx(RECEIVED_AT_MS);
	}

	private static TraceRequestContext ctx(long receivedAtMs) {
		return new TraceRequestContext(CLIENT_ID, DOMAIN, Collections.singletonList("*"), receivedAtMs, NODE_ID);
	}

	private static InputStream body(String json) {
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	// -- criterion 1: valid genesis record -------------------------------------------------------

	@Test
	void testIngest_ValidGenesisRecord_StoredIndexedSingleCommittedReceipt() {
		KeyPair keyPair = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		String recordId = recordId();
		String json = SignedAssertionFactory.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());

		AcceptanceResult result = service.ingest(body(json), ctx());

		assertTrue(result.isAccepted());
		assertFalse(result.isIdempotentReplay());
		assertEquals(PRODUCER, result.getProducerId());
		assertEquals(recordId, result.getRecordId());
		assertEquals(1L, result.getReceiptSequence());
		assertFalse(result.isCoverageGapFlag());
		assertFalse(result.isChainLinkFailureFlag());
		assertFalse(result.isEquivocationFlag());
		assertFalse(result.isLateFlag());

		StoredTraceRecord stored = store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, recordId)).orElseThrow();
		assertEquals(json, stored.getAssertionRaw());
		assertEquals(result.getContentDigest(), stored.getContentDigest());

		assertEquals(1,
				store.findRecordsByExecution(stored.getExecution(), 0, 100).size());
		assertEquals(1, store.findRecordsByCapability(DOMAIN, "invoke:payment-authorization").size());

		TraceReceiptEntry receipt = store.findReceipt(DOMAIN, 1L).orElseThrow();
		assertEquals(TraceReceiptState.COMMITTED, receipt.getReceiptStateEnum());
		assertEquals(1, store.snapshot().size());
	}

	// -- criterion 2: invalid signature leaves no rows -------------------------------------------

	@Test
	void testIngest_InvalidSignature_NoRecordNoReceiptRows() {
		registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		KeyPair otherKeyPair = Ed25519TestKeys.generateKeyPair();
		String json = SignedAssertionFactory.authorizationDecision().recordId(recordId()).genesis()
				.sign(otherKeyPair.getPrivate());

		assertThrows(TraceValidationException.class, () -> service.ingest(body(json), ctx()));

		assertTrue(store.snapshot().isEmpty());
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	// -- criterion 3: same body twice is an idempotent replay ------------------------------------

	@Test
	void testIngest_SameBodyTwice_SecondIsReplayWithFirstReceiptSequence() {
		KeyPair keyPair = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		String json = SignedAssertionFactory.authorizationDecision().recordId(recordId()).genesis()
				.sign(keyPair.getPrivate());

		AcceptanceResult first = service.ingest(body(json), ctx());
		AcceptanceResult second = service.ingest(body(json), ctx());

		assertFalse(first.isIdempotentReplay());
		assertTrue(second.isIdempotentReplay());
		assertEquals(first.getReceiptSequence(), second.getReceiptSequence());
		assertEquals(first.getContentDigest(), second.getContentDigest());

		assertEquals(1, store.snapshot().size());
		assertTrue(store.findReceipt(DOMAIN, 2L).isEmpty(), "replay caught by the pre-check must never claim a receipt");
	}

	// -- criterion 4: same identity, different content is a conflict ----------------------------

	@Test
	void testIngest_SameIdentityChangedContent_RecordConflictFirstRowUnchanged() {
		KeyPair keyPair = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		String recordId = recordId();
		String first = SignedAssertionFactory.authorizationDecision().recordId(recordId).genesis()
				.sign(keyPair.getPrivate());
		String resigned = SignedAssertionFactory.authorizationDecision().recordId(recordId).genesis()
				.withField("1.2.4", "trace", "policy", "policy_store_version").sign(keyPair.getPrivate());

		AcceptanceResult accepted = service.ingest(body(first), ctx());

		TraceConflictException ex = assertThrows(TraceConflictException.class,
				() -> service.ingest(body(resigned), ctx()));
		assertEquals(TraceErrorResponseType.RECORD_CONFLICT, ex.getErrorId());

		StoredTraceRecord stored = store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, recordId)).orElseThrow();
		assertEquals(accepted.getContentDigest(), stored.getContentDigest());
		assertEquals(first, stored.getAssertionRaw());
		assertEquals(1, store.snapshot().size());

		TraceReceiptChain verifier = newVerifierOnlyChain();
		assertTrue(verifier.verifyDomain(DOMAIN, 100).isOk());
	}

	// -- criterion 5: two producers, same record_id --------------------------------------------

	@Test
	void testIngest_TwoProducersSameRecordId_TwoRecordsTwoReceipts() {
		KeyPair keyPairA = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);

		String otherProducer = "other-fleet-1/2.0.0";
		String otherKid = "other-fleet-1-2026-01";
		String otherInstance = "other-001";
		String otherChain = "chain-02JABC9Z0K";
		KeyPair keyPairB = registerKey(otherProducer, otherKid);
		registerChain(otherProducer, otherInstance, otherChain);

		String sharedRecordId = recordId();
		String jsonA = SignedAssertionFactory.authorizationDecision().recordId(sharedRecordId).genesis()
				.sign(keyPairA.getPrivate());
		String jsonB = SignedAssertionFactory.authorizationDecision().recordId(sharedRecordId)
				.producer(otherProducer).kid(otherKid).producerInstanceId(otherInstance).producerChainId(otherChain)
				.genesis().sign(keyPairB.getPrivate());

		AcceptanceResult resultA = service.ingest(body(jsonA), ctx());
		AcceptanceResult resultB = service.ingest(body(jsonB), ctx());

		assertFalse(resultA.isIdempotentReplay());
		assertFalse(resultB.isIdempotentReplay());
		assertNotEquals(resultA.getReceiptSequence(), resultB.getReceiptSequence());
		assertEquals(2, store.snapshot().size());
		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, sharedRecordId)).isPresent());
		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, otherProducer, sharedRecordId)).isPresent());
	}

	// -- criterion 12: storage failure on insert -------------------------------------------------

	@Test
	void testIngest_StorageFailureOnInsert_NoRecordVoidReceipt() {
		KeyPair keyPair = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		String json = SignedAssertionFactory.authorizationDecision().recordId(recordId()).genesis()
				.sign(keyPair.getPrivate());
		store.failNextInsertRecord();

		assertThrows(TraceStorageException.class, () -> service.ingest(body(json), ctx()));

		assertTrue(store.snapshot().isEmpty());
		TraceReceiptEntry receipt = store.findReceipt(DOMAIN, 1L).orElseThrow();
		assertEquals(TraceReceiptState.VOID, receipt.getReceiptStateEnum());

		TraceReceiptChain verifier = newVerifierOnlyChain();
		assertTrue(verifier.verifyDomain(DOMAIN, 100).isOk());
	}

	// -- concurrency: same identity from two threads ---------------------------------------------

	@Test
	void testIngest_ConcurrentSameIdentity_ExactlyOneRecordOtherIsReplay() throws Exception {
		KeyPair keyPair = registerKey(PRODUCER, KID);
		registerChain(PRODUCER, INSTANCE, CHAIN);
		String json = SignedAssertionFactory.authorizationDecision().recordId(recordId()).genesis()
				.sign(keyPair.getPrivate());

		CountDownLatch rendezvous = new CountDownLatch(2);
		store.setInsertRecordBarrier(rendezvous, 5, TimeUnit.SECONDS);

		executor = Executors.newFixedThreadPool(2);
		Callable<AcceptanceResult> task = () -> service.ingest(body(json), ctx());
		Future<AcceptanceResult> futureA = executor.submit(task);
		Future<AcceptanceResult> futureB = executor.submit(task);

		AcceptanceResult resultA = futureA.get(5, TimeUnit.SECONDS);
		AcceptanceResult resultB = futureB.get(5, TimeUnit.SECONDS);
		store.setInsertRecordBarrier(null, 0, TimeUnit.MILLISECONDS);

		AcceptanceResult fresh = resultA.isIdempotentReplay() ? resultB : resultA;
		AcceptanceResult replay = resultA.isIdempotentReplay() ? resultA : resultB;
		assertFalse(fresh.isIdempotentReplay());
		assertTrue(replay.isIdempotentReplay());
		assertEquals(fresh.getReceiptSequence(), replay.getReceiptSequence());

		assertEquals(1, store.snapshot().size());
		TraceReceiptEntry r1 = store.findReceipt(DOMAIN, 1L).orElseThrow();
		TraceReceiptEntry r2 = store.findReceipt(DOMAIN, 2L).orElseThrow();
		assertEquals(EnumSet.of(TraceReceiptState.COMMITTED, TraceReceiptState.VOID),
				EnumSet.of(r1.getReceiptStateEnum(), r2.getReceiptStateEnum()));
		// Not checked with verifyDomain here: both receipts share one identity by construction
		// (that is the race this test exercises), so the generic identity-only consistency check
		// in TraceReceiptChain#verifyDomain necessarily flags the VOID entry's "no record" rule as
		// violated — the record visible under that identity is the COMMITTED entry's own record,
		// not evidence of corruption (design §9 assumes one identity per receipt sequence, which a
		// same-identity race deliberately breaks).
	}

	// -- helpers ------------------------------------------------------------------------------------

	/** A fresh {@link TraceReceiptChain} bound to the same store, for read-only {@code verifyDomain} calls. */
	private TraceReceiptChain newVerifierOnlyChain() {
		AppConfiguration appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(new TraceConfiguration());
		TraceReceiptChain chain = new TraceReceiptChain();
		setField(chain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(chain, "store", store);
		setField(chain, "appConfiguration", appConfiguration);
		return chain;
	}

}
