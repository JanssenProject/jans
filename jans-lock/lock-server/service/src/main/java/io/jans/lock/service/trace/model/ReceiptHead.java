/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

/**
 * The current head of a domain's receipt chain: the highest {@code receiptSequence} in any state
 * (design decision D-8 step 2) and its {@code receiptHash}, used to compute the next receipt's
 * {@code prev_receipt_hash}.
 *
 * @author Yuriy Movchan
 */
public final class ReceiptHead {

	private final long receiptSequence;

	private final String receiptHash;

	public ReceiptHead(long receiptSequence, String receiptHash) {
		this.receiptSequence = receiptSequence;
		this.receiptHash = Objects.requireNonNull(receiptHash, "receiptHash");
	}

	public long getReceiptSequence() {
		return receiptSequence;
	}

	public String getReceiptHash() {
		return receiptHash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReceiptHead)) {
			return false;
		}
		ReceiptHead other = (ReceiptHead) o;
		return receiptSequence == other.receiptSequence && receiptHash.equals(other.receiptHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(receiptSequence, receiptHash);
	}

	@Override
	public String toString() {
		return "ReceiptHead [receiptSequence=" + receiptSequence + ", receiptHash=" + receiptHash + "]";
	}

}
