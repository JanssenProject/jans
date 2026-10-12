/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.jans.lock.model.trace.entity.TraceReceiptState;

/**
 * Diagnostic result of {@link TraceReceiptChain#verifyDomain} (design §9): one {@link Entry} per
 * receipt-chain position from sequence 1 up to the domain's head (or {@code maxEntries},
 * whichever is smaller), reporting density, hash recomputation, {@code prev} linkage and, for a
 * {@code COMMITTED}/{@code VOID} position, whether the matching record row exists with the
 * expected {@code content_digest} (design decision D-8, "state is not part of the receipt hash,
 * so VOID tombstones keep the chain dense and verifiable").
 *
 * <p>Used by tests and the manual verification script; not part of any request path.
 *
 * @author Yuriy Movchan
 */
public final class ChainVerificationReport {

	/**
	 * Per-position outcome. {@code OK} covers a correctly linked {@code PENDING} position too —
	 * {@code PENDING} is reported via {@link Entry#getState()}, not as a distinct failure status.
	 */
	public enum EntryStatus {
		/** Hash and {@code prev} linkage check out; record presence matches the receipt state. */
		OK,
		/** No receipt row exists at this sequence, ahead of the domain's head (a density break). */
		MISSING_SEQUENCE,
		/** The stored {@code receipt_hash} does not match the recomputed hash for this position. */
		HASH_MISMATCH,
		/** The stored {@code prev_receipt_hash} does not match the previous position's hash. */
		PREV_HASH_MISMATCH,
		/** {@code COMMITTED} but no record row exists with this receipt's identity. */
		RECORD_MISSING,
		/** {@code COMMITTED} and the record exists, but its {@code content_digest} differs. */
		DIGEST_MISMATCH,
		/** {@code VOID} but a record row exists with this receipt's identity (tombstone violated). */
		RECORD_UNEXPECTED
	}

	/** One receipt-chain position as observed by {@link TraceReceiptChain#verifyDomain}. */
	public static final class Entry {

		private final long seq;

		private final TraceReceiptState state;

		private final EntryStatus status;

		public Entry(long seq, TraceReceiptState state, EntryStatus status) {
			this.seq = seq;
			this.state = state;
			this.status = Objects.requireNonNull(status, "status");
		}

		public long getSeq() {
			return seq;
		}

		/** {@code null} only for {@link EntryStatus#MISSING_SEQUENCE}. */
		public TraceReceiptState getState() {
			return state;
		}

		public EntryStatus getStatus() {
			return status;
		}

		/** @return {@code true} for {@link EntryStatus#OK}, {@code false} for every other status */
		public boolean isOk() {
			return status == EntryStatus.OK;
		}

		@Override
		public String toString() {
			return "Entry [seq=" + seq + ", state=" + state + ", status=" + status + "]";
		}

	}

	private final boolean ok;

	private final long headSequence;

	private final List<Entry> entries;

	private final boolean truncated;

	public ChainVerificationReport(boolean ok, long headSequence, List<Entry> entries, boolean truncated) {
		this.ok = ok;
		this.headSequence = headSequence;
		this.entries = Collections.unmodifiableList(entries);
		this.truncated = truncated;
	}

	/** @return {@code true} only if every entry's {@link Entry#isOk()} is {@code true} */
	public boolean isOk() {
		return ok;
	}

	/** @return the domain's receipt-chain head sequence, or {@code 0} for an empty domain */
	public long getHeadSequence() {
		return headSequence;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	/** @return {@code true} if {@code headSequence} exceeded {@code maxEntries} and the walk stopped early */
	public boolean isTruncated() {
		return truncated;
	}

	@Override
	public String toString() {
		return "ChainVerificationReport [ok=" + ok + ", headSequence=" + headSequence + ", entries=" + entries
				+ ", truncated=" + truncated + "]";
	}

}
