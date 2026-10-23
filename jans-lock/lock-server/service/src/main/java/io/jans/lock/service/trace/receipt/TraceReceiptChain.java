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
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.Entry;
import io.jans.lock.service.trace.receipt.ChainVerificationReport.EntryStatus;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Allocates per-domain receipt-chain positions, hash-chains them safely across several Lock
 * nodes and settles them (design §9). jans-orm offers no multi-entry transactions and no
 * sequences, but it does offer per-entry compare-and-swap ({@code updateWithVersion}, commit
 * {@code 393cd48bac9}). The protocol selected under those persistence APIs is not the only
 * possible one:
 * <ul>
 * <li>Claim: correctness of the position comes from {@link TraceStore#insertReceipt} rejecting a
 * second claim at the same {@code (domainId, seq)}; the per-domain {@link ReentrantLock} below only
 * reduces same-node contention and is never load bearing.</li>
 * <li>Settlement: a claim leaves {@code PENDING} once, to {@code COMMITTED} or {@code VOID}, by
 * compare-and-swap ({@link TraceStore#settleReceipt}). Both are terminal; re-applying the stored
 * state is a no-op and the other state is a {@link ReceiptSettlement#CONFLICT}.</li>
 * <li>Authoritative read: a claim {@code (domain, seq)} is satisfied if and only if the record row
 * for its identity exists and its embedded receipt sequence equals {@code seq} (and its
 * {@code content_digest} equals the receipt's). Receipt state is a derived fact about the record
 * row; see {@link #isClaimSatisfied}.</li>
 * <li>Fencing: a worker must not insert its record after {@code TraceReceiptRepairTimer} had the
 * right to void its claim; see {@link #isFenceExpired}. The fence narrows that window, it cannot
 * close it.</li>
 * <li>A later claim binds the {@code receipt_hash} of the head in any state; the hash does not
 * depend on the state, so the chain stays valid whatever an unsettled position becomes.</li>
 * </ul>
 *
 * <p>{@link #claim} is the only method that allocates a new sequence; {@link #commit} and
 * {@link #voidClaim} settle an already-allocated one and never throw: a store failure is logged
 * at WARN, reported as {@link ReceiptSettlement#FAILED} and left for
 * {@code TraceReceiptRepairTimer} to resolve later.
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

	@Inject
	private TraceCryptoProvider cryptoProvider;

	private final ConcurrentHashMap<String, ReentrantLock> domainLocks = new ConcurrentHashMap<>();

	/**
	 * Allocates the next receipt-chain position for {@code domainId} and inserts it as a
	 * {@code PENDING} claim. Retries on contention with another
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
				String receiptHash = cryptoProvider.receiptHash(domainId, seq, receivedAtMs, producerId, recordId,
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
					return new ReceiptClaim(seq, prevReceiptHash, receiptHash, receivedAtMs, System.currentTimeMillis());
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
	 * Settles a claim as {@code COMMITTED} once the record row carrying it is visible. Best-effort: a
	 * store failure is logged at WARN, not thrown, and reported as {@link ReceiptSettlement#FAILED};
	 * the repair timer resolves a stuck {@code PENDING} row later. {@link ReceiptSettlement#CONFLICT}
	 * means the claim is already {@code VOID} and is left to the caller to handle.
	 */
	public ReceiptSettlement commit(String domainId, long seq) {
		return settle(domainId, seq, TraceReceiptState.COMMITTED);
	}

	/**
	 * Settles a claim as {@code VOID} after the matching record insert lost a cross-node identity
	 * race or otherwise failed. Best-effort, like {@link #commit}.
	 * {@link ReceiptSettlement#CONFLICT} means the claim is already {@code COMMITTED}.
	 */
	public ReceiptSettlement voidClaim(String domainId, long seq) {
		return settle(domainId, seq, TraceReceiptState.VOID);
	}

	private ReceiptSettlement settle(String domainId, long seq, TraceReceiptState target) {
		try {
			ReceiptSettlement outcome = store.settleReceipt(domainId, seq, target);
			if (outcome == ReceiptSettlement.NOT_FOUND) {
				log.warn("Could not settle receipt as {}: no row at domain={}, seq={}", target, domainId, seq);
			}
			return outcome;
		} catch (TraceStorageException ex) {
			log.warn("Failed to settle receipt as {}: domain={}, seq={}", target, domainId, seq, ex);
			return ReceiptSettlement.FAILED;
		}
	}

	/**
	 * The authoritative read for a claim: {@code true} if and only if the record row for the
	 * receipt's identity exists, carries this receipt's sequence and has the receipt's
	 * {@code content_digest}. A record under the same identity with another sequence belongs to
	 * another claim (a lost duplicate-identity race) and does not satisfy this one.
	 */
	public boolean isClaimSatisfied(TraceReceiptEntry receipt) {
		RecordIdentity identity = new RecordIdentity(receipt.getDomainId(), receipt.getProducerId(),
				receipt.getRecordId());
		return store.findRecord(identity)
				.filter(record -> carriesClaim(record, receipt.getReceiptSeq(), receipt.getContentDigest())).isPresent();
	}

	/** @return {@code true} if {@code record} carries receipt sequence {@code seq} and {@code contentDigest} */
	public static boolean carriesClaim(StoredTraceRecord record, long seq, String contentDigest) {
		return record.getReceipt().getReceiptSequence() == seq && record.getContentDigest().equals(contentDigest);
	}

	/**
	 * The ingest fence: how long after {@link ReceiptClaim#getClaimedAtMs()} a worker may still insert
	 * its record. Derived as half of {@code pendingReceiptTimeoutSeconds}, so it stays strictly below
	 * the age at which {@code TraceReceiptRepairTimer} may void the claim and leaves the other half
	 * as margin for clock difference between nodes and a database round trip. The timer ages a claim
	 * from {@code received_at}, which precedes the claim, so the margin also absorbs the time between
	 * receipt of the request and the claim.
	 */
	public long fenceMs() {
		return appConfiguration.getTraceConfiguration().getPendingReceiptTimeoutSeconds() * 1000L / 2;
	}

	/**
	 * @return {@code true} once this node's clock is past {@code claimedAtMs + fenceMs()}: the worker
	 *         must not insert its record any more. Narrows, but cannot close, the window in which a
	 *         record is inserted after the claim was voided.
	 */
	public boolean isFenceExpired(ReceiptClaim claim) {
		return System.currentTimeMillis() > claim.getClaimedAtMs() + fenceMs();
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
			String recomputedHash = null;
			try {
				recomputedHash = cryptoProvider.receiptHash(domainId, seq, receipt.getReceivedAtMs(),
						receipt.getProducerId(), receipt.getRecordId(), receipt.getContentDigest(),
						receipt.getPrevReceiptHash());
			} catch (TraceCryptoException ex) {
				if (TraceCryptoException.REASON_CRYPTO_UNAVAILABLE.equals(ex.getReason())) {
					throw ex;
				}
				log.debug("receipt hash not recomputable: domain={}, seq={}, reason={}", domainId, seq,
						ex.getReason());
			}
			EntryStatus status;
			if (recomputedHash == null || !recomputedHash.equals(receipt.getReceiptHash())) {
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
			if (record.get().getReceipt().getReceiptSequence() != receipt.getReceiptSeq()) {
				return EntryStatus.SEQUENCE_MISMATCH;
			}
			if (!record.get().getContentDigest().equals(receipt.getContentDigest())) {
				return EntryStatus.DIGEST_MISMATCH;
			}
			return EntryStatus.OK;
		case VOID:
			// A record under this identity that carries another sequence belongs to the claim that won
			// the identity race; it does not make this tombstone unexpected.
			return record.isPresent()
					&& carriesClaim(record.get(), receipt.getReceiptSeq(), receipt.getContentDigest())
							? EntryStatus.RECORD_UNEXPECTED
							: EntryStatus.OK;
		case PENDING:
		default:
			return EntryStatus.OK;
		}
	}

}
