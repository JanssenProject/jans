/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.jans.lock.model.trace.entity.TraceChainEntry;
import io.jans.lock.model.trace.entity.TraceProducerKeyEntry;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * An in-memory {@link TraceStore} used by every higher-level TRACE test (tasks 13, 17, 18, 19),
 * and exercised directly by {@code TraceStoreContractTest}. Backed by {@link ConcurrentHashMap}s
 * keyed by the {@link TraceKeys} outputs, so the same collision-freedom guarantees apply as to a
 * real backend.
 *
 * <p>Every stored value is either inherently immutable ({@code String}/{@code long}/enum) or,
 * for {@link StoredTraceRecord} and the mutable jans-orm entities ({@link TraceReceiptEntry},
 * {@link TraceChainEntry}, {@link TraceProducerKeyEntry} — which carry a {@code Map} or plain
 * setters), defensively copied on the way in and out, so a caller can never mutate what the store
 * holds (design rule 4: immutability of evidence).
 *
 * <p>Test hooks: {@link #failNextInsertRecord()}, {@link #failNextInsertReceipt()},
 * {@link #failNextUpdateReceiptState()} make the next matching call throw
 * {@link TraceStorageException} instead of touching any map (auto-resetting, one-shot);
 * {@link #setInsertRecordBarrier(CountDownLatch, long, TimeUnit)} lets a race test rendezvous two
 * threads immediately before the {@code insertRecord} map mutation; {@link #snapshot()} returns an
 * immutable view for assertions like "nothing became visible after a failure".
 *
 * @author Yuriy Movchan
 */
public class InMemoryTraceStore implements TraceStore {

	private final ConcurrentHashMap<String, StoredTraceRecord> recordsByKey = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, TraceReceiptEntry> receiptsByKey = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, TraceChainEntry> chainsByKey = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, TraceProducerKeyEntry> producerKeysByKey = new ConcurrentHashMap<>();

	private final AtomicBoolean failNextInsertRecord = new AtomicBoolean(false);

	private final AtomicBoolean failNextInsertReceipt = new AtomicBoolean(false);

	private final AtomicBoolean failNextUpdateReceiptState = new AtomicBoolean(false);

	private volatile CountDownLatch insertRecordBarrier;

	private volatile long insertRecordBarrierTimeoutMs;

	// -- test hooks ---------------------------------------------------------------------------

	/** The next {@link #insertRecord(StoredTraceRecord)} call throws {@link TraceStorageException}. */
	public void failNextInsertRecord() {
		failNextInsertRecord.set(true);
	}

	/** The next {@link #insertReceipt(TraceReceiptEntry)} call throws {@link TraceStorageException}. */
	public void failNextInsertReceipt() {
		failNextInsertReceipt.set(true);
	}

	/**
	 * The next {@link #updateReceiptState(String, long, TraceReceiptState)} call throws
	 * {@link TraceStorageException}.
	 */
	public void failNextUpdateReceiptState() {
		failNextUpdateReceiptState.set(true);
	}

	/**
	 * Installs a barrier that {@link #insertRecord(StoredTraceRecord)} counts down and then waits
	 * on (bounded by {@code timeout}) immediately before it becomes visible in the store, so a race
	 * test can line up two concurrent inserts. Pass {@code null} to remove the barrier.
	 */
	public void setInsertRecordBarrier(CountDownLatch latch, long timeout, TimeUnit unit) {
		this.insertRecordBarrier = latch;
		this.insertRecordBarrierTimeoutMs = latch == null ? 0 : unit.toMillis(timeout);
	}

	/**
	 * @return an immutable snapshot of every record currently visible in the store, independent of
	 *         later mutations
	 */
	public List<StoredTraceRecord> snapshot() {
		return Collections
				.unmodifiableList(recordsByKey.values().stream().map(StoredTraceRecord::copy).collect(Collectors.toList()));
	}

	// -- records --------------------------------------------------------------------------------

	@Override
	public void insertRecord(StoredTraceRecord record) throws DuplicateEntryException, TraceStorageException {
		if (failNextInsertRecord.getAndSet(false)) {
			throw new TraceStorageException("test_fail_next_insert_record", "Forced test failure");
		}
		awaitInsertRecordBarrier();

		String key = TraceKeys.recordKey(record.getIdentity());
		StoredTraceRecord existing = recordsByKey.putIfAbsent(key, record.copy());
		if (existing != null) {
			throw new DuplicateEntryException(key);
		}
	}

	private void awaitInsertRecordBarrier() {
		CountDownLatch latch = this.insertRecordBarrier;
		if (latch == null) {
			return;
		}
		latch.countDown();
		boolean reachedZero;
		try {
			reachedZero = latch.await(insertRecordBarrierTimeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TraceStorageException("insert_record_barrier_interrupted", "Interrupted awaiting barrier", e);
		}
		if (!reachedZero) {
			throw new TraceStorageException("insert_record_barrier_timeout",
					"Timed out waiting for the insertRecord barrier latch to reach zero");
		}
	}

	@Override
	public boolean updateRecordFlags(RecordIdentity id, IngestionFlags flags) throws TraceStorageException {
		String key = TraceKeys.recordKey(id);
		StoredTraceRecord[] holder = new StoredTraceRecord[1];
		recordsByKey.computeIfPresent(key, (k, existing) -> {
			IngestionFlags updated = existing.getFlags();
			updated.setCoverageGap(flags.isCoverageGap());
			updated.setChainLinkFailure(flags.isChainLinkFailure());
			updated.setEquivocation(flags.isEquivocation());
			// jansTraceLate is fixed at ingestion (design §10, T-1); never overwritten here.
			StoredTraceRecord replacement = existing.withFlags(updated);
			holder[0] = replacement;
			return replacement;
		});
		return holder[0] != null;
	}

	@Override
	public Optional<StoredTraceRecord> findRecord(RecordIdentity id) {
		StoredTraceRecord found = recordsByKey.get(TraceKeys.recordKey(id));
		return Optional.ofNullable(found).map(StoredTraceRecord::copy);
	}

	@Override
	public List<StoredTraceRecord> findRecordsByBareRecordId(String domainId, String recordId) {
		return recordsByKey.values().stream()
				.filter(r -> r.getIdentity().getDomainId().equals(domainId)
						&& r.getIdentity().getRecordId().equals(recordId))
				.map(StoredTraceRecord::copy).collect(Collectors.toList());
	}

	@Override
	public List<StoredTraceRecord> findRecordsByExecution(ExecutionIdentity exec, int start, int count) {
		return recordsByKey.values().stream().filter(r -> r.getExecution().equals(exec))
				.sorted(Comparator.comparingLong(r -> r.getReceipt().getReceiptSequence())).skip(Math.max(0, start))
				.limit(Math.max(0, count)).map(StoredTraceRecord::copy).collect(Collectors.toList());
	}

	@Override
	public List<String> findExecutionAuthorities(String domainId, String traceExecutionId) {
		Set<String> authorities = new LinkedHashSet<>();
		recordsByKey.values().stream()
				.filter(r -> r.getExecution().getDomainId().equals(domainId)
						&& r.getExecution().getTraceExecutionId().equals(traceExecutionId))
				.sorted(Comparator.comparingLong(r -> r.getReceipt().getReceiptSequence()))
				.forEach(r -> authorities.add(r.getExecution().getExecutionAuthority()));
		return new ArrayList<>(authorities);
	}

	@Override
	public List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position) {
		return recordsByKey.values().stream().filter(r -> r.getChainPosition().equals(position))
				.sorted(Comparator.comparingLong(r -> r.getReceipt().getReceiptSequence())).map(StoredTraceRecord::copy)
				.collect(Collectors.toList());
	}

	@Override
	public List<StoredTraceRecord> findRecordsByCapability(String domainId, String capabilityId) {
		String targetKey = TraceKeys.capabilityKey(domainId, capabilityId);
		return recordsByKey.values().stream()
				.filter(r -> r.getIdentity().getDomainId().equals(domainId) && r.getCapabilityIds().stream()
						.anyMatch(cap -> TraceKeys.capabilityKey(domainId, cap).equals(targetKey)))
				.sorted(Comparator.comparingLong(r -> r.getReceipt().getReceiptSequence())).map(StoredTraceRecord::copy)
				.collect(Collectors.toList());
	}

	@Override
	public List<StoredTraceRecord> findRecordsByToken(String domainId, TokenRef token) {
		String targetKey = TraceKeys.tokenKey(domainId, token);
		return recordsByKey.values().stream()
				.filter(r -> r.getIdentity().getDomainId().equals(domainId) && r.getTokenRefs().stream()
						.anyMatch(ref -> TraceKeys.tokenKey(domainId, ref).equals(targetKey)))
				.sorted(Comparator.comparingLong(r -> r.getReceipt().getReceiptSequence())).map(StoredTraceRecord::copy)
				.collect(Collectors.toList());
	}

	@Override
	public long countRecords(String domainId) {
		return recordsByKey.values().stream().filter(r -> r.getIdentity().getDomainId().equals(domainId)).count();
	}

	// -- receipts -------------------------------------------------------------------------------

	@Override
	public void insertReceipt(TraceReceiptEntry row) throws DuplicateEntryException, TraceStorageException {
		if (failNextInsertReceipt.getAndSet(false)) {
			throw new TraceStorageException("test_fail_next_insert_receipt", "Forced test failure");
		}
		String key = TraceKeys.receiptKey(row.getDomainId(), row.getReceiptSeq());
		row.setId(key);
		TraceReceiptEntry existing = receiptsByKey.putIfAbsent(key, copyOf(row));
		if (existing != null) {
			throw new DuplicateEntryException(key);
		}
	}

	@Override
	public boolean updateReceiptState(String domainId, long receiptSequence, TraceReceiptState state)
			throws TraceStorageException {
		if (failNextUpdateReceiptState.getAndSet(false)) {
			throw new TraceStorageException("test_fail_next_update_receipt_state", "Forced test failure");
		}
		String key = TraceKeys.receiptKey(domainId, receiptSequence);
		TraceReceiptEntry[] holder = new TraceReceiptEntry[1];
		receiptsByKey.computeIfPresent(key, (k, existing) -> {
			TraceReceiptEntry replacement = copyOf(existing);
			replacement.setReceiptStateEnum(state);
			holder[0] = replacement;
			return replacement;
		});
		return holder[0] != null;
	}

	@Override
	public Optional<ReceiptHead> findReceiptHead(String domainId) {
		return receiptsByKey.values().stream().filter(r -> r.getDomainId().equals(domainId))
				.max(Comparator.comparingLong(TraceReceiptEntry::getReceiptSeq))
				.map(r -> new ReceiptHead(r.getReceiptSeq(), r.getReceiptHash()));
	}

	@Override
	public Optional<TraceReceiptEntry> findReceipt(String domainId, long receiptSequence) {
		return Optional.ofNullable(receiptsByKey.get(TraceKeys.receiptKey(domainId, receiptSequence)))
				.map(InMemoryTraceStore::copyOf);
	}

	@Override
	public List<TraceReceiptEntry> findPendingReceiptsOlderThan(long cutoffMs, int limit) {
		return receiptsByKey.values().stream()
				.filter(r -> r.getReceiptStateEnum() == TraceReceiptState.PENDING && r.getReceivedAtMs() < cutoffMs)
				.sorted(Comparator.comparingLong(TraceReceiptEntry::getReceivedAtMs)).limit(Math.max(0, limit))
				.map(InMemoryTraceStore::copyOf).collect(Collectors.toList());
	}

	// -- registries -----------------------------------------------------------------------------

	@Override
	public Optional<TraceChainEntry> findChain(ChainIdentity id) {
		return Optional.ofNullable(chainsByKey.get(TraceKeys.chainKey(id))).map(InMemoryTraceStore::copyOf);
	}

	@Override
	public void insertChain(TraceChainEntry registration) throws DuplicateEntryException, TraceStorageException {
		ChainIdentity identity = new ChainIdentity(registration.getDomainId(), registration.getProducerId(),
				registration.getProducerInstanceId(), registration.getProducerChainId());
		String key = TraceKeys.chainKey(identity);
		registration.setId(key);
		TraceChainEntry existing = chainsByKey.putIfAbsent(key, copyOf(registration));
		if (existing != null) {
			throw new DuplicateEntryException(key);
		}
	}

	@Override
	public List<TraceChainEntry> findChains(String domainId, String producerIdOrNull) {
		return chainsByKey.values().stream()
				.filter(c -> c.getDomainId().equals(domainId)
						&& (producerIdOrNull == null || c.getProducerId().equals(producerIdOrNull)))
				.map(InMemoryTraceStore::copyOf).collect(Collectors.toList());
	}

	@Override
	public Optional<TraceProducerKeyEntry> findProducerKey(String domainId, String producerId, String kid) {
		TraceProducerKeyEntry found = producerKeysByKey.get(TraceKeys.producerKeyKey(domainId, producerId, kid));
		return Optional.ofNullable(found).map(InMemoryTraceStore::copyOf);
	}

	@Override
	public void insertProducerKey(TraceProducerKeyEntry key) throws DuplicateEntryException, TraceStorageException {
		String mapKey = TraceKeys.producerKeyKey(key.getDomainId(), key.getProducerId(), key.getKid());
		key.setId(mapKey);
		TraceProducerKeyEntry existing = producerKeysByKey.putIfAbsent(mapKey, copyOf(key));
		if (existing != null) {
			throw new DuplicateEntryException(mapKey);
		}
	}

	@Override
	public boolean revokeProducerKey(String domainId, String producerId, String kid, long revokedAtMs)
			throws TraceStorageException {
		String key = TraceKeys.producerKeyKey(domainId, producerId, kid);
		TraceProducerKeyEntry[] holder = new TraceProducerKeyEntry[1];
		producerKeysByKey.computeIfPresent(key, (k, existing) -> {
			// Idempotent: an already-revoked key keeps its original revokedAt.
			TraceProducerKeyEntry replacement = copyOf(existing);
			if (replacement.getRevokedAt() == null) {
				replacement.setRevokedAt(new Date(revokedAtMs));
			}
			holder[0] = replacement;
			return replacement;
		});
		return holder[0] != null;
	}

	@Override
	public List<TraceProducerKeyEntry> findProducerKeys(String domainId, String producerIdOrNull) {
		return producerKeysByKey.values().stream()
				.filter(k -> k.getDomainId().equals(domainId)
						&& (producerIdOrNull == null || k.getProducerId().equals(producerIdOrNull)))
				.map(InMemoryTraceStore::copyOf).collect(Collectors.toList());
	}

	// -- defensive copy helpers (design rule 4: immutability of evidence) -----------------------

	private static TraceReceiptEntry copyOf(TraceReceiptEntry source) {
		TraceReceiptEntry copy = new TraceReceiptEntry();
		copy.setDn(source.getDn());
		copy.setId(source.getId());
		copy.setDomainId(source.getDomainId());
		copy.setReceiptSeq(source.getReceiptSeq());
		copy.setReceivedAt(source.getReceivedAt());
		copy.setReceivedAtMs(source.getReceivedAtMs());
		copy.setProducerId(source.getProducerId());
		copy.setRecordId(source.getRecordId());
		copy.setRecordKey(source.getRecordKey());
		copy.setContentDigest(source.getContentDigest());
		copy.setPrevReceiptHash(source.getPrevReceiptHash());
		copy.setReceiptHash(source.getReceiptHash());
		copy.setReceiptState(source.getReceiptState());
		copy.setNodeId(source.getNodeId());
		copy.setCreationDate(source.getCreationDate());
		return copy;
	}

	private static TraceChainEntry copyOf(TraceChainEntry source) {
		TraceChainEntry copy = new TraceChainEntry();
		copy.setDn(source.getDn());
		copy.setId(source.getId());
		copy.setDomainId(source.getDomainId());
		copy.setProducerId(source.getProducerId());
		copy.setProducerInstanceId(source.getProducerInstanceId());
		copy.setProducerChainId(source.getProducerChainId());
		copy.setRegisteredBy(source.getRegisteredBy());
		copy.setCreationDate(source.getCreationDate());
		return copy;
	}

	private static TraceProducerKeyEntry copyOf(TraceProducerKeyEntry source) {
		TraceProducerKeyEntry copy = new TraceProducerKeyEntry();
		copy.setDn(source.getDn());
		copy.setId(source.getId());
		copy.setDomainId(source.getDomainId());
		copy.setProducerId(source.getProducerId());
		copy.setKid(source.getKid());
		Map<String, String> jwk = source.getPublicKeyJwk();
		copy.setPublicKeyJwk(jwk == null ? null : new LinkedHashMap<>(jwk));
		copy.setValidFrom(source.getValidFrom());
		copy.setValidUntil(source.getValidUntil());
		copy.setRevokedAt(source.getRevokedAt());
		copy.setRegisteredBy(source.getRegisteredBy());
		copy.setCreationDate(source.getCreationDate());
		return copy;
	}

}
