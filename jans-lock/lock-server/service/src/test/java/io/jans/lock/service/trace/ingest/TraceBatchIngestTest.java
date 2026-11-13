/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ChainVerificationReport;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.testkit.PolicyMismatchChecks;
import io.jans.lock.service.trace.testkit.TraceRecordBuilder;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;
import io.jans.lock.trace.crypto.bc.CanonicalHashes;

/**
 * Collision, fallback and concurrency behavior of {@link TraceIngestionService#ingestBatch}. The
 * same-result property against the one-by-one path is in {@link TraceBatchIngestDifferentialTest}.
 */
class TraceBatchIngestTest {

	private static final String DOMAIN = "domain-a";

	private static final String PRODUCER = "cedarling-fleet-1";

	private static final String KID = "cedarling-fleet-1-2026-01";

	private static final String INSTANCE = "cedarling-001";

	private static final String CHAIN_1 = "chain-01JABC9Z0K";

	private static final String CHAIN_2 = "chain-03JABC9Z0K";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** Runs a hook once or on every call before the atomic insert, to play a competing node. */
	private static final class CompetingStore extends InMemoryTraceStore {

		private volatile Runnable beforeBatchInsert;

		private volatile boolean everyAttempt;

		CompetingStore(TraceKeys keys) {
			super(keys);
		}

		@Override
		public synchronized void insertIngestBatch(List<TraceReceiptEntry> receipts, List<StoredTraceRecord> records)
				throws DuplicateEntryException, TraceStorageException {
			Runnable hook = beforeBatchInsert;
			if (hook != null) {
				if (!everyAttempt) {
					beforeBatchInsert = null;
				}
				hook.run();
			}
			super.insertIngestBatch(receipts, records);
		}

	}

	private CompetingStore store;

	/** The node under test. */
	private TraceTestHarness node;

	/** A second node over the same store, used as the competitor. */
	private TraceTestHarness competitor;

	private KeyPair key;

