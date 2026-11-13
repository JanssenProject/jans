/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;

/**
 * The {@link PlanLookup} of one batch. It answers from the records read before planning (one
 * prefetch for the whole batch) plus the <em>provisional</em> records of the earlier elements of the
 * same batch that planned successfully, so record n of a chain sees record n-1 as its predecessor
 * exactly as it would when the elements are ingested one by one.
 *
 * <p>A provisional record is built from the verified assertion and its plan with a
 * <strong>placeholder receipt</strong> (sequence {@code 0}, zero hashes). It lives only in this
 * overlay: it is never persisted, never returned to a caller and its receipt must never be read.
 * What planning reads from a record is its identity, chain position and content digest.
 *
 * <p>The overlay also applies, to its own copies, the flag changes that
 * {@link TraceCorrelationService#afterInsert} would make to neighbors of an inserted record
 * (equivocation peers; coverage gap and chain link of the successor). Those copies are what an
 * idempotent replay later in the batch reads, as it would read the store in the one-by-one path.
 * The overlay is confined to one request thread and is not thread safe.
 *
 * @author Yuriy Movchan
 */
public final class BatchOverlay implements PlanLookup {

	private static final class Entry {

		private StoredTraceRecord record;

		private final boolean provisional;

		private Entry(StoredTraceRecord record, boolean provisional) {
			this.record = record;
			this.provisional = provisional;
		}

	}

	private final Map<RecordIdentity, Entry> byIdentity = new HashMap<>();

	private final Map<ChainPosition, List<Entry>> byPosition = new HashMap<>();

	private final Set<ChainIdentity> confirmedChains = new HashSet<>();

	/**
	 * @param existing   records stored before the batch, by identity (may be empty)
	 * @param prefetched records stored at the predecessor, same and successor positions of the batch
	 *                   items, as returned by {@code TraceStore.findRecordsByChainPositions}
	 */
	public BatchOverlay(Map<RecordIdentity, StoredTraceRecord> existing,
			Map<ChainPosition, List<StoredTraceRecord>> prefetched) {
		List<StoredTraceRecord> known = new ArrayList<>(existing.values());
		for (List<StoredTraceRecord> rows : prefetched.values()) {
			known.addAll(rows);
		}
		for (StoredTraceRecord record : known) {
			if (byIdentity.containsKey(record.getIdentity())) {
				continue;
			}
			Entry entry = new Entry(record.copy(), false);
			byIdentity.put(record.getIdentity(), entry);
			byPosition.computeIfAbsent(record.getChainPosition(), p -> new ArrayList<>()).add(entry);
		}
		Comparator<Entry> bySequence = Comparator.comparingLong(e -> e.record.getReceipt().getReceiptSequence());
		for (List<Entry> entries : byPosition.values()) {
			entries.sort(bySequence);
		}
	}

	@Override
	public List<StoredTraceRecord> findRecordsByChainPosition(ChainPosition position) {
		List<Entry> entries = byPosition.get(position);
		if (entries == null) {
			return Collections.emptyList();
		}
		List<StoredTraceRecord> rows = new ArrayList<>(entries.size());
		for (Entry entry : entries) {
			rows.add(entry.record);
		}
		return rows;
	}

	@Override
	public boolean isChainConfirmed(ChainIdentity chain) {
		return confirmedChains.contains(chain);
	}

	@Override
	public void confirmChain(ChainIdentity chain) {
		confirmedChains.add(chain);
	}

	/**
	 * @return the overlay's current copy of the record holding {@code identity}: a record stored before
	 *         the batch, or a provisional one (see {@link #isProvisional})
	 */
	public Optional<StoredTraceRecord> find(RecordIdentity identity) {
		Entry entry = byIdentity.get(identity);
		return entry == null ? Optional.<StoredTraceRecord>empty() : Optional.of(entry.record);
	}

	/**
	 * @return {@code true} when {@code identity} was added by {@link #add}, so its receipt is a placeholder
	 */
	public boolean isProvisional(RecordIdentity identity) {
		Entry entry = byIdentity.get(identity);
		return entry != null && entry.provisional;
	}

	/**
	 * Adds the provisional record of an element that planned successfully and applies the flag
	 * changes {@link TraceCorrelationService#afterInsert} makes to its neighbors.
	 */
	public void add(StoredTraceRecord provisional) {
		ChainPosition position = provisional.getChainPosition();
		List<Entry> atPosition = byPosition.computeIfAbsent(position, p -> new ArrayList<>());
		for (Entry peer : atPosition) {
			IngestionFlags flags = peer.record.getFlags();
			if (!flags.isEquivocation()) {
				flags.setEquivocation(true);
				peer.record = peer.record.withFlags(flags, peer.record.getVersion());
			}
		}

		ChainPosition next = new ChainPosition(position.getChainIdentity(), position.getSequenceNumber() + 1);
		List<Entry> successors = byPosition.get(next);
		if (successors != null) {
			for (Entry successor : successors) {
				IngestionFlags flags = successor.record.getFlags();
				boolean changed = false;
				if (flags.isCoverageGap()) {
					flags.setCoverageGap(false);
					changed = true;
				}
				if (!flags.isChainLinkFailure() && ProducerChainRules
						.successorLinkBroken(successor.record.getPrevRecordHash(), provisional.getContentDigest())) {
					flags.setChainLinkFailure(true);
					changed = true;
				}
				if (changed) {
					successor.record = successor.record.withFlags(flags, successor.record.getVersion());
				}
			}
		}

		Entry entry = new Entry(provisional, true);
		atPosition.add(entry);
		byIdentity.put(provisional.getIdentity(), entry);
	}

}
