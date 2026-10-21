/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.StoredTraceRecord;

/**
 * The outcome of {@link TraceIngestionService#ingest}: the design §11.1 success payload for
 * {@code POST /audit/trace}, built from a {@link StoredTraceRecord} that is either the row just
 * inserted or, for an idempotent replay or a cross-node race resolved as a replay, the row that
 * already held the identity.
 *
 * <p>Only ever constructed via {@link #fromStored}: every rejection path (conflict, validation,
 * crypto, storage) throws instead of returning an {@link AcceptanceResult}, so {@link #isAccepted()}
 * is always {@code true}.
 *
 * @author Yuriy Movchan
 */
public final class AcceptanceResult {

	private static final boolean ACCEPTED = true;

	private final String producerId;

	private final String recordId;

	private final String contentDigest;

	private final long receiptSequence;

	private final long receivedAtMs;

	private final boolean idempotentReplay;

	private final IngestionFlags flags;

	private AcceptanceResult(String producerId, String recordId, String contentDigest, long receiptSequence,
			long receivedAtMs, boolean idempotentReplay, IngestionFlags flags) {
		this.producerId = producerId;
		this.recordId = recordId;
		this.contentDigest = contentDigest;
		this.receiptSequence = receiptSequence;
		this.receivedAtMs = receivedAtMs;
		this.idempotentReplay = idempotentReplay;
		this.flags = flags.copy();
	}

	/**
	 * @param stored the record that now holds this identity (just inserted, or the pre-existing row
	 *               on a replay)
	 * @param replay whether this call is reporting an idempotent replay rather than a fresh insert
	 */
	public static AcceptanceResult fromStored(StoredTraceRecord stored, boolean replay) {
		return new AcceptanceResult(stored.getIdentity().getProducerId(), stored.getIdentity().getRecordId(),
				stored.getContentDigest(), stored.getReceipt().getReceiptSequence(),
				stored.getReceipt().getReceivedAtMs(), replay, stored.getFlags());
	}

	/**
	 * @return always {@code true}; every rejection path throws instead of returning a result
	 */
	public boolean isAccepted() {
		return ACCEPTED;
	}

	public String getProducerId() {
		return producerId;
	}

	public String getRecordId() {
		return recordId;
	}

	public String getContentDigest() {
		return contentDigest;
	}

	public long getReceiptSequence() {
		return receiptSequence;
	}

	public long getReceivedAtMs() {
		return receivedAtMs;
	}

	public boolean isIdempotentReplay() {
		return idempotentReplay;
	}

	public boolean isCoverageGapFlag() {
		return flags.isCoverageGap();
	}

	public boolean isChainLinkFailureFlag() {
		return flags.isChainLinkFailure();
	}

	public boolean isEquivocationFlag() {
		return flags.isEquivocation();
	}

	public boolean isLateFlag() {
		return flags.isLate();
	}

	public boolean isPolicyMismatchFlag() {
		return flags.isPolicyMismatch();
	}

	public boolean isReservedMemberFlag() {
		return flags.isReservedMember();
	}

	@Override
	public String toString() {
		return "AcceptanceResult [producerId=" + producerId + ", recordId=" + recordId + ", contentDigest="
				+ contentDigest + ", receiptSequence=" + receiptSequence + ", receivedAtMs=" + receivedAtMs
				+ ", idempotentReplay=" + idempotentReplay + ", flags=" + flags + "]";
	}

}
