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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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
import io.jans.lock.service.trace.receipt.TraceReceiptChain.Sleeper;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;

/**
 * Tests for {@link TraceReceiptChain#runWithRange}: contiguous range reservation, hash chaining,
 * collision retry and re-plan, exhaustion, concurrency and per-domain independence, the separate
 * range retry limit and the jittered backoff taken with the domain lock released.
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
		traceConfiguration.setRangeAllocationRetryLimit(3);
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
		traceConfiguration.setRangeAllocationRetryLimit(1_000);
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

	// -- range retry limit and backoff -----------------------------------------------------------

	/** Backoff base for the tests that need a sleep, pinned so they do not depend on the default. */
	private static final int BACKOFF_BASE_MS = 5;

	/** Records every requested sleep instead of sleeping. */
	private static final class RecordingSleeper implements Sleeper {

		private final List<Long> sleeps = Collections.synchronizedList(new ArrayList<>());

		@Override
		public void sleep(long millis) {
			sleeps.add(millis);
		}

	}

	private static RecordingSleeper recordingSleeper(TraceReceiptChain chain) {
		RecordingSleeper sleeper = new RecordingSleeper();
		setField(chain, "sleeper", sleeper);
		return sleeper;
	}

	@SuppressWarnings("unchecked")
	private static ReentrantLock domainLock(TraceReceiptChain chain, String domainId) {
		try {
			java.lang.reflect.Field field = TraceReceiptChain.class.getDeclaredField("domainLocks");
			field.setAccessible(true);
			return ((Map<String, ReentrantLock>) field.get(chain)).get(domainId);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Test
	void testRunWithRange_OwnRetryLimit_ClaimKeepsReceiptAllocationRetryLimit() {
		traceConfiguration.setReceiptAllocationRetryLimit(3);
		traceConfiguration.setRangeAllocationRetryLimit(5);
		traceConfiguration.setRangeAllocationBackoffMs(BACKOFF_BASE_MS);
		AlwaysDuplicateReceiptStore duplicates = new AlwaysDuplicateReceiptStore();
		TraceReceiptChain chain = newChain(duplicates);
		RecordingSleeper sleeper = recordingSleeper(chain);
		AtomicInteger attempts = new AtomicInteger();

		TraceStorageException rangeFailure = assertThrows(TraceStorageException.class,
				() -> chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", run -> {
					attempts.incrementAndGet();
					throw new DuplicateEntryException("forced");
				}));
		assertEquals("receipt_allocation_exhausted", rangeFailure.getReason());
		assertEquals(5, attempts.get());
		assertEquals(4, sleeper.sleeps.size(), "one backoff per retry, none after the last attempt");

		TraceStorageException claimFailure = assertThrows(TraceStorageException.class,
				() -> chain.claim(DOMAIN, PRODUCER, "single-1", "single-1-key", digest("single-1"), 1_000L, "node-single"));
		assertEquals("receipt_allocation_exhausted", claimFailure.getReason());
		assertEquals(3, duplicates.receiptInserts.get());
		assertEquals(4, sleeper.sleeps.size(), "claim never backs off");
	}

	@Test
	void testRunWithRange_RangeRetryLimitNotPositive_FallsBackToDefault() {
		traceConfiguration.setRangeAllocationRetryLimit(0);
		traceConfiguration.setRangeAllocationBackoffMs(0);
		TraceReceiptChain chain = newChain(store);
		AtomicInteger attempts = new AtomicInteger();

		assertThrows(TraceStorageException.class, () -> chain.runWithRange(DOMAIN, items("rec-", 1), "node-batch", run -> {
			attempts.incrementAndGet();
			throw new DuplicateEntryException("forced");
		}));

		assertEquals(TraceConfiguration.DEFAULT_RANGE_ALLOCATION_RETRY_LIMIT, attempts.get());
	}

	@Test
	void testRunWithRange_BackoffSequence_WithinExponentialWindowAndCap() {
		int base = 7;
		int max = 100;
		int limit = 12;
		traceConfiguration.setRangeAllocationRetryLimit(limit);
		traceConfiguration.setRangeAllocationBackoffMs(base);
		traceConfiguration.setRangeAllocationMaxBackoffMs(max);
		TraceReceiptChain chain = newChain(store);
		RecordingSleeper sleeper = recordingSleeper(chain);

		for (int round = 0; round < 50; round++) {
			sleeper.sleeps.clear();
			assertThrows(TraceStorageException.class,
					() -> chain.runWithRange(DOMAIN, items("rec-", 1), "node-batch", run -> {
						throw new DuplicateEntryException("forced");
					}));

			assertEquals(limit - 1, sleeper.sleeps.size());
			for (int k = 1; k <= sleeper.sleeps.size(); k++) {
				long window = Math.min((long) base << (k - 1), max);
				long slept = sleeper.sleeps.get(k - 1);
				assertTrue(slept >= 0 && slept <= window, "retry " + k + " slept " + slept + " outside [0, " + window + "]");
			}
		}
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	@Test
	void testBackoffMs_BoundsAndNoOverflow() {
		boolean sawZero = false;
		boolean sawTop = false;
		for (int i = 0; i < 2_000; i++) {
			long value = TraceReceiptChain.backoffMs(1, 1, 250);
			assertTrue(value == 0 || value == 1, "retry 1 with base 1 is 0 or 1: " + value);
			sawZero |= value == 0;
			sawTop |= value == 1;
			long capped = TraceReceiptChain.backoffMs(40, Integer.MAX_VALUE, 250);
			assertTrue(capped >= 0 && capped <= 250, "capped: " + capped);
			long large = TraceReceiptChain.backoffMs(31, 5, Integer.MAX_VALUE);
			assertTrue(large >= 0 && large <= Integer.MAX_VALUE, "large: " + large);
		}
		assertTrue(sawZero && sawTop, "both ends of the window are drawn");
	}

	@Test
	void testRunWithRange_DefaultBackoff_TotalWaitBoundUnderEightSeconds() {
		int base = TraceConfiguration.DEFAULT_RANGE_ALLOCATION_BACKOFF_MS;
		int max = TraceConfiguration.DEFAULT_RANGE_ALLOCATION_MAX_BACKOFF_MS;
		long total = 0;
		for (int k = 1; k < TraceConfiguration.DEFAULT_RANGE_ALLOCATION_RETRY_LIMIT; k++) {
			total += Math.min((long) base << (k - 1), max);
		}
		assertEquals(6565L, total);
		assertTrue(total < 8_000L);
	}

	@Test
	void testRunWithRange_BackoffBaseZero_NoSleepAndLockKept() {
		traceConfiguration.setRangeAllocationRetryLimit(4);
		traceConfiguration.setRangeAllocationBackoffMs(0);
		TraceReceiptChain chain = newChain(store);
		RecordingSleeper sleeper = recordingSleeper(chain);
		AtomicInteger attempts = new AtomicInteger();
		AtomicBoolean lockHeldInReplan = new AtomicBoolean(true);

		assertThrows(TraceStorageException.class, () -> chain.runWithRange(DOMAIN, items("rec-", 1), "node-batch", run -> {
			attempts.incrementAndGet();
			throw new DuplicateEntryException("forced");
		}, () -> {
			lockHeldInReplan.compareAndSet(true, domainLock(chain, DOMAIN).isHeldByCurrentThread());
			return items("rec-", 1);
		}));

		assertEquals(4, attempts.get());
		assertTrue(sleeper.sleeps.isEmpty(), "base 0 disables the sleep");
		assertTrue(lockHeldInReplan.get());
	}

	@Test
	void testRunWithRange_Backoff_UnlockSleepLockReplanHeadRead() throws Exception {
		traceConfiguration.setRangeAllocationRetryLimit(3);
		traceConfiguration.setRangeAllocationBackoffMs(BACKOFF_BASE_MS);
		List<String> events = Collections.synchronizedList(new ArrayList<>());
		InMemoryTraceStore recording = new InMemoryTraceStore() {
			@Override
			public Optional<ReceiptHead> findReceiptHead(String domainId) {
				events.add("head");
				return super.findReceiptHead(domainId);
			}
		};
		TraceReceiptChain chain = newChain(recording);
		setField(chain, "sleeper", (Sleeper) millis -> events
				.add("sleep:lockHeld=" + domainLock(chain, DOMAIN).isHeldByCurrentThread()));
		AtomicInteger attempts = new AtomicInteger();
		TraceReceiptChain.RangeAction inner = storeInto(recording, DOMAIN);

		ReceiptRun run = chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", r -> {
			events.add("commit");
			if (attempts.getAndIncrement() == 0) {
				throw new DuplicateEntryException("forced");
			}
			inner.commit(r);
		}, () -> {
			events.add("replan:lockHeld=" + domainLock(chain, DOMAIN).isHeldByCurrentThread());
			return items("rec-", 2);
		});

		assertEquals(1, run.getAttempt());
		assertEquals(List.of("head", "commit", "sleep:lockHeld=false", "replan:lockHeld=true", "head", "commit"),
				events);
		assertFalse(domainLock(chain, DOMAIN).isLocked(), "lock released after the run");
	}

	@Test
	void testRunWithRange_LockReleasedDuringBackoff_OtherThreadCommitsThenRetryUsesFreshHead() throws Exception {
		traceConfiguration.setRangeAllocationBackoffMs(BACKOFF_BASE_MS);
		TraceReceiptChain chain = newChain(store);
		CountDownLatch inSleep = new CountDownLatch(1);
		CountDownLatch resume = new CountDownLatch(1);
		setField(chain, "sleeper", (Sleeper) millis -> {
			inSleep.countDown();
			if (!resume.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("never resumed");
			}
		});
		AtomicInteger attempts = new AtomicInteger();
		TraceReceiptChain.RangeAction inner = storeInto(store, DOMAIN);
		executor = Executors.newFixedThreadPool(2);

		Future<ReceiptRun> first = executor.submit(() -> chain.runWithRange(DOMAIN, items("first-", 2), "node-batch", r -> {
			if (attempts.getAndIncrement() == 0) {
				throw new DuplicateEntryException("lost the position");
			}
			inner.commit(r);
		}, () -> items("first-", 2)));
		assertTrue(inSleep.await(10, TimeUnit.SECONDS), "first run reached its backoff");

		Future<ReceiptRun> second = executor
				.submit(() -> chain.runWithRange(DOMAIN, items("second-", 2), "node-batch", storeInto(store, DOMAIN)));
		ReceiptRun secondRun = second.get(10, TimeUnit.SECONDS);
		assertEquals(1L, secondRun.getPositions().get(0).getSeq(), "the same domain lock was free during the sleep");
		assertFalse(first.isDone());

		resume.countDown();
		ReceiptRun firstRun = first.get(10, TimeUnit.SECONDS);

		assertEquals(1, firstRun.getAttempt());
		assertEquals(3L, firstRun.getPositions().get(0).getSeq());
		assertEquals(secondRun.getPositions().get(1).getReceiptHash(), firstRun.getPositions().get(0).getPrevReceiptHash());
		assertChainOk(chain, DOMAIN, 4);
	}

	@Test
	void testRunWithRange_InterruptedDuringBackoff_FailsWithFlagSetAndNoRows() throws Exception {
		traceConfiguration.setRangeAllocationBackoffMs(BACKOFF_BASE_MS);
		TraceReceiptChain chain = newChain(store);
		setField(chain, "sleeper", (Sleeper) millis -> {
			throw new InterruptedException("stop");
		});
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

		assertEquals("receipt_allocation_interrupted", ex.getReason());
		assertTrue(Thread.interrupted(), "interrupt flag restored (and cleared here)");
		assertEquals(1, attempts.get());
		assertEquals(0, replans.get());
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
		assertFalse(domainLock(chain, DOMAIN).isLocked(), "lock not left held");

		ReceiptRun run = chain.runWithRange(DOMAIN, items("rec-", 2), "node-batch", storeInto(store, DOMAIN));
		assertEquals(1L, run.getPositions().get(0).getSeq());
	}

	@Test
	void testRunWithRange_InterruptFlagSetBeforeRealSleep_FailsInterrupted() {
		traceConfiguration.setRangeAllocationBackoffMs(BACKOFF_BASE_MS);
		TraceReceiptChain chain = newChain(store);

		TraceStorageException ex = assertThrows(TraceStorageException.class,
				() -> chain.runWithRange(DOMAIN, items("rec-", 1), "node-batch", run -> {
					Thread.currentThread().interrupt();
					throw new DuplicateEntryException("forced");
				}));

		assertEquals("receipt_allocation_interrupted", ex.getReason());
		assertTrue(Thread.interrupted());
		assertFalse(store.findReceiptHead(DOMAIN).isPresent());
	}

	/** Rejects every receipt claim as a duplicate and counts the attempts. */
	private static final class AlwaysDuplicateReceiptStore extends InMemoryTraceStore {

		private final AtomicInteger receiptInserts = new AtomicInteger();

		@Override
		public void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException {
			receiptInserts.incrementAndGet();
			throw new DuplicateEntryException("forced");
		}

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
