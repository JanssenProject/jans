/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.store.TraceStore;

/**
 * {@link TimingTraceStore} which also counts the writes that matter for the batch path: the
 * {@code insertIngestBatch} calls and how many of them the store rejected as a duplicate (one such
 * rejection is one retry of {@code runWithRange}), the single-path {@code insertReceipt} claims that
 * lost a position, and the {@code insertRecord} calls (the fallback and the single path use them,
 * the batch path never does). Fault hooks: fail the next {@code insertIngestBatch} with an exception
 * of the test's choice instead of calling the store, alter the rows of the next
 * {@code insertIngestBatch} before the store sees them (so the database itself rejects the
 * transaction), run something before the next {@code findReceiptHead} (another node writing between
 * the batch's pre-read and its head read), and observe every {@code insertIngestBatch} failure
 * before the ingest service reacts to it.
 *
 * <p>Retry profile: per thread, the duplicate rejections since the last successful
 * {@code insertIngestBatch} are counted; a success records "committed after k retries", and
 * {@link #endSubmission()} (called by the driver after each submission) records a run of rejections
 * that never ended in a success as "abandoned after k retries", i.e. a range that used up its
 * attempts and fell back to the per-record path.
 *
 * @author Yuriy Movchan
 */
public class InstrumentedTraceStore extends TimingTraceStore {

	private final AtomicInteger batchCalls = new AtomicInteger();

	private final AtomicInteger batchDuplicates = new AtomicInteger();

	private final AtomicInteger batchFailures = new AtomicInteger();

	private final AtomicInteger receiptDuplicates = new AtomicInteger();

	private final AtomicInteger recordInserts = new AtomicInteger();

	private final ThreadLocal<int[]> retryStreak = ThreadLocal.withInitial(() -> new int[1]);

	private final Map<Integer, AtomicInteger> committedAfterRetries = new ConcurrentHashMap<>();

	private final Map<Integer, AtomicInteger> abandonedAfterRetries = new ConcurrentHashMap<>();

	private final AtomicReference<Supplier<RuntimeException>> nextBatchFailure = new AtomicReference<>();

	private final AtomicReference<BiConsumer<List<TraceReceiptEntry>, List<StoredTraceRecord>>> nextBatchEdit = new AtomicReference<>();

	private final AtomicReference<Runnable> nextHeadReadHook = new AtomicReference<>();

	private volatile Consumer<Exception> batchFailureObserver;

	private volatile Exception lastBatchFailure;

	public InstrumentedTraceStore(TraceStore delegate) {
		super(delegate);
	}

	/** The next {@code insertIngestBatch} throws {@code failure} instead of writing anything. */
	public void failNextIngestBatch(Supplier<RuntimeException> failure) {
		nextBatchFailure.set(failure);
	}

	/** The next {@code insertIngestBatch} hands its rows to {@code edit} first; the edited rows go to the store. */
	public void editNextIngestBatch(BiConsumer<List<TraceReceiptEntry>, List<StoredTraceRecord>> edit) {
		nextBatchEdit.set(edit);
	}

	/** {@code hook} runs before the next {@code findReceiptHead}, in the thread that reads the head. */
	public void beforeNextHeadRead(Runnable hook) {
		nextHeadReadHook.set(hook);
	}

	/** {@code observer} sees every exception {@code insertIngestBatch} throws, before it propagates. */
	public void observeIngestBatchFailures(Consumer<Exception> observer) {
		this.batchFailureObserver = observer;
	}

	/** @return the last exception {@code insertIngestBatch} threw, or {@code null} */
	public Exception lastBatchFailure() {
		return lastBatchFailure;
	}

	public int batchCalls() {
		return batchCalls.get();
	}

	/** @return {@code insertIngestBatch} calls the store rejected with {@link DuplicateEntryException} */
	public int batchDuplicates() {
		return batchDuplicates.get();
	}

	/** @return {@code insertIngestBatch} calls that failed for another reason */
	public int batchFailures() {
		return batchFailures.get();
	}

	/** @return {@code insertReceipt} claims rejected as duplicate (single-path position contention) */
	public int receiptDuplicates() {
		return receiptDuplicates.get();
	}

	public int recordInserts() {
		return recordInserts.get();
	}

	public void resetCounters() {
		batchCalls.set(0);
		batchDuplicates.set(0);
		batchFailures.set(0);
		receiptDuplicates.set(0);
		recordInserts.set(0);
		committedAfterRetries.clear();
		abandonedAfterRetries.clear();
	}

	/** Ends the calling thread's submission: a pending run of rejections counts as abandoned. */
	public void endSubmission() {
		int[] streak = retryStreak.get();
		if (streak[0] > 0) {
			abandonedAfterRetries.computeIfAbsent(streak[0], k -> new AtomicInteger()).incrementAndGet();
			streak[0] = 0;
		}
	}

	/** @return committed batches by the number of duplicate rejections before their commit */
	public Map<Integer, Integer> committedAfterRetries() {
		return snapshot(committedAfterRetries);
	}

	/** @return ranges given up (fallback) by the number of duplicate rejections they collected */
	public Map<Integer, Integer> abandonedAfterRetries() {
		return snapshot(abandonedAfterRetries);
	}

	private static Map<Integer, Integer> snapshot(Map<Integer, AtomicInteger> counts) {
		Map<Integer, Integer> copy = new TreeMap<>();
		counts.forEach((k, v) -> copy.put(k, v.get()));
		return copy;
	}

	@Override
	public void insertIngestBatch(List<TraceReceiptEntry> receipts, List<StoredTraceRecord> records)
			throws DuplicateEntryException, TraceStorageException {
		batchCalls.incrementAndGet();
		Supplier<RuntimeException> failure = nextBatchFailure.getAndSet(null);
		if (failure != null) {
			batchFailures.incrementAndGet();
			throw observed(failure.get());
		}
		BiConsumer<List<TraceReceiptEntry>, List<StoredTraceRecord>> edit = nextBatchEdit.getAndSet(null);
		if (edit != null) {
			edit.accept(receipts, records);
		}
		try {
			super.insertIngestBatch(receipts, records);
			int[] streak = retryStreak.get();
			committedAfterRetries.computeIfAbsent(streak[0], k -> new AtomicInteger()).incrementAndGet();
			streak[0] = 0;
		} catch (DuplicateEntryException ex) {
			batchDuplicates.incrementAndGet();
			retryStreak.get()[0]++;
			throw observed(ex);
		} catch (RuntimeException ex) {
			batchFailures.incrementAndGet();
			throw observed(ex);
		}
	}

	private <E extends Exception> E observed(E failure) {
		lastBatchFailure = failure;
		Consumer<Exception> observer = batchFailureObserver;
		if (observer != null) {
			observer.accept(failure);
		}
		return failure;
	}

	@Override
	public Optional<ReceiptHead> findReceiptHead(String domainId) {
		Runnable hook = nextHeadReadHook.getAndSet(null);
		if (hook != null) {
			hook.run();
		}
		return super.findReceiptHead(domainId);
	}

	@Override
	public void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException, TraceStorageException {
		try {
			super.insertReceipt(row);
		} catch (DuplicateEntryException ex) {
			receiptDuplicates.incrementAndGet();
			throw ex;
		}
	}

	@Override
	public void insertRecord(StoredTraceRecord stored) throws DuplicateEntryException, TraceStorageException {
		recordInserts.incrementAndGet();
		super.insertRecord(stored);
	}

}
