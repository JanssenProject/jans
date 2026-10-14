/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.Objects;

/**
 * The outcome of a successful {@link TraceReceiptChain#claim} call: the allocated
 * {@code receipt_sequence}, the {@code prev_receipt_hash} it was computed against, the resulting
 * {@code receipt_hash}, and the {@code received_at} (epoch milliseconds) the caller supplied —
 * everything {@code TraceIngestionService} (task 19) needs to build the receipt row it then
 * carries on the record itself (design decision D-8 steps 2-5).
 *
 * @author Yuriy Movchan
 */
public final class ReceiptClaim {

	private final long seq;

	private final String prevReceiptHash;

	private final String receiptHash;

	private final long receivedAtMs;

	public ReceiptClaim(long seq, String prevReceiptHash, String receiptHash, long receivedAtMs) {
		this.seq = seq;
		this.prevReceiptHash = Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");
		this.receiptHash = Objects.requireNonNull(receiptHash, "receiptHash");
		this.receivedAtMs = receivedAtMs;
	}

	public long getSeq() {
		return seq;
	}

	public String getPrevReceiptHash() {
		return prevReceiptHash;
	}

	public String getReceiptHash() {
		return receiptHash;
	}

	public long getReceivedAtMs() {
		return receivedAtMs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReceiptClaim)) {
			return false;
		}
		ReceiptClaim other = (ReceiptClaim) o;
		return seq == other.seq && receivedAtMs == other.receivedAtMs && prevReceiptHash.equals(other.prevReceiptHash)
				&& receiptHash.equals(other.receiptHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(seq, prevReceiptHash, receiptHash, receivedAtMs);
	}

	@Override
	public String toString() {
		return "ReceiptClaim [seq=" + seq + ", prevReceiptHash=" + prevReceiptHash + ", receiptHash=" + receiptHash
				+ ", receivedAtMs=" + receivedAtMs + "]";
	}

}
