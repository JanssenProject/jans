/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import java.util.Objects;

/**
 * The inputs of one receipt in {@link TraceReceiptChain#runWithRange}: the same values
 * {@link TraceReceiptChain#claim} takes for a single record.
 *
 * @author Yuriy Movchan
 */
public final class ReceiptBatchItem {

	private final String producerId;

	private final String recordId;

	private final String recordKey;

	private final String contentDigest;

	private final long receivedAtMs;

	public ReceiptBatchItem(String producerId, String recordId, String recordKey, String contentDigest,
			long receivedAtMs) {
		this.producerId = Objects.requireNonNull(producerId, "producerId");
		this.recordId = Objects.requireNonNull(recordId, "recordId");
		this.recordKey = Objects.requireNonNull(recordKey, "recordKey");
		this.contentDigest = Objects.requireNonNull(contentDigest, "contentDigest");
		this.receivedAtMs = receivedAtMs;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getRecordId() {
		return recordId;
	}

	public String getRecordKey() {
		return recordKey;
	}

	public String getContentDigest() {
		return contentDigest;
	}

	public long getReceivedAtMs() {
		return receivedAtMs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReceiptBatchItem)) {
			return false;
		}
		ReceiptBatchItem other = (ReceiptBatchItem) o;
		return receivedAtMs == other.receivedAtMs && producerId.equals(other.producerId)
				&& recordId.equals(other.recordId) && recordKey.equals(other.recordKey)
				&& contentDigest.equals(other.contentDigest);
	}

	@Override
	public int hashCode() {
		return Objects.hash(producerId, recordId, recordKey, contentDigest, receivedAtMs);
	}

	@Override
	public String toString() {
		return "ReceiptBatchItem{producerId=" + producerId + ", recordId=" + recordId + ", receivedAtMs="
				+ receivedAtMs + "}";
	}

}
