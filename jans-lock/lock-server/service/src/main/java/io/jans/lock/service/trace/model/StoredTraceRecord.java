/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A fully-assembled TRACE record as the store layer sees it (design §10, T-1). This is
 * <strong>not</strong> the {@code TraceRecordEntry} ORM entity (task 03) — it carries the same
 * information in domain terms so both {@code InMemoryTraceStore} (this task) and the ORM store
 * (task 13) share one contract.
 *
 * <p>{@link #equals(Object)}/{@link #hashCode()} are defined on {@link #getIdentity()} only, the
 * record's unique identity; use field-by-field comparison in tests that need to assert on the
 * full content. The raw {@link #getAssertionRaw()} text is carried verbatim and is never
 * re-serialized by the store (design rule 4, immutability of evidence).
 *
 * @author Yuriy Movchan
 */
public final class StoredTraceRecord {

	private final RecordIdentity identity;

	private final String assertionRaw;

	private final String contentDigest;

	private final VerificationResult verification;

	private final ReceiptEntry receipt;

	private final IngestionFlags flags;

	private final ExecutionIdentity execution;

	private final ChainPosition chainPosition;

	private final String prevRecordHash;

	private final List<String> capabilityIds;

	private final List<TokenRef> tokenRefs;

	private final String eventKind;

	private final long signedAt;

	private final String nodeId;

	private final long createdAtMs;

	public StoredTraceRecord(RecordIdentity identity, String assertionRaw, String contentDigest,
			VerificationResult verification, ReceiptEntry receipt, IngestionFlags flags, ExecutionIdentity execution,
			ChainPosition chainPosition, String prevRecordHash, List<String> capabilityIds, List<TokenRef> tokenRefs,
			String eventKind, long signedAt, String nodeId, long createdAtMs) {
		this.identity = Objects.requireNonNull(identity, "identity");
		this.assertionRaw = Objects.requireNonNull(assertionRaw, "assertionRaw");
		this.contentDigest = Objects.requireNonNull(contentDigest, "contentDigest");
		this.verification = Objects.requireNonNull(verification, "verification");
		this.receipt = Objects.requireNonNull(receipt, "receipt");
		this.flags = new IngestionFlags(Objects.requireNonNull(flags, "flags"));
		this.execution = Objects.requireNonNull(execution, "execution");
		this.chainPosition = Objects.requireNonNull(chainPosition, "chainPosition");
		this.prevRecordHash = Objects.requireNonNull(prevRecordHash, "prevRecordHash");
		this.capabilityIds = capabilityIds == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(capabilityIds));
		this.tokenRefs = tokenRefs == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(tokenRefs));
		this.eventKind = Objects.requireNonNull(eventKind, "eventKind");
		this.signedAt = signedAt;
		this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
		this.createdAtMs = createdAtMs;
	}

	public RecordIdentity getIdentity() {
		return identity;
	}

	public String getAssertionRaw() {
		return assertionRaw;
	}

	public String getContentDigest() {
		return contentDigest;
	}

	public VerificationResult getVerification() {
		return verification;
	}

	public ReceiptEntry getReceipt() {
		return receipt;
	}

	/**
	 * @return an independent copy of the current flags; mutating it never affects this record
	 */
	public IngestionFlags getFlags() {
		return flags.copy();
	}

	public ExecutionIdentity getExecution() {
		return execution;
	}

	public ChainPosition getChainPosition() {
		return chainPosition;
	}

	public String getPrevRecordHash() {
		return prevRecordHash;
	}

	/**
	 * @return an unmodifiable list, never {@code null}
	 */
	public List<String> getCapabilityIds() {
		return capabilityIds;
	}

	/**
	 * @return an unmodifiable list, never {@code null}
	 */
	public List<TokenRef> getTokenRefs() {
		return tokenRefs;
	}

	public String getEventKind() {
		return eventKind;
	}

	public long getSignedAt() {
		return signedAt;
	}

	public String getNodeId() {
		return nodeId;
	}

	public long getCreatedAtMs() {
		return createdAtMs;
	}

	/**
	 * @return an independent deep copy of this record (defensive copy of the mutable
	 *         {@link IngestionFlags} and the two lists; everything else is already immutable)
	 */
	public StoredTraceRecord copy() {
		return new StoredTraceRecord(identity, assertionRaw, contentDigest, verification, receipt, flags, execution,
				chainPosition, prevRecordHash, capabilityIds, tokenRefs, eventKind, signedAt, nodeId, createdAtMs);
	}

	/**
	 * @return an independent copy of this record with {@link #getFlags()} replaced; used by the
	 *         store to apply {@code updateRecordFlags} without a full re-construction call site
	 */
	public StoredTraceRecord withFlags(IngestionFlags newFlags) {
		return new StoredTraceRecord(identity, assertionRaw, contentDigest, verification, receipt, newFlags,
				execution, chainPosition, prevRecordHash, capabilityIds, tokenRefs, eventKind, signedAt, nodeId,
				createdAtMs);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StoredTraceRecord)) {
			return false;
		}
		StoredTraceRecord other = (StoredTraceRecord) o;
		return identity.equals(other.identity);
	}

	@Override
	public int hashCode() {
		return identity.hashCode();
	}

	@Override
	public String toString() {
		return "StoredTraceRecord [identity=" + identity + ", contentDigest=" + contentDigest + ", receipt="
				+ receipt + ", flags=" + flags + ", execution=" + execution + ", chainPosition=" + chainPosition
				+ ", eventKind=" + eventKind + ", signedAt=" + signedAt + ", nodeId=" + nodeId + ", createdAtMs="
				+ createdAtMs + "]";
	}

}
