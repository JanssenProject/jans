/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.ReceiptHead;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.Entry;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.EntryStatus;
import io.jans.lock.service.trace.store.TraceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Allocates per-domain receipt-chain positions and hash-chains them safely across several Lock
 * nodes, using only primary-key uniqueness (design §9, TRACE MVP design decision D-8). jans-orm
 * offers no transactions, sequences or CAS, so correctness comes entirely from
 * {@link TraceStore#insertReceipt} rejecting a second claim at the same {@code (domainId, seq)};
 * the per-domain {@link ReentrantLock} below only reduces same-node contention and is never load
 * bearing (design decision D-8 step 1).
 *
 * <p>{@link #claim} is the only method that allocates a new sequence; {@link #commit} and
 * {@link #voidClaim} settle an already-allocated one and never throw — a failure is logged at
 * WARN and left for {@code TraceReceiptRepairTimer} to resolve later (design decision D-8 steps
 * 6-7).
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceReceiptChain {

	@Inject
	private Logger log;

	@Inject
	private TraceStore store;

	@Inject
	private AppConfiguration appConfiguration;

	private final ConcurrentHashMap<String, ReentrantLock> domainLocks = new ConcurrentHashMap<>();

	/**
	 * Allocates the next receipt-chain position for {@code domainId} and inserts it as a
	 * {@code PENDING} claim (design decision D-8 steps 2-4). Retries on contention with another
	 * claim (same node or another) up to {@code receiptAllocationRetryLimit} times.
	 *
	 * @throws TraceStorageException {@code receipt_allocation_exhausted} once every retry is spent,
	 *                                or any other persistence failure surfaced by the store
	 */
	public ReceiptClaim claim(String domainId, String producerId, String recordId, String recordKey,
			String contentDigest, long receivedAtMs, String nodeId) {
		ReentrantLock lock = domainLocks.computeIfAbsent(domainId, d -> new ReentrantLock());
		int retryLimit = appConfiguration.getTraceConfiguration().getReceiptAllocationRetryLimit();

		lock.lock();
		try {
			for (int attempt = 0; attempt < retryLimit; attempt++) {
				Optional<ReceiptHead> head = store.findReceiptHead(domainId);
				long seq = head.map(h -> h.getReceiptSequence() + 1).orElse(1L);
				String prevReceiptHash = head.map(ReceiptHead::getReceiptHash).orElse(TraceConstants.ZERO_HASH);
				String receiptHash = ReceiptHashes.receiptHash(domainId, seq, receivedAtMs, producerId, recordId,
						contentDigest, prevReceiptHash);

				TraceReceiptEntry row = new TraceReceiptEntry();
				row.setDomainId(domainId);
				row.setReceiptSeq(seq);
				row.setReceivedAt(new Date(receivedAtMs));
				row.setReceivedAtMs(receivedAtMs);
				row.setProducerId(producerId);
				row.setRecordId(recordId);
				row.setRecordKey(recordKey);
				row.setContentDigest(contentDigest);
				row.setPrevReceiptHash(prevReceiptHash);
				row.setReceiptHash(receiptHash);
				row.setReceiptStateEnum(TraceReceiptState.PENDING);
				row.setNodeId(nodeId);
				row.setCreationDate(new Date(receivedAtMs));

				try {
					store.insertReceipt(row);
					return new ReceiptClaim(seq, prevReceiptHash, receiptHash, receivedAtMs);
				} catch (DuplicateEntryException ex) {
					log.debug("receipt position contended: domain={}, seq={}", domainId, seq);
				}
			}
			throw new TraceStorageException("receipt_allocation_exhausted",
					"Exhausted receipt allocation retries for domain " + domainId);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Settles a claim as {@code COMMITTED} once the record row carrying it is visible (design
	 * decision D-8 step 6). Best-effort: a failure is logged at WARN, not thrown — the repair timer
	 * resolves a stuck {@code PENDING} row later.
	 */
	public void commit(String domainId, long seq) {
		try {
			boolean updated = store.updateReceiptState(domainId, seq, TraceReceiptState.COMMITTED);
			if (!updated) {
				log.warn("Could not commit receipt: no row at domain={}, seq={}", domainId, seq);
			}
		} catch (TraceStorageException ex) {
			log.warn("Failed to commit receipt: domain={}, seq={}", domainId, seq, ex);
		}
	}

	/**
	 * Settles a claim as {@code VOID} after the matching record insert lost a cross-node identity
	 * race or otherwise failed (design decision D-8 step 5). Best-effort, like {@link #commit}.
	 */
	public void voidClaim(String domainId, long seq) {
		try {
			boolean updated = store.updateReceiptState(domainId, seq, TraceReceiptState.VOID);
			if (!updated) {
				log.warn("Could not void receipt: no row at domain={}, seq={}", domainId, seq);
			}
		} catch (TraceStorageException ex) {
			log.warn("Failed to void receipt: domain={}, seq={}", domainId, seq, ex);
		}
	}

	/**
	 * Diagnostic walk of {@code domainId}'s receipt chain from sequence 1 up to its head (capped at
	 * {@code maxEntries} positions): density, hash recomputation, {@code prev} linkage, and, per
	 * state, record-row consistency (design §9). Never mutates anything.
	 */
	public ChainVerificationReport verifyDomain(String domainId, int maxEntries) {
		Optional<ReceiptHead> head = store.findReceiptHead(domainId);
		if (!head.isPresent()) {
			return new ChainVerificationReport(true, 0, new ArrayList<>(), false);
		}

		long headSequence = head.get().getReceiptSequence();
		long limit = Math.min(headSequence, maxEntries);
		boolean truncated = headSequence > maxEntries;

		List<Entry> entries = new ArrayList<>();
		boolean ok = true;
		String expectedPrevHash = TraceConstants.ZERO_HASH;

		for (long seq = 1; seq <= limit; seq++) {
			Optional<TraceReceiptEntry> receiptOpt = store.findReceipt(domainId, seq);
			if (!receiptOpt.isPresent()) {
				entries.add(new Entry(seq, null, EntryStatus.MISSING_SEQUENCE));
				ok = false;
				continue;
			}

			TraceReceiptEntry receipt = receiptOpt.get();
			TraceReceiptState state = receipt.getReceiptStateEnum();
			String recomputedHash = ReceiptHashes.receiptHash(domainId, seq, receipt.getReceivedAtMs(),
					receipt.getProducerId(), receipt.getRecordId(), receipt.getContentDigest(),
					receipt.getPrevReceiptHash());

			EntryStatus status;
			if (!recomputedHash.equals(receipt.getReceiptHash())) {
				status = EntryStatus.HASH_MISMATCH;
			} else if (!expectedPrevHash.equals(receipt.getPrevReceiptHash())) {
				status = EntryStatus.PREV_HASH_MISMATCH;
			} else {
				status = recordConsistencyStatus(domainId, receipt, state);
			}
			if (status != EntryStatus.OK) {
				ok = false;
			}
			entries.add(new Entry(seq, state, status));
			expectedPrevHash = receipt.getReceiptHash();
		}

		return new ChainVerificationReport(ok, headSequence, entries, truncated);
	}

	private EntryStatus recordConsistencyStatus(String domainId, TraceReceiptEntry receipt, TraceReceiptState state) {
		RecordIdentity identity = new RecordIdentity(domainId, receipt.getProducerId(), receipt.getRecordId());
		Optional<StoredTraceRecord> record = store.findRecord(identity);
		switch (state) {
		case COMMITTED:
			if (!record.isPresent()) {
				return EntryStatus.RECORD_MISSING;
			}
			if (!record.get().getContentDigest().equals(receipt.getContentDigest())) {
				return EntryStatus.DIGEST_MISMATCH;
			}
			return EntryStatus.OK;
		case VOID:
			return record.isPresent() ? EntryStatus.RECORD_UNEXPECTED : EntryStatus.OK;
		case PENDING:
		default:
			return EntryStatus.OK;
		}
	}

}
