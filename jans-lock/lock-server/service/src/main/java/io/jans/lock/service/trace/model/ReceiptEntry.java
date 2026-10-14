/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * The receipt-chain data carried alongside a {@link StoredTraceRecord} (design §9, design decision
 * D-8): the sequence number this record was accepted at, when, and the hash-chain link. This is a
 * plain value copy of the corresponding {@code jansTraceReceipt*} attributes on the record row —
 * it is not the allocation claim itself (see {@link ReceiptRow}).
 *
 * @author Yuriy Movchan
 */
public final class ReceiptEntry {

	private final long receiptSequence;

	private final long receivedAtMs;

	private final String prevReceiptHash;

	private final String receiptHash;

	public ReceiptEntry(long receiptSequence, long receivedAtMs, String prevReceiptHash, String receiptHash) {
		this.receiptSequence = receiptSequence;
		this.receivedAtMs = receivedAtMs;
		this.prevReceiptHash = Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");
		this.receiptHash = Objects.requireNonNull(receiptHash, "receiptHash");
	}

	public long getReceiptSequence() {
		return receiptSequence;
	}

	public long getReceivedAtMs() {
		return receivedAtMs;
	}

	public String getPrevReceiptHash() {
		return prevReceiptHash;
	}

	public String getReceiptHash() {
		return receiptHash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReceiptEntry)) {
			return false;
		}
		ReceiptEntry other = (ReceiptEntry) o;
		return receiptSequence == other.receiptSequence && receivedAtMs == other.receivedAtMs
				&& prevReceiptHash.equals(other.prevReceiptHash) && receiptHash.equals(other.receiptHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(receiptSequence, receivedAtMs, prevReceiptHash, receiptHash);
	}

	@Override
	public String toString() {
		return "ReceiptEntry [receiptSequence=" + receiptSequence + ", receivedAtMs=" + receivedAtMs
				+ ", prevReceiptHash=" + prevReceiptHash + ", receiptHash=" + receiptHash + "]";
	}

}