	@BeforeEach
	void setUp() {
		node = new TraceTestHarness(TraceTestCrypto.javaProvider(), keys -> {
			store = new CompetingStore(keys);
			return store;
		});
		competitor = new TraceTestHarness(TraceTestCrypto.javaProvider(), keys -> store);
		key = node.registerKey(DOMAIN, PRODUCER, KID);
		node.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN_1);
		node.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN_2);
	}

	private static String digestOf(String json) {
		try {
			return CanonicalHashes.sha256PrefixedOfJcs(MAPPER.readTree(json));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private List<String> chain(String chain, int count) {
		List<String> records = new ArrayList<>();
		String prev = TraceConstants.ZERO_HASH;
		for (int seq = 1; seq <= count; seq++) {
			String json = TraceRecordBuilder.authorizationDecision().producerInstanceId(INSTANCE)
					.producerChainId(chain).sequenceNumber(seq).prevRecordHash(prev).sign(key.getPrivate());
			records.add(json);
			prev = digestOf(json);
		}
		return records;
	}

	private static List<BatchElement> elements(List<String> jsons) {
		List<BatchElement> list = new ArrayList<>();
		for (int i = 0; i < jsons.size(); i++) {
			list.add(new BatchElement(i, jsons.get(i).getBytes(StandardCharsets.UTF_8)));
		}
		return list;
	}

	private List<BatchOutcome> submit(TraceTestHarness harness, List<String> jsons) {
		return harness.ingestionService().ingestBatch(elements(jsons), harness.ctx(DOMAIN));
	}

	private void assertDenseValidChain(int expectedReceipts) {
		ChainVerificationReport report = node.verifyDomain(DOMAIN);
		assertTrue(report.isOk(), () -> "chain: " + report);
		assertEquals(expectedReceipts, report.getHeadSequence());
		assertEquals(report.getHeadSequence(), report.getEntries().size(), "receipt sequence must be dense");
		Set<Long> seen = new HashSet<>();
		for (StoredTraceRecord record : store.snapshot()) {
			assertTrue(seen.add(record.getReceipt().getReceiptSequence()), "no receipt sequence used twice");
		}
	}

	// -- collision ----------------------------------------------------------------------------

	@Test
	void competingReceiptRowShiftsTheRangeAndTheRetrySucceeds() {
		List<String> batch = chain(CHAIN_1, 5);
		String foreign = chain(CHAIN_2, 1).get(0);
		store.beforeBatchInsert = () -> competitor.ingest(foreign, competitor.ctx(DOMAIN));

		List<BatchOutcome> outcomes = submit(node, batch);

		assertEquals(2, store.getIngestBatches(), "first attempt collides, second succeeds");
		for (int i = 0; i < 5; i++) {
			assertTrue(outcomes.get(i).isAccepted());
			assertFalse(outcomes.get(i).getResult().isIdempotentReplay());
			assertEquals(i + 2L, outcomes.get(i).getResult().getReceiptSequence(), "position 1 is the competitor's");
		}
		assertEquals(6, store.snapshot().size());
		assertDenseValidChain(6);
	}

	@Test
	void competingIdentityWithSameDigestBecomesReplay() {
		List<String> batch = chain(CHAIN_1, 5);
		store.beforeBatchInsert = () -> competitor.ingest(batch.get(2), competitor.ctx(DOMAIN));

		List<BatchOutcome> outcomes = submit(node, batch);

		assertTrue(outcomes.get(2).isAccepted());
		assertTrue(outcomes.get(2).getResult().isIdempotentReplay());
		assertEquals(1L, outcomes.get(2).getResult().getReceiptSequence());
		long expected = 2;
		for (int i : new int[] { 0, 1, 3, 4 }) {
			assertTrue(outcomes.get(i).isAccepted());
			assertFalse(outcomes.get(i).getResult().isIdempotentReplay());
			assertEquals(expected++, outcomes.get(i).getResult().getReceiptSequence());
		}
		assertEquals(5, store.snapshot().size(), "the competing record is stored once");
		assertDenseValidChain(5);
	}

	@Test
	void competingIdentityWithOtherDigestIsConflict() {
		List<String> batch = chain(CHAIN_1, 3);
		String rival = TraceRecordBuilder.fromJson(batch.get(1))
				.withField("9.9.9-rival", "trace", "policy", "policy_store_version").sign(key.getPrivate());
		store.beforeBatchInsert = () -> competitor.ingest(rival, competitor.ctx(DOMAIN));

		List<BatchOutcome> outcomes = submit(node, batch);

		assertFalse(outcomes.get(1).isAccepted());
		assertTrue(outcomes.get(1).getError() instanceof TraceConflictException);
		assertEquals(TraceErrorResponseType.RECORD_CONFLICT,
				((TraceConflictException) outcomes.get(1).getError()).getErrorId());
		assertTrue(outcomes.get(0).isAccepted());
		assertTrue(outcomes.get(2).isAccepted());
		assertEquals(3, store.snapshot().size());
		assertDenseValidChain(3);
	}

	@Test
	void inBatchReplayOfDroppedCandidateFollowsItsOutcome() {
		List<String> c = chain(CHAIN_1, 3);
		List<String> batch = List.of(c.get(0), c.get(1), c.get(1), c.get(2));
		store.beforeBatchInsert = () -> competitor.ingest(c.get(1), competitor.ctx(DOMAIN));

		List<BatchOutcome> outcomes = submit(node, batch);

		assertTrue(outcomes.get(1).getResult().isIdempotentReplay());
		assertTrue(outcomes.get(2).getResult().isIdempotentReplay());
		assertEquals(outcomes.get(1).getResult().getReceiptSequence(), outcomes.get(2).getResult().getReceiptSequence());
		assertEquals(3, store.snapshot().size());
		assertDenseValidChain(3);
	}

	@Test
	void collisionOnEveryAttemptExhaustsThenFallsBackToSinglePath() {
		node.traceConfiguration().setReceiptAllocationRetryLimit(2);
		List<String> batch = chain(CHAIN_1, 3);
		List<String> foreign = chain(CHAIN_2, 5);
		int[] next = { 0 };
		store.everyAttempt = true;
		store.beforeBatchInsert = () -> competitor.ingest(foreign.get(next[0]++), competitor.ctx(DOMAIN));

		List<BatchOutcome> outcomes = submit(node, batch);

		assertEquals(2, next[0], "one competing write per attempt, none after the fallback started");
		for (BatchOutcome outcome : outcomes) {
			assertTrue(outcome.isAccepted());
			assertFalse(outcome.getResult().isIdempotentReplay());
		}
		assertEquals(5, store.snapshot().size());
		assertDenseValidChain(5);
	}

	// -- fallback -----------------------------------------------------------------------------

	@Test
	void failedBatchInsertFallsBackAndGivesTheSinglePathOutcomes() {
		List<String> c = chain(CHAIN_1, 4);
		String tampered = TraceRecordBuilder.tamperAfterSigning(c.get(3), "9.9.9-tampered", "trace", "policy",
				"policy_store_version");
		List<String> batch = List.of(c.get(0), c.get(1), c.get(0), tampered, c.get(2));

		TraceTestHarness reference = new TraceTestHarness();
		reference.registerKey(DOMAIN, PRODUCER, KID, key);
		reference.registerChain(DOMAIN, PRODUCER, INSTANCE, CHAIN_1);
		List<String> expected = new ArrayList<>();
		for (String json : batch) {
			try {
				expected.add(String.valueOf(reference.ingest(json, reference.ctx(DOMAIN))));
			} catch (RuntimeException ex) {
				expected.add(ex.getClass().getSimpleName());
			}
		}

		store.failNextInsertIngestBatch();
		List<BatchOutcome> outcomes = submit(node, batch);

		assertEquals(1, store.getIngestBatches());
		assertEquals(batch.size(), outcomes.size());
		for (int i = 0; i < batch.size(); i++) {
			String actual = outcomes.get(i).isAccepted() ? String.valueOf(outcomes.get(i).getResult())
					: outcomes.get(i).getError().getClass().getSimpleName();
			assertEquals(expected.get(i), actual, "outcome of element " + i);
		}
		assertEquals(3, store.snapshot().size(), "no element is stored twice");
		assertDenseValidChain(3);
	}

	@Test
	void storeWithoutAtomicBatchesFallsBackAndReportsUnavailable() {
		store.setAtomicIngestSupported(false);
		assertFalse(node.ingestionService().isBatchIngestAvailable());

		List<BatchOutcome> outcomes = submit(node, chain(CHAIN_1, 3));

		for (BatchOutcome outcome : outcomes) {
			assertTrue(outcome.isAccepted());
		}
		assertEquals(3, store.snapshot().size());
		assertDenseValidChain(3);
	}

	@Test
	void batchIsAvailableOnAnAtomicStoreWithCrypto() {
		assertTrue(node.ingestionService().isBatchIngestAvailable());
	}

	@Test
	void batchRowsAreCommittedImmediately() {
		submit(node, chain(CHAIN_1, 3));

		for (TraceReceiptEntry receipt : store.receiptSnapshot()) {
			assertEquals(io.jans.lock.model.trace.entity.TraceReceiptState.COMMITTED, receipt.getReceiptStateEnum());
		}
	}

	// -- policy release reads -----------------------------------------------------------------

	/**
	 * {@code PolicyMismatchCheck.check} caches each release it reads, and never writes, so a batch
	 * whose records name an unserved release reads that release once. This is why the batch path
	 * has no per-batch policy memo of its own.
	 */
	@Test
	void policyReleaseOfAWholeBatchIsReadOnceAndNeverWritten() {
		node.setPolicyMismatchCheck(PolicyMismatchChecks.serving(
				new PolicyMismatchChecks.StaticPolicyDownloadService().archive(
						"https://example.org/policy-stores/payments", "1.2.3", "sha256:" + "f".repeat(64)),
				new io.jans.lock.model.config.AppConfiguration(), store));
		int reads = store.getPolicyReleaseReads();
		int writes = store.getPolicyReleaseWrites();

		List<BatchOutcome> outcomes = submit(node, chain(CHAIN_1, 30));

		for (BatchOutcome outcome : outcomes) {
			assertTrue(outcome.isAccepted());
			assertTrue(outcome.getResult().isPolicyMismatchFlag());
		}
		assertEquals(1, store.getPolicyReleaseReads() - reads);
		assertEquals(0, store.getPolicyReleaseWrites() - writes);
	}

	// -- concurrency --------------------------------------------------------------------------

	@Test
	void concurrentBatchesOnOneDomainStayDenseWithoutDuplicates() throws Exception {
		int perThread = 60;
		int batchSize = 10;
		List<List<String>> chains = new ArrayList<>();
		chains.add(chain(CHAIN_1, perThread));
		chains.add(chain(CHAIN_2, perThread));

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			List<Future<Integer>> futures = new ArrayList<>();
			for (List<String> chain : chains) {
				Callable<Integer> task = () -> {
					start.await();
					int accepted = 0;
					for (int from = 0; from < chain.size(); from += batchSize) {
						for (BatchOutcome outcome : submit(node, chain.subList(from, from + batchSize))) {
							if (outcome.isAccepted() && !outcome.getResult().isIdempotentReplay()) {
								accepted++;
							}
						}
					}
					return accepted;
				};
				futures.add(pool.submit(task));
			}
			start.countDown();
			for (Future<Integer> future : futures) {
				assertEquals(perThread, future.get(60, TimeUnit.SECONDS).intValue());
			}
		} finally {
			pool.shutdownNow();
		}

		assertEquals(2 * perThread, store.snapshot().size());
		assertDenseValidChain(2 * perThread);
	}

	// -- single submission as a batch of one --------------------------------------------------

	private AcceptanceResult submitSingle(String json) {
		return node.ingestionService().ingestAsBatch(
				new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), node.ctx(DOMAIN));
	}

	@Test
	void singleSubmissionAsBatchWritesOneTransactionAndCommittedReceipt() {
		List<String> c = chain(CHAIN_1, 2);

		AcceptanceResult first = submitSingle(c.get(0));
		AcceptanceResult second = submitSingle(c.get(1));
		AcceptanceResult replay = submitSingle(c.get(0));

		assertFalse(first.isIdempotentReplay());
		assertFalse(second.isIdempotentReplay());
		assertTrue(replay.isIdempotentReplay());
		assertEquals(1L, first.getReceiptSequence());
		assertEquals(2L, second.getReceiptSequence());
		assertEquals(1L, replay.getReceiptSequence());
		assertEquals(2, store.getIngestBatches(), "one transaction per new record, none for the replay");
		for (TraceReceiptEntry receipt : store.receiptSnapshot()) {
			assertEquals(io.jans.lock.model.trace.entity.TraceReceiptState.COMMITTED, receipt.getReceiptStateEnum());
		}
		assertDenseValidChain(2);
	}

	@Test
	void singleSubmissionAsBatchThrowsWhatTheSinglePathThrows() {
		String json = chain(CHAIN_1, 1).get(0);
		submitSingle(json);

		String conflicting = TraceRecordBuilder.fromJson(json)
				.withField("9.9.9-conflict", "trace", "policy", "policy_store_version").sign(key.getPrivate());
		assertThrows(TraceConflictException.class, () -> submitSingle(conflicting));

		String tampered = TraceRecordBuilder.tamperAfterSigning(chain(CHAIN_2, 1).get(0), "9.9.9-tampered", "trace",
				"policy", "policy_store_version");
		TraceValidationException rejected = assertThrows(TraceValidationException.class, () -> submitSingle(tampered));
		assertEquals(TraceErrorResponseType.INVALID_SIGNATURE, rejected.getErrorId());
		assertEquals(1, store.getIngestBatches(), "a rejected submission writes nothing");
	}

	@Test
	void singleSubmissionAsBatchFallsBackToSinglePathOnFailedInsert() {
		store.failNextInsertIngestBatch();

		AcceptanceResult result = submitSingle(chain(CHAIN_1, 1).get(0));

		assertFalse(result.isIdempotentReplay());
		assertEquals(1L, result.getReceiptSequence());
		assertEquals(1, store.snapshot().size());
		assertDenseValidChain(1);
	}

	// -- ingest pools -------------------------------------------------------------------------

	@Test
	void parallelVerificationKeepsInputOrderAndPerElementFailures() {
		node.traceConfiguration().setBatchVerifyThreads(4);
		List<String> c = chain(CHAIN_1, 6);
		List<String> batch = new ArrayList<>(c);
		batch.set(2, TraceRecordBuilder.tamperAfterSigning(c.get(2), "9.9.9-tampered", "trace", "policy",
				"policy_store_version"));

		List<BatchOutcome> outcomes;
		try {
			outcomes = submit(node, batch);
		} finally {
			node.drainExecutors();
		}

		assertFalse(outcomes.get(2).isAccepted());
		assertEquals(TraceErrorResponseType.INVALID_SIGNATURE,
				((TraceValidationException) outcomes.get(2).getError()).getErrorId());
		long expectedSeq = 1;
		for (int i = 0; i < batch.size(); i++) {
			if (i == 2) {
				continue;
			}
			assertTrue(outcomes.get(i).isAccepted(), "element " + i);
			assertEquals(expectedSeq++, outcomes.get(i).getResult().getReceiptSequence(), "element " + i);
		}
		assertDenseValidChain(5);
	}

	@Test
	void postCommitCorrelationRunsOnItsPoolAfterTheOutcomesAreReturned() throws Exception {
		node.traceConfiguration().setPostCommitCorrelationThreads(1);
		List<String> c = chain(CHAIN_1, 2);
		AcceptanceResult successor = node.ingest(c.get(1), node.ctx(DOMAIN));
		assertTrue(successor.isCoverageGapFlag());
		RecordIdentity successorId = new RecordIdentity(DOMAIN, PRODUCER, successor.getRecordId());

		CountDownLatch release = new CountDownLatch(1);
		try {
			node.executors().correlationExecutor().execute(() -> {
				try {
					release.await();
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			});

			List<BatchOutcome> outcomes = submit(node, c.subList(0, 1));

			assertTrue(outcomes.get(0).isAccepted());
			assertTrue(store.findRecord(successorId).get().getFlags().isCoverageGap(),
					"the successor is corrected only after the response");
		} finally {
			release.countDown();
			node.drainExecutors();
		}
		assertFalse(store.findRecord(successorId).get().getFlags().isCoverageGap());
	}

}
