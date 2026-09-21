/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.Objects;

import io.jans.lock.model.trace.entity.TraceReceiptState;

/**
 * A receipt-chain position allocation claim (design §9, T-2, design decision D-8). Unique on
 * {@code (domainId, receiptSequence)} — key type {@code rcpt}. Only {@link #getState()} may change
 * after creation ({@code updateReceiptState}); {@link #withState(TraceReceiptState)} produces the
 * updated copy.
 *
 * @author Yuriy Movchan
 */
public final class ReceiptRow {

	private final String domainId;

	private final long receiptSequence;

	private final long receivedAtMs;

	private final String producerId;

	private final String recordId;

	private final String recordKey;

	private final String contentDigest;

	private final String prevReceiptHash;

	private final String receiptHash;

	private final TraceReceiptState state;

	private final String nodeId;

	public ReceiptRow(String domainId, long receiptSequence, long receivedAtMs, String producerId, String recordId,
			String recordKey, String contentDigest, String prevReceiptHash, String receiptHash,
			TraceReceiptState state, String nodeId) {
		this.domainId = Objects.requireNonNull(domainId, "domainId");
		this.receiptSequence = receiptSequence;
		this.receivedAtMs = receivedAtMs;
		this.producerId = Objects.requireNonNull(producerId, "producerId");
		this.recordId = Objects.requireNonNull(recordId, "recordId");
		this.recordKey = Objects.requireNonNull(recordKey, "recordKey");
		this.contentDigest = Objects.requireNonNull(contentDigest, "contentDigest");
		this.prevReceiptHash = Objects.requireNonNull(prevReceiptHash, "prevReceiptHash");
		this.receiptHash = Objects.requireNonNull(receiptHash, "receiptHash");
		this.state = Objects.requireNonNull(state, "state");
		this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
	}

	public String getDomainId() {
		return domainId;
	}

	public long getReceiptSequence() {
		return receiptSequence;
	}

	public long getReceivedAtMs() {
		return receivedAtMs;
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

	public String getPrevReceiptHash() {
		return prevReceiptHash;
	}

	public String getReceiptHash() {
		return receiptHash;
	}

	public TraceReceiptState getState() {
		return state;
	}

	public String getNodeId() {
		return nodeId;
	}

	/**
	 * @return an independent copy of this row with {@link #getState()} replaced
	 */
	public ReceiptRow withState(TraceReceiptState newState) {
		return new ReceiptRow(domainId, receiptSequence, receivedAtMs, producerId, recordId, recordKey, contentDigest,
				prevReceiptHash, receiptHash, newState, nodeId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReceiptRow)) {
			return false;
		}
		ReceiptRow other = (ReceiptRow) o;
		return receiptSequence == other.receiptSequence && domainId.equals(other.domainId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(domainId, receiptSequence);
	}

	@Override
	public String toString() {
		return "ReceiptRow [domainId=" + domainId + ", receiptSequence=" + receiptSequence + ", receivedAtMs="
				+ receivedAtMs + ", producerId=" + producerId + ", recordId=" + recordId + ", recordKey=" + recordKey
				+ ", contentDigest=" + contentDigest + ", prevReceiptHash=" + prevReceiptHash + ", receiptHash="
				+ receiptHash + ", state=" + state + ", nodeId=" + nodeId + "]";
	}

}
