/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.security.KeyPair;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.Entry;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.EntryStatus;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

/**
 * The receipt state protocol (design §9): terminal states, fencing and the interleavings of claim,
 * record insert and settle that follow from having no multi-entry transaction. Each test forces one
 * interleaving with {@link InMemoryTraceStore} hooks; the harness's "now" is in 2023, so every claim
 * is older than {@code pendingReceiptTimeoutSeconds} for the repair timer, whose cutoff reads the
 * system clock.
 *
 * <p>Interleavings: A (repair voids while the worker is paused before its insert), B (two settlers
 * with opposite outcomes), C (insert commits but the caller sees a failure), D (a later claim while an
 * earlier one is unsettled), E (a duplicate-identity race loser that could not void itself).
 */
class TraceReceiptProtocolTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN = "chain-01JABC9Z0K";

	private static final String REASON_VOID_AFTER_INSERT = "receipt_void_after_insert";

	private static final String REASON_CLAIM_EXPIRED = "receipt_claim_expired";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceTestHarness harness;

	private InMemoryTraceStore store;

	private KeyPair key;

	private ExecutorService executor;

	@BeforeAll
	static void installProvider() {
		TraceTestKeys.installProvider();
	}

	@BeforeEach
	void setUp() {
		harness = new TraceTestHarness();
		store = harness.store();
		key = harness.registerKey(DOMAIN, PRODUCER, KID);
		harness.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN);
	}

	@AfterEach
	void tearDown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	private String genesisJson(String recordId) {
		return TraceRecordBuilder.authorizationDecision().recordId(recordId).genesis().sign(key.getPrivate());
	}

	private AcceptanceResult ingest(String json) {
		return harness.ingest(json, harness.ctx(DOMAIN));
	}

	private static String digestOf(String json) {
		try {
			return CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String digest(char c) {
		char[] chars = new char[64];
		java.util.Arrays.fill(chars, c);
		return "sha256:" + new String(chars);
	}

	private TraceReceiptState stateOf(long seq) {
		return store.findReceipt(DOMAIN, seq).orElseThrow().getReceiptStateEnum();
	}

	private ReceiptClaim claim(String recordId, char digestChar) {
		return harness.receiptChain().claim(DOMAIN, PRODUCER, recordId, recordId + "-key", digest(digestChar),
				harness.now(), TraceTestHarness.DEFAULT_NODE_ID);
	}

	private void insertRecord(String recordId, String contentDigest, long receiptSequence) throws Exception {
		store.insertRecord(StoredTraceRecordFactory.builder().domainId(DOMAIN).producerId(PRODUCER).recordId(recordId)
				.contentDigest(contentDigest).receiptSequence(receiptSequence).build());
	}

	// -- A: repair voids the claim while the worker is paused ------------------------------------

	@Test
	void testInterleavingA_RepairVoidsBeforeInsert_RecordOrphanedReceiptStaysVoid() {
		TraceCorrelationService correlationSpy = spy(harness.correlation());
		setField(harness.ingestionService(), "correlation", correlationSpy);
		String json = genesisJson(TraceRecordBuilder.freshRecordId());
		String recordId = jsonRecordId(json);

		// The hook runs after the ingest fence check and before the record is visible: it is the
		// paused worker, resumed after repair had the right to void its claim.
		store.setBeforeInsertRecord(() -> {
			store.setBeforeInsertRecord(null);
			harness.repairPendingReceipts();
		});

		TraceStorageException ex = assertThrows(TraceStorageException.class, () -> ingest(json));

		assertEquals(REASON_VOID_AFTER_INSERT, ex.getReason());
		assertEquals(TraceReceiptState.VOID, stateOf(1L));
		assertTrue(store.findRecord(new RecordIdentity(DOMAIN, PRODUCER, recordId)).isPresent(),
				"records are immutable: the orphan stays");
		verify(correlationSpy, never()).afterInsert(any(), any());

		TraceStorageException retry = assertThrows(TraceStorageException.class, () -> ingest(json));
		assertEquals(REASON_VOID_AFTER_INSERT, retry.getReason());
		assertEquals(1, store.snapshot().size());
		assertFalse(store.findReceipt(DOMAIN, 2L).isPresent(), "the guard fires before any new claim");

		ChainVerificationReport report = harness.verifyDomain(DOMAIN);
		assertFalse(report.isOk());
		assertEquals(EntryStatus.RECORD_UNEXPECTED, report.getEntries().get(0).getStatus());
	}

	private static String jsonRecordId(String json) {
		try {
			return MAPPER.readTree(json).path("record_id").asText();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	// -- fencing ------------------------------------------------------------------------------------

	@Test
	void testFence_WorkerPastDeadline_NeverInsertsAndVoidsClaim() {
		TraceReceiptChain realChain = harness.receiptChain();
		TraceReceiptChain chainSpy = spy(realChain);
		doAnswer(inv -> {
			ReceiptClaim claimed = (ReceiptClaim) inv.callRealMethod();
			long longAgo = harness.now() - 3_600_000L;
			return new ReceiptClaim(claimed.getSeq(), claimed.getPrevReceiptHash(), claimed.getReceiptHash(),
					claimed.getReceivedAtMs(), longAgo);
		}).when(chainSpy).claim(any(), any(), any(), any(), any(), anyLong(), any());
		setField(harness.ingestionService(), "receiptChain", chainSpy);
		store.setBeforeInsertRecord(() -> fail("a worker past its fence must not insert"));

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> ingest(genesisJson(TraceRecordBuilder.freshRecordId())));

		assertEquals(REASON_CLAIM_EXPIRED, ex.getReason());
		assertEquals(TraceReceiptState.VOID, stateOf(1L));
		assertTrue(store.snapshot().isEmpty());
		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

	@Test
	void testFenceMs_HalfOfPendingTimeout_StrictlyBelowIt() {
		harness.traceConfiguration().setPendingReceiptTimeoutSeconds(120);
		assertEquals(60_000L, harness.receiptChain().fenceMs());

		harness.traceConfiguration().setPendingReceiptTimeoutSeconds(0);
		assertEquals(60_000L, harness.receiptChain().fenceMs(), "0 falls back to the 120 s default");

		harness.traceConfiguration().setPendingReceiptTimeoutSeconds(30);
		assertEquals(15_000L, harness.receiptChain().fenceMs());
	}

	@Test
	void testFence_FreshClaimNotExpired() {
		ReceiptClaim fresh = claim("rec-fresh", '1');

		assertFalse(harness.receiptChain().isFenceExpired(fresh));
		assertTrue(harness.receiptChain().isFenceExpired(new ReceiptClaim(fresh.getSeq(), fresh.getPrevReceiptHash(),
				fresh.getReceiptHash(), fresh.getReceivedAtMs(),
				harness.now() - harness.receiptChain().fenceMs() - 1)));
	}

	@Test
	void testFence_ClaimMadeLateInRequest_ExpiresAtReceivedDeadlineBeforeClaimDeadline() {
		harness.traceConfiguration().setPendingReceiptTimeoutSeconds(100);
		long nowMs = harness.now();
		// Claimed just now, but the request was received 80 s ago: past 3/4 of 100 s counted from receipt,
		// although the claim based fence (50 s after the claim) has not ended.
		ReceiptClaim late = new ReceiptClaim(1L, TraceConstants.ZERO_HASH, TraceConstants.ZERO_HASH, nowMs - 80_000L, nowMs);
		ReceiptClaim prompt = new ReceiptClaim(1L, TraceConstants.ZERO_HASH, TraceConstants.ZERO_HASH, nowMs - 70_000L, nowMs);

		assertEquals(75_000L, harness.receiptChain().receivedFenceMs());
		assertTrue(harness.receiptChain().isFenceExpired(late));
		assertFalse(harness.receiptChain().isFenceExpired(prompt));
	}

	// -- B: two settlers with opposite outcomes ------------------------------------------------------

	@Test
	void testInterleavingB_CommitThenVoid_VoidLosesTerminalStateKept() {
		TraceReceiptChain chain = harness.receiptChain();
		ReceiptClaim claimed = claim("rec-b1", '1');

		assertEquals(ReceiptSettlement.SETTLED, chain.commit(DOMAIN, claimed.getSeq()));
		assertEquals(ReceiptSettlement.CONFLICT, chain.voidClaim(DOMAIN, claimed.getSeq()));
		assertEquals(TraceReceiptState.COMMITTED, stateOf(claimed.getSeq()));
	}

	@Test
	void testInterleavingB_VoidThenCommit_CommitLosesTerminalStateKept() {
		TraceReceiptChain chain = harness.receiptChain();
		ReceiptClaim claimed = claim("rec-b2", '2');

		assertEquals(ReceiptSettlement.SETTLED, chain.voidClaim(DOMAIN, claimed.getSeq()));
		assertEquals(ReceiptSettlement.CONFLICT, chain.commit(DOMAIN, claimed.getSeq()));
		assertEquals(TraceReceiptState.VOID, stateOf(claimed.getSeq()));
	}

	@Test
	void testInterleavingB_ConcurrentOppositeSettlers_ExactlyOneWins() throws Exception {
		TraceReceiptChain chain = harness.receiptChain();
		executor = Executors.newFixedThreadPool(2);
		for (int round = 0; round < 50; round++) {
			ReceiptClaim claimed = claim("rec-race-" + round, '3');
			CountDownLatch start = new CountDownLatch(1);
			Callable<ReceiptSettlement> committer = () -> {
				start.await();
				return chain.commit(DOMAIN, claimed.getSeq());
			};
			Callable<ReceiptSettlement> voider = () -> {
				start.await();
				return chain.voidClaim(DOMAIN, claimed.getSeq());
			};
			Future<ReceiptSettlement> committed = executor.submit(committer);
			Future<ReceiptSettlement> voided = executor.submit(voider);
			start.countDown();
			ReceiptSettlement commitOutcome = committed.get(5, TimeUnit.SECONDS);
			ReceiptSettlement voidOutcome = voided.get(5, TimeUnit.SECONDS);

			assertTrue(
					commitOutcome == ReceiptSettlement.SETTLED && voidOutcome == ReceiptSettlement.CONFLICT
							|| commitOutcome == ReceiptSettlement.CONFLICT && voidOutcome == ReceiptSettlement.SETTLED,
					"round " + round + ": commit=" + commitOutcome + ", void=" + voidOutcome);
			TraceReceiptState winner = commitOutcome == ReceiptSettlement.SETTLED ? TraceReceiptState.COMMITTED
					: TraceReceiptState.VOID;
			assertEquals(winner, stateOf(claimed.getSeq()));
		}
	}

	// -- idempotency ---------------------------------------------------------------------------------

	@Test
	void testSettle_SameTerminalStateTwice_AlreadySettled() {
		TraceReceiptChain chain = harness.receiptChain();
		ReceiptClaim claimed = claim("rec-idem", '4');

		assertEquals(ReceiptSettlement.SETTLED, chain.commit(DOMAIN, claimed.getSeq()));
		assertEquals(ReceiptSettlement.ALREADY_SETTLED, chain.commit(DOMAIN, claimed.getSeq()));
		assertEquals(TraceReceiptState.COMMITTED, stateOf(claimed.getSeq()));
	}

	@Test
	void testSettle_UnknownSequence_NotFound() {
		assertEquals(ReceiptSettlement.NOT_FOUND, harness.receiptChain().commit(DOMAIN, 99L));
	}

	@Test
	void testSettle_StoreFailure_FailedAndClaimStaysPending() {
		ReceiptClaim claimed = claim("rec-fail", '5');
		store.failNextSettleReceipt();

		assertEquals(ReceiptSettlement.FAILED, harness.receiptChain().commit(DOMAIN, claimed.getSeq()));
		assertEquals(TraceReceiptState.PENDING, stateOf(claimed.getSeq()));
	}

	// -- C: the insert commits, the caller sees a failure ----------------------------------------------

	@Test
	void testInterleavingC_InsertLandedThenFailure_CommittedAndSuccess() {
		String json = genesisJson(TraceRecordBuilder.freshRecordId());
		store.failAfterNextInsertRecord();

		AcceptanceResult result = ingest(json);

		assertTrue(result.isAccepted());
		assertFalse(result.isIdempotentReplay());
		assertEquals(1L, result.getReceiptSequence());
		assertEquals(TraceReceiptState.COMMITTED, stateOf(1L));
		assertEquals(1, store.snapshot().size());
		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

	@Test
	void testInterleavingC_InsertLandedRereadFails_ClaimStaysPendingOriginalErrorRethrown() {
		String json = genesisJson(TraceRecordBuilder.freshRecordId());
		store.failAfterNextInsertRecord();
		// Armed at the start of insertRecord, so the pre-check read is already behind it.
		store.setBeforeInsertRecord(() -> {
			store.setBeforeInsertRecord(null);
			store.failNextFindRecord();
		});

		TraceStorageException ex = assertThrows(TraceStorageException.class, () -> ingest(json));

		assertEquals("test_fail_after_next_insert_record", ex.getReason());
		assertEquals(TraceReceiptState.PENDING, stateOf(1L));
		assertEquals(1, store.snapshot().size());

		harness.repairPendingReceipts();
		assertEquals(TraceReceiptState.COMMITTED, stateOf(1L), "repair settles it from the authoritative read");
	}

	@Test
	void testInterleavingC_InsertTrulyFailed_Void() {
		String json = genesisJson(TraceRecordBuilder.freshRecordId());
		store.failNextInsertRecord();

		TraceStorageException ex = assertThrows(TraceStorageException.class, () -> ingest(json));

		assertEquals("test_fail_next_insert_record", ex.getReason());
		assertEquals(TraceReceiptState.VOID, stateOf(1L));
		assertTrue(store.snapshot().isEmpty());
	}

	// -- D: a later claim while an earlier one is unsettled ---------------------------------------------

	@Test
	void testInterleavingD_LaterClaimWhileEarlierUnsettled_ChainStaysValid() throws Exception {
		ReceiptClaim first = claim("rec-d1", '6');
		ReceiptClaim second = claim("rec-d2", '7');

		assertEquals(first.getSeq() + 1, second.getSeq());
		assertEquals(first.getReceiptHash(), second.getPrevReceiptHash());
		insertRecord("rec-d2", digest('7'), second.getSeq());
		harness.receiptChain().commit(DOMAIN, second.getSeq());

		harness.receiptChain().voidClaim(DOMAIN, first.getSeq());

		ChainVerificationReport report = harness.verifyDomain(DOMAIN);
		assertTrue(report.isOk(), report.toString());
		List<Entry> entries = report.getEntries();
		assertEquals(TraceReceiptState.VOID, entries.get(0).getState());
		assertEquals(EntryStatus.OK, entries.get(0).getStatus());
		assertEquals(TraceReceiptState.COMMITTED, entries.get(1).getState());
	}

	// -- E: duplicate-identity race loser that could not void itself ------------------------------------

	@Test
	void testInterleavingE_LoserVoidFailed_RepairVoidsBecauseRecordCarriesWinnersSequence() {
		String json = genesisJson(TraceRecordBuilder.freshRecordId());
		String recordId = jsonRecordId(json);
		long winnerSequence = 99L;
		store.setBeforeInsertRecord(() -> {
			store.setBeforeInsertRecord(null);
			try {
				insertRecord(recordId, digestOf(json), winnerSequence);
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
			store.failNextSettleReceipt();
		});

		AcceptanceResult result = ingest(json);

		assertTrue(result.isIdempotentReplay(), "the loser is classified against the winner");
		assertEquals(winnerSequence, result.getReceiptSequence());
		assertEquals(TraceReceiptState.PENDING, stateOf(1L), "the loser's void failed");

		harness.repairPendingReceipts();

		assertEquals(TraceReceiptState.VOID, stateOf(1L));
		ChainVerificationReport report = harness.verifyDomain(DOMAIN);
		assertTrue(report.isOk(), "the loser tombstone is fine: the record belongs to the winner claim: " + report);
		assertEquals(TraceReceiptState.VOID, report.getEntries().get(0).getState());
	}

	@Test
	void testIsClaimSatisfied_RequiresSequenceAndDigest() throws Exception {
		ReceiptClaim claimed = claim("rec-sat", '8');
		var receipt = store.findReceipt(DOMAIN, claimed.getSeq()).orElseThrow();
		assertFalse(harness.receiptChain().isClaimSatisfied(receipt), "no record yet");

		insertRecord("rec-sat", digest('8'), claimed.getSeq() + 5);
		assertFalse(harness.receiptChain().isClaimSatisfied(receipt), "another sequence");
	}

	@Test
	void testVerifyDomain_CommittedWithOtherSequenceOnRecord_SequenceMismatch() throws Exception {
		ReceiptClaim claimed = claim("rec-seq", '9');
		insertRecord("rec-seq", digest('9'), claimed.getSeq() + 5);
		harness.receiptChain().commit(DOMAIN, claimed.getSeq());

		ChainVerificationReport report = harness.verifyDomain(DOMAIN);

		assertFalse(report.isOk());
		assertEquals(EntryStatus.SEQUENCE_MISMATCH, report.getEntries().get(0).getStatus());
	}

	@Test
	void testVerifyDomain_VoidWithoutRecord_StillOk() {
		ReceiptClaim claimed = claim("rec-void", 'a');
		harness.receiptChain().voidClaim(DOMAIN, claimed.getSeq());

		assertTrue(harness.verifyDomain(DOMAIN).isOk());
	}

}
