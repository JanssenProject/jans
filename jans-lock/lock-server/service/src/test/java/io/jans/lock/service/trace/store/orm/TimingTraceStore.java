/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TracePolicyReleaseEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.PolicyRecordQuery;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.store.TraceStore;

/**
 * {@link TraceStore} decorator which counts the calls and sums the time of every method, so a
 * benchmark can show where the time of one ingested record goes. It delegates every method and adds
 * only two {@link System#nanoTime()} reads and two {@link LongAdder} updates per call; it logs
 * nothing. A call which throws is counted and timed too, and the exception propagates unchanged.
 *
 * <p>A method is a write when its name starts with {@code insert}, {@code settle}, {@code update},
 * {@code revoke}, {@code record} or {@code remove}; every other method is a read.
 *
 * @author Yuriy Movchan
 */
public class TimingTraceStore implements TraceStore {

	private static final String[] WRITE_PREFIXES = { "insert", "settle", "update", "revoke", "record", "remove" };

	private final TraceStore delegate;

	private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

	public TimingTraceStore(TraceStore delegate) {
		this.delegate = delegate;
	}

	/** @return {@code true} when the {@link TraceStore} method named {@code methodName} writes */
	public static boolean isWrite(String methodName) {
		for (String prefix : WRITE_PREFIXES) {
			if (methodName.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	/** @return the calls and the time recorded since construction or the last {@link #reset()} */
	public Snapshot snapshot() {
		Map<String, MethodStats> sorted = new TreeMap<>();
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			long calls = entry.getValue().calls.sum();
			if (calls > 0) {
				sorted.put(entry.getKey(), new MethodStats(entry.getKey(), calls, entry.getValue().nanos.sum()));
			}
		}
		return new Snapshot(new ArrayList<>(sorted.values()));
	}

	public void reset() {
		timers.clear();
	}

	private void record(String method, long startNs) {
		long elapsed = System.nanoTime() - startNs;
		Timer timer = timers.computeIfAbsent(method, name -> new Timer());
		timer.calls.increment();
		timer.nanos.add(elapsed);
	}

	// -- records ------------------------------------------------------------------------------

	@Override
	public void insertRecord(StoredTraceRecord stored) throws DuplicateEntryException, TraceStorageException {
		long start = System.nanoTime();
		try {
			delegate.insertRecord(stored);
		} finally {
			record("insertRecord", start);
		}
	}

	@Override
	public boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags, long expectedVersion)
			throws TraceStorageException {
		long start = System.nanoTime();
		try {
			return delegate.updateRecordFlags(id, flags, expectedVersion);
		} finally {
			record("updateRecordFlags", start);
		}
	}

	@Override
	public Optional<StoredTraceRecord> findRecord(RecordIdentity id) {
		long start = System.nanoTime();
		try {
			return delegate.findRecord(id);
		} finally {
			record("findRecord", start);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByBareRecordId(String domainId, String recordId) {
		long start = System.nanoTime();
		try {
			return delegate.findRecordsByBareRecordId(domainId, recordId);
		} finally {
			record("findRecordsByBareRecordId", start);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByExecution(ExecutionIdentity exec, int start, int count) {
		long begin = System.nanoTime();
		try {
			return delegate.findRecordsByExecution(exec, start, count);
		} finally {
			record("findRecordsByExecution", begin);
		}
	}

	@Override
	public List<String> findExecutionAuthorities(String domainId, String traceExecutionId) {
		long start = System.nanoTime();
		try {
			return delegate.findExecutionAuthorities(domainId, traceExecutionId);
		} finally {
			record("findExecutionAuthorities", start);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position) {
		long start = System.nanoTime();
		try {
			return delegate.findRecordsByChainPosition(position);
		} finally {
			record("findRecordsByChainPosition", start);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token) {
		long start = System.nanoTime();
		try {
			return delegate.findRecordsByToken(domainId, token);
		} finally {
			record("findRecordsByToken", start);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByPolicy(String domainId, PolicyRecordQuery query, int start, int count) {
		long begin = System.nanoTime();
		try {
			return delegate.findRecordsByPolicy(domainId, query, start, count);
		} finally {
			record("findRecordsByPolicy", begin);
		}
	}

	@Override
	public long countRecords(String domainId) {
		long start = System.nanoTime();
		try {
			return delegate.countRecords(domainId);
		} finally {
			record("countRecords", start);
		}
	}

	// -- receipts -------------------------------------------------------------------------------

	@Override
	public void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException, TraceStorageException {
		long start = System.nanoTime();
		try {
			delegate.insertReceipt(row);
		} finally {
			record("insertReceipt", start);
		}
	}

	@Override
	public ReceiptSettlement settleReceipt(String domainId, long receiptSequence, TraceReceiptState target)
			throws TraceStorageException {
		long start = System.nanoTime();
		try {
			return delegate.settleReceipt(domainId, receiptSequence, target);
		} finally {
			record("settleReceipt", start);
		}
	}

	@Override
	public Optional<ReceiptHead> findReceiptHead(String domainId) {
		long start = System.nanoTime();
		try {
			return delegate.findReceiptHead(domainId);
		} finally {
			record("findReceiptHead", start);
		}
	}

	@Override
	public Optional<TraceReceiptEntry> findReceipt(String domainId, long receiptSequence) {
		long start = System.nanoTime();
		try {
			return delegate.findReceipt(domainId, receiptSequence);
		} finally {
			record("findReceipt", start);
		}
	}

	@Override
	public List<TraceReceiptEntry> findReceipts(String domainId, Collection<Long> receiptSequences) {
		long start = System.nanoTime();
		try {
			return delegate.findReceipts(domainId, receiptSequences);
		} finally {
			record("findReceipts", start);
		}
	}

	// -- policy store releases ------------------------------------------------------------------

	@Override
	public Optional<TracePolicyReleaseEntry> findPolicyRelease(String policyStoreId, String bundleHash) {
		long start = System.nanoTime();
		try {
			return delegate.findPolicyRelease(policyStoreId, bundleHash);
		} finally {
			record("findPolicyRelease", start);
		}
	}

	@Override
	public void insertPolicyRelease(TracePolicyReleaseEntry entry) throws DuplicateEntryException, TraceStorageException {
		long start = System.nanoTime();
		try {
			delegate.insertPolicyRelease(entry);
		} finally {
			record("insertPolicyRelease", start);
		}
	}

	@Override
	public boolean recordPolicyReleaseServed(String policyStoreId, String bundleHash, String policyStoreVersion,
			long servedAtMs) throws TraceStorageException {
		long start = System.nanoTime();
		try {
			return delegate.recordPolicyReleaseServed(policyStoreId, bundleHash, policyStoreVersion, servedAtMs);
		} finally {
			record("recordPolicyReleaseServed", start);
		}
	}

	@Override
	public List<TracePolicyReleaseEntry> findPolicyReleasesLastServedBefore(long cutoffMs, int limit) {
		long start = System.nanoTime();
		try {
			return delegate.findPolicyReleasesLastServedBefore(cutoffMs, limit);
		} finally {
			record("findPolicyReleasesLastServedBefore", start);
		}
	}

	@Override
	public boolean removePolicyReleaseLastServedBefore(String policyStoreId, String bundleHash, long cutoffMs)
			throws TraceStorageException {
		long start = System.nanoTime();
		try {
			return delegate.removePolicyReleaseLastServedBefore(policyStoreId, bundleHash, cutoffMs);
		} finally {
			record("removePolicyReleaseLastServedBefore", start);
		}
	}

	@Override
	public List<TraceReceiptEntry> findPendingReceiptsOlderThan(long cutoffMs, int limit) {
		long start = System.nanoTime();
		try {
			return delegate.findPendingReceiptsOlderThan(cutoffMs, limit);
		} finally {
			record("findPendingReceiptsOlderThan", start);
		}
	}

	// -- registries -----------------------------------------------------------------------------

	@Override
	public Optional<TraceChainEntry> findChain(ChainIdentity id) {
		long start = System.nanoTime();
		try {
			return delegate.findChain(id);
		} finally {
			record("findChain", start);
		}
	}

	@Override
	public void insertChain(TraceChainEntry registration) throws DuplicateEntryException, TraceStorageException {
		long start = System.nanoTime();
		try {
			delegate.insertChain(registration);
		} finally {
			record("insertChain", start);
		}
	}

	@Override
	public List<TraceChainEntry> findChains(String domainId, String producerIdOrNull) {
		long start = System.nanoTime();
		try {
			return delegate.findChains(domainId, producerIdOrNull);
		} finally {
			record("findChains", start);
		}
	}

	@Override
	public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
		long start = System.nanoTime();
		try {
			return delegate.findProducerKey(domainId, producerId, kid);
		} finally {
			record("findProducerKey", start);
		}
	}

	@Override
	public void insertProducerKey(TraceProducerKeyEntry key) throws DuplicateEntryException, TraceStorageException {
		long start = System.nanoTime();
		try {
			delegate.insertProducerKey(key);
		} finally {
			record("insertProducerKey", start);
		}
	}

	@Override
	public boolean revokeProducerKey(String domainId, String producerId, String kid, long revokedAtMs)
			throws TraceStorageException {
		long start = System.nanoTime();
		try {
			return delegate.revokeProducerKey(domainId, producerId, kid, revokedAtMs);
		} finally {
			record("revokeProducerKey", start);
		}
	}

	@Override
	public List<TraceProducerKeyEntry> findProducerKeys(String domainId, String producerIdOrNull) {
		long start = System.nanoTime();
		try {
			return delegate.findProducerKeys(domainId, producerIdOrNull);
		} finally {
			record("findProducerKeys", start);
		}
	}

	private static final class Timer {

		final LongAdder calls = new LongAdder();

		final LongAdder nanos = new LongAdder();
	}

	/** Calls and total time of one {@link TraceStore} method. */
	public static final class MethodStats {

		private final String method;

		private final long calls;

		private final long totalNanos;

		MethodStats(String method, long calls, long totalNanos) {
			this.method = method;
			this.calls = calls;
			this.totalNanos = totalNanos;
		}

		public String method() {
			return method;
		}

		public long calls() {
			return calls;
		}

		public long totalNanos() {
			return totalNanos;
		}

		public boolean write() {
			return isWrite(method);
		}
	}

	/** Immutable copy of the recorded numbers, methods sorted by name. */
	public static final class Snapshot {

		private final List<MethodStats> methods;

		Snapshot(List<MethodStats> methods) {
			this.methods = Collections.unmodifiableList(methods);
		}

		public List<MethodStats> methods() {
			return methods;
		}

		public boolean isEmpty() {
			return methods.isEmpty();
		}

		public long totalCalls() {
			return calls(true) + calls(false);
		}

		public long writeCalls() {
			return calls(true);
		}

		public long readCalls() {
			return calls(false);
		}

		public long totalNanos() {
			long sum = 0;
			for (MethodStats stats : methods) {
				sum += stats.totalNanos();
			}
			return sum;
		}

		/** @return the stats of {@code method}, or {@code null} when it was never called */
		public MethodStats get(String method) {
			for (MethodStats stats : methods) {
				if (stats.method().equals(method)) {
					return stats;
				}
			}
			return null;
		}

		private long calls(boolean writes) {
			long sum = 0;
			for (MethodStats stats : methods) {
				if (stats.write() == writes) {
					sum += stats.calls();
				}
			}
			return sum;
		}
	}

}
