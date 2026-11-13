/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;

/**
 * Tests for {@link TraceReceiptChain#runWithRange}: contiguous range reservation, hash chaining,
 * collision retry and re-plan, exhaustion, concurrency and per-domain independence.
 */
class TraceReceiptChainBatchTest extends BaseLockServiceTest {

	private static final String DOMAIN = "domain-1";

	private static final String OTHER_DOMAIN = "domain-2";

	private static final String PRODUCER = "producer-1";

	private InMemoryTraceStore store;

	private TraceConfiguration traceConfiguration;

	private AppConfiguration appConfiguration;

	private ExecutorService executor;

	@BeforeEach
	void setUp() {
		store = new InMemoryTraceStore();
		traceConfiguration = new TraceConfiguration();
		appConfiguration = mock(AppConfiguration.class);
		when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);
	}

	@AfterEach
	void tearDown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	private TraceReceiptChain newChain(InMemoryTraceStore backingStore) {
		TraceReceiptChain chain = new TraceReceiptChain();
		setField(chain, "log", LoggerFactory.getLogger(TraceReceiptChain.class));
		setField(chain, "store", backingStore);
		setField(chain, "appConfiguration", appConfiguration);
		setField(chain, "cryptoProvider", TraceTestCrypto.javaProvider());
		return chain;
	}

	private static String digest(String seed) {
		StringBuilder sb = new StringBuilder("sha256:");
		char c = Character.forDigit(Math.abs(seed.hashCode()) % 16, 16);
		for (int i = 0; i < 64; i++) {
			sb.append(c);
		}
		return sb.toString();
	}

	private static ReceiptBatchItem item(String recordId) {
		return new ReceiptBatchItem(PRODUCER, recordId, recordId + "-key", digest(recordId), 1_000L);
	}

	private static List<ReceiptBatchItem> items(String prefix, int count) {
		List<ReceiptBatchItem> list = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			list.add(item(prefix + i));
		}
		return list;
	}

	/** The pipeline's commit step in miniature: receipts and matching records in one atomic write. */
	private static TraceReceiptChain.RangeAction storeInto(InMemoryTraceStore target, String domainId) {
		return run -> {
			List<StoredTraceRecord> records = new ArrayList<>();
			for (int i = 0; i < run.getItems().size(); i++) {
				ReceiptBatchItem item = run.getItems().get(i);
				ReceiptRun.Position position = run.getPositions().get(i);
				records.add(StoredTraceRecordFactory.builder().domainId(domainId).producerId(item.getProducerId())
						.recordId(item.getRecordId()).contentDigest(item.getContentDigest())
						.receiptSequence(position.getSeq()).prevReceiptHash(position.getPrevReceiptHash())
						.receiptHash(position.getReceiptHash()).build());
			}
			target.insertIngestBatch(run.getRows(), records);
		};
	}

	private void claimAndStore(TraceReceiptChain chain, InMemoryTraceStore target, ReceiptBatchItem item)
			throws DuplicateEntryException {
		ReceiptClaim claim = chain.claim(DOMAIN, item.getProducerId(), item.getRecordId(), item.getRecordKey(),
				item.getContentDigest(), item.getReceivedAtMs(), "node-single");
		target.insertRecord(StoredTraceRecordFactory.builder().domainId(DOMAIN).producerId(item.getProducerId())
				.recordId(item.getRecordId()).contentDigest(item.getContentDigest())
				.receiptSequence(claim.getSeq()).build());
		chain.commit(DOMAIN, claim.getSeq());
	}

	private void assertChainOk(TraceReceiptChain chain, String domainId, long expectedHead) {
		ChainVerificationReport report = chain.verifyDomain(domainId, 100_000);
		assertTrue(report.isOk(), "chain must verify: " + report);
		assertEquals(expectedHead, report.getHeadSequence());
		assertEquals(expectedHead, report.getEntries().size());
	}

	@Test
	void testRunWithRange_EmptyDomain_ChainedFromZeroHashAndEqualsSequentialClaims() throws Exception {
		TraceReceiptChain batchChain = newChain(store);
		List<ReceiptBatchItem> batch = items("rec-", 5);

		ReceiptRun run = batchChain.runWithRange(DOMAIN, batch, "node-batch", storeInto(store, DOMAIN));

		assertEquals(0, run.getAttempt());
		assertEquals(5, run.getRows().size());
		assertEquals(batch, run.getItems());
		for (int i = 0; i < 5; i++) {
			ReceiptRun.Position position = run.getPositions().get(i);
			TraceReceiptEntry row = run.getRows().get(i);
			assertEquals(i + 1L, position.getSeq());
			assertEquals(position.getSeq(), row.getReceiptSeq());
			assertEquals(TraceReceiptState.COMMITTED, row.getReceiptStateEnum());
			assertEquals("node-batch", row.getNodeId());
			assertEquals(i == 0 ? TraceConstants.ZERO_HASH : run.getPositions().get(i - 1).getReceiptHash(),
					position.getPrevReceiptHash());
			TraceReceiptEntry stored = store.findReceipt(DOMAIN, i + 1L).orElseThrow();
			assertEquals(position.getReceiptHash(), stored.getReceiptHash());
			assertEquals(TraceReceiptState.COMMITTED, stored.getReceiptStateEnum());
		}
		assertChainOk(batchChain, DOMAIN, 5);

		InMemoryTraceStore sequentialStore = new InMemoryTraceStore();
		TraceReceiptChain sequentialChain = newChain(sequentialStore);
		for (int i = 0; i < 5; i++) {
			ReceiptBatchItem item = batch.get(i);
			ReceiptClaim claim = sequentialChain.claim(DOMAIN, item.getProducerId(), item.getRecordId(),
					item.getRecordKey(), item.getContentDigest(), item.getReceivedAtMs(), "node-batch");
			sequentialChain.commit(DOMAIN, claim.getSeq());
			ReceiptRun.Position position = run.getPositions().get(i);
			assertEquals(claim.getSeq(), position.getSeq());
			assertEquals(claim.getPrevReceiptHash(), position.getPrevReceiptHash());
			assertEquals(claim.getReceiptHash(), position.getReceiptHash());
		}
	}

	@Test
	void testRunWithRange_ExistingHead_ContinuesAtHeadPlusOne() throws Exception {
		TraceReceiptChain chain = newChain(store);
		claimAndStore(chain, store, item("single-1"));
		claimAndStore(chain, store, item("single-2"));
		ReceiptHead head = store.findReceiptHead(DOMAIN).orElseThrow();

		ReceiptRun run = chain.runWithRange(DOMAIN, items("batch-", 3), "node-batch", storeInto(store, DOMAIN));

		assertEquals(3L, run.getPositions().get(0).getSeq());
		assertEquals(head.getReceiptHash(), run.getPositions().get(0).getPrevReceiptHash());
		assertEquals(5L, run.getPositions().get(2).getSeq());
		assertChainOk(chain, DOMAIN, 5);
	}

	@Test
	void testRunWithRange_MixedSingleAndBatchRows_Verifies() throws Exception {
		TraceReceiptChain chain = newChain(store);
		claimAndStore(chain, store, item("single-1"));
		chain.runWithRange(DOMAIN, items("batch-a-", 2), "node-batch", storeInto(store, DOMAIN));
		claimAndStore(chain, store, item("single-2"));
		chain.runWithRange(DOMAIN, items("batch-b-", 3), "node-batch", storeInto(store, DOMAIN));
		claimAndStore(chain, store, item("single-3"));

		assertChainOk(chain, DOMAIN, 8);
	}

	@Test
	void testRunWithRange_CompetingRowAfterHeadRead_RetriesFromFreshHead() throws Exception {
		CompetingStore competing = new CompetingStore();
		TraceReceiptChain chain = newChain(competing);
		TraceReceiptChain otherNode = newChain(competing);
		competing.competitor = () -> {
			try {
				claimAndStore(otherNode, competing, item("competitor"));
			} catch (DuplicateEntryException e) {
				throw new IllegalStateException(e);
			}
		};
		AtomicInteger attempts = new AtomicInteger();
		TraceReceiptChain.RangeAction inner = storeInto(competing, DOMAIN);

		ReceiptRun run = chain.runWithRange(DOMAIN, items("rec-", 3), "node-batch", r -> {
			attempts.incrementAndGet();
			inner.commit(r);
		});

		assertEquals(2, attempts.get());
		assertEquals(1, run.getAttempt());
		assertEquals(2L, run.getPositions().get(0).getSeq());
		assertEquals(4L, run.getPositions().get(2).getSeq());
		assertEquals(competing.findReceipt(DOMAIN, 1L).orElseThrow().getReceiptHash(),
				run.getPositions().get(0).getPrevReceiptHash());
		assertChainOk(chain, DOMAIN, 4);
	}

	@Test
	void testRunWithRange_AlwaysDuplicate_ExhaustsAfterRetryLimit() {
		traceConfiguration.setReceiptAllocationRetryLimit(3);
		TraceReceiptChain chain = newChain(store);
		AtomicInteger attempts = new AtomicInteger();
		AtomicInteger replans = new AtomicInteger();

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", run -> {
					attempts.incrementAndGet();
					throw new DuplicateEntryException("forced");
				}, () -> {
					replans.incrementAndGet();
					return items("rec-", 2);
				}));

		assertEquals("receipt_allocation_exhausted", ex.getReason());
		assertEquals(3, attempts.get());
		assertEquals(2, replans.get(), "no re-plan after the last attempt");
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	@Test
	void testRunWithRange_ReplanReturnsFewerItems_NextAttemptUsesThem() throws Exception {
		TraceReceiptChain chain = newChain(store);
		List<ReceiptBatchItem> original = items("rec-", 4);
		List<ReceiptBatchItem> reduced = original.subList(0, 2);
		AtomicInteger attempts = new AtomicInteger();
		TraceReceiptChain.RangeAction inner = storeInto(store, DOMAIN);

		ReceiptRun run = chain.runWithRange(DOMAIN, original, "node-batch", r -> {
			if (attempts.getAndIncrement() == 0) {
				assertEquals(4, r.getRows().size());
				throw new DuplicateEntryException("rec-3 is now a replay");
			}
			inner.commit(r);
		}, () -> reduced);

		assertEquals(reduced, run.getItems());
		assertEquals(2, run.getRows().size());
		assertEquals(1L, run.getPositions().get(0).getSeq());
		assertChainOk(chain, DOMAIN, 2);
	}

	@Test
	void testRunWithRange_ReplanReturnsEmpty_NoRowsWritten() {
		TraceReceiptChain chain = newChain(store);
		AtomicInteger attempts = new AtomicInteger();

		ReceiptRun run = chain.runWithRange(DOMAIN, items("rec-", 3), "node-batch", r -> {
			attempts.incrementAndGet();
			throw new DuplicateEntryException("all replays");
		}, () -> new ArrayList<ReceiptBatchItem>());

		assertTrue(run.isEmpty());
		assertEquals(1, attempts.get());
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	@Test
	void testRunWithRange_NoItems_ActionNotCalled() {
		TraceReceiptChain chain = newChain(store);

		ReceiptRun run = chain.runWithRange(DOMAIN, new ArrayList<ReceiptBatchItem>(), "node-batch", r -> {
			throw new AssertionError("must not be called");
		});

		assertTrue(run.isEmpty());
	}

	@Test
	void testRunWithRange_OtherExceptionPropagates_NoRowsAndLockReleased() throws Exception {
		TraceReceiptChain chain = newChain(store);
		IllegalStateException boom = new IllegalStateException("boom");

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", r -> {
					throw boom;
				}));
		assertSame(boom, thrown);

		store.failNextInsertIngestBatch();
		TraceStorageException storageFailure = assertThrows(TraceStorageException.class,
				() -> chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", storeInto(store, DOMAIN)));
		assertEquals("test_fail_next_insert_ingest_batch", storageFailure.getReason());
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());

		ReceiptRun run = chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", storeInto(store, DOMAIN));
		assertEquals(1L, run.getPositions().get(0).getSeq());
	}

	@Test
	void testRunWithRange_ConcurrentBatchesSameDomain_ChainDense() throws Exception {
		traceConfiguration.setReceiptAllocationRetryLimit(1_000);
		TraceReceiptChain nodeA = newChain(store);
		TraceReceiptChain nodeB = newChain(store);
		int threads = 8;
		int batchesPerThread = 20;
		int batchSize = 3;
		executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();
		for (int t = 0; t < threads; t++) {
			final int thread = t;
			final TraceReceiptChain chain = thread % 2 == 0 ? nodeA : nodeB;
			futures.add(executor.submit(() -> {
				for (int b = 0; b < batchesPerThread; b++) {
					chain.runWithRange(DOMAIN, items("t" + thread + "-b" + b + "-", batchSize), "node-" + thread % 2,
							storeInto(store, DOMAIN));
				}
				return null;
			}));
		}
		for (Future<?> future : futures) {
			future.get(60, TimeUnit.SECONDS);
		}

		long expected = (long) threads * batchesPerThread * batchSize;
		assertChainOk(nodeA, DOMAIN, expected);
		assertEquals(expected, store.countRecords(DOMAIN));
	}

	@Test
	void testRunWithRange_TwoDomains_RunInParallel() throws Exception {
		TraceReceiptChain chain = newChain(store);
		CountDownLatch inA = new CountDownLatch(1);
		CountDownLatch inB = new CountDownLatch(1);
		executor = Executors.newFixedThreadPool(2);

		Future<ReceiptRun> a = executor.submit(() -> chain.runWithRange(DOMAIN, items("a-", 1), "node", run -> {
			inA.countDown();
			awaitOrFail(inB);
			storeInto(store, DOMAIN).commit(run);
		}));
		Future<ReceiptRun> b = executor.submit(() -> chain.runWithRange(OTHER_DOMAIN, items("b-", 1), "node", run -> {
			inB.countDown();
			awaitOrFail(inA);
			storeInto(store, OTHER_DOMAIN).commit(run);
		}));

		assertEquals(1L, a.get(10, TimeUnit.SECONDS).getPositions().get(0).getSeq());
		assertEquals(1L, b.get(10, TimeUnit.SECONDS).getPositions().get(0).getSeq());
	}

	private static void awaitOrFail(CountDownLatch latch) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError("the other domain's batch never started: domains are serialized");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	/** Runs {@code competitor} once, right after the first head read, before the caller can insert. */
	private static final class CompetingStore extends InMemoryTraceStore {

		private volatile Runnable competitor;

		private final AtomicInteger headReads = new AtomicInteger();

		@Override
		public Optional<ReceiptHead> findReceiptHead(String domainId) {
			Optional<ReceiptHead> head = super.findReceiptHead(domainId);
			Runnable hook = competitor;
			if (hook != null && headReads.incrementAndGet() == 1) {
				competitor = null;
				hook.run();
			}
			return head;
		}

	}

}
