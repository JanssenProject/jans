/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import java.util.Collections;
import java.util.List;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.model.VerificationResult;

/**
 * Test-only fluent builder of {@link StoredTraceRecord}s, so a test can seed a
 * {@code TraceStore} with predecessor/peer/successor rows directly, without going through
 * parsing, verification or ingestion. Shared by the correlation (task 17), receipt-chain (task
 * 18) and ingestion (task 19) tests.
 *
 * <p>Every field has a usable default except {@link #recordId(String)} and
 * {@link #contentDigest(String)}, which a test must always set explicitly since they are exactly
 * the values correlation and chain-link logic compare.
 */
public final class StoredTraceRecordFactory {

	private String domainId = "domain-1";

	private String producerId = "producer-1/1.0.0";

	private String recordId;

	private String executionAuthority = "spiffe://example.org/agent/test";

	private String traceExecutionId = "exec-1";

	private String producerInstanceId = "instance-1";

	private String producerChainId = "chain-1";

	private long sequenceNumber = 1L;

	private String prevRecordHash = TraceConstants.ZERO_HASH;

	private String contentDigest;

	private long receiptSequence = 1L;

	private long receivedAtMs = 1_000L;

	private String prevReceiptHash = TraceConstants.ZERO_HASH;

	private String receiptHash = TraceConstants.ZERO_HASH;

	private List<String> capabilityIds = Collections.emptyList();

	private List<TokenRef> tokenRefs = Collections.emptyList();

	private String eventKind = TraceConstants.EVENT_KIND_CAPABILITY_INVOKED;

	private long signedAt = 500L;

	private String nodeId = "node-1";

	private long createdAtMs = 1_000L;

	private boolean coverageGap;

	private boolean chainLinkFailure;

	private boolean equivocation;

	private boolean late;

	private StoredTraceRecordFactory() {
	}

	public static StoredTraceRecordFactory builder() {
		return new StoredTraceRecordFactory();
	}

	public StoredTraceRecordFactory domainId(String domainId) {
		this.domainId = domainId;
		return this;
	}

	public StoredTraceRecordFactory producerId(String producerId) {
		this.producerId = producerId;
		return this;
	}

	public StoredTraceRecordFactory recordId(String recordId) {
		this.recordId = recordId;
		return this;
	}

	public StoredTraceRecordFactory executionAuthority(String executionAuthority) {
		this.executionAuthority = executionAuthority;
		return this;
	}

	public StoredTraceRecordFactory traceExecutionId(String traceExecutionId) {
		this.traceExecutionId = traceExecutionId;
		return this;
	}

	public StoredTraceRecordFactory producerInstanceId(String producerInstanceId) {
		this.producerInstanceId = producerInstanceId;
		return this;
	}

	public StoredTraceRecordFactory producerChainId(String producerChainId) {
		this.producerChainId = producerChainId;
		return this;
	}

	public StoredTraceRecordFactory sequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
		return this;
	}

	public StoredTraceRecordFactory prevRecordHash(String prevRecordHash) {
		this.prevRecordHash = prevRecordHash;
		return this;
	}

	/**
	 * Convenience for a genesis row: {@code sequence_number = 1} and the zero-hash sentinel
	 * (design decision D-5).
	 */
	public StoredTraceRecordFactory genesis() {
		sequenceNumber(1L);
		return prevRecordHash(TraceConstants.ZERO_HASH);
	}

	public StoredTraceRecordFactory contentDigest(String contentDigest) {
		this.contentDigest = contentDigest;
		return this;
	}

	public StoredTraceRecordFactory receiptSequence(long receiptSequence) {
		this.receiptSequence = receiptSequence;
		return this;
	}

	public StoredTraceRecordFactory receivedAtMs(long receivedAtMs) {
		this.receivedAtMs = receivedAtMs;
		return this;
	}

	public StoredTraceRecordFactory prevReceiptHash(String prevReceiptHash) {
		this.prevReceiptHash = prevReceiptHash;
		return this;
	}

	public StoredTraceRecordFactory receiptHash(String receiptHash) {
		this.receiptHash = receiptHash;
		return this;
	}

	public StoredTraceRecordFactory capabilityIds(List<String> capabilityIds) {
		this.capabilityIds = capabilityIds;
		return this;
	}

	public StoredTraceRecordFactory tokenRefs(List<TokenRef> tokenRefs) {
		this.tokenRefs = tokenRefs;
		return this;
	}

	public StoredTraceRecordFactory eventKind(String eventKind) {
		this.eventKind = eventKind;
		return this;
	}

	public StoredTraceRecordFactory signedAt(long signedAt) {
		this.signedAt = signedAt;
		return this;
	}

	public StoredTraceRecordFactory nodeId(String nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public StoredTraceRecordFactory createdAtMs(long createdAtMs) {
		this.createdAtMs = createdAtMs;
		return this;
	}

	public StoredTraceRecordFactory coverageGap(boolean coverageGap) {
		this.coverageGap = coverageGap;
		return this;
	}

	public StoredTraceRecordFactory chainLinkFailure(boolean chainLinkFailure) {
		this.chainLinkFailure = chainLinkFailure;
		return this;
	}

	public StoredTraceRecordFactory equivocation(boolean equivocation) {
		this.equivocation = equivocation;
		return this;
	}

	public StoredTraceRecordFactory late(boolean late) {
		this.late = late;
		return this;
	}

	public StoredTraceRecord build() {
		if (recordId == null) {
			throw new IllegalStateException("recordId must be set");
		}
		if (contentDigest == null) {
			throw new IllegalStateException("contentDigest must be set");
		}

		RecordIdentity identity = new RecordIdentity(domainId, producerId, recordId);
		ExecutionIdentity execution = new ExecutionIdentity(domainId, executionAuthority, traceExecutionId);
		ChainIdentity chainIdentity = new ChainIdentity(domainId, producerId, producerInstanceId, producerChainId);
		ChainPosition chainPosition = new ChainPosition(chainIdentity, sequenceNumber);
		VerificationResult verification = new VerificationResult(true, "kid-1", receivedAtMs, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(receiptSequence, receivedAtMs, prevReceiptHash, receiptHash);
		IngestionFlags flags = new IngestionFlags(coverageGap, chainLinkFailure, equivocation, late);

		return new StoredTraceRecord(identity, "{\"trace\":{}}", contentDigest, verification, receipt, flags,
				execution, chainPosition, prevRecordHash, capabilityIds, tokenRefs, eventKind, signedAt, nodeId,
				createdAtMs);
	}

}
