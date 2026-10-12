/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@code POST /audit/trace} success body (design §11.1, TRACE MVP task 20): the identity and
 * receipt position the record was accepted at, plus the four ingestion flags fixed at ingestion
 * time. {@code idempotent_replay=true} means these are the values of the row that already held the
 * identity, not a freshly inserted one.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "TRACE record acceptance response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceAcceptanceResponse {

	@JsonProperty("accepted")
	@Schema(description = "Always true; rejections are reported as structured errors instead", example = "true")
	private boolean accepted;

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("record_id")
	@Schema(description = "Signed record id", example = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41")
	private String recordId;

	@JsonProperty("content_digest")
	@Schema(description = "sha256:<hex> digest of the full signed assertion", example = "sha256:abc123...")
	private String contentDigest;

	@JsonProperty("receipt_sequence")
	@Schema(description = "Position in the domain's receipt chain", example = "108422")
	private long receiptSequence;

	@JsonProperty("received_at")
	@Schema(description = "RFC 3339 UTC timestamp Lock received the record", example = "2026-06-11T00:41:02.123Z")
	private String receivedAt;

	@JsonProperty("idempotent_replay")
	@Schema(description = "True when this identity already existed with the same content digest")
	private boolean idempotentReplay;

	@JsonProperty("coverage_gap_flag")
	@Schema(description = "True when this record's predecessor in its producer chain is missing")
	private boolean coverageGapFlag;

	@JsonProperty("chain_link_failure_flag")
	@Schema(description = "True when this record's prev_record_hash does not match its predecessor")
	private boolean chainLinkFailureFlag;

	@JsonProperty("equivocation_flag")
	@Schema(description = "True when another record already occupies this producer-chain position")
	private boolean equivocationFlag;

	@JsonProperty("late_flag")
	@Schema(description = "True when the record arrived after the configured lateness threshold")
	private boolean lateFlag;

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	public String getProducerId() {
		return producerId;
	}

	public void setProducerId(String producerId) {
		this.producerId = producerId;
	}

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getContentDigest() {
		return contentDigest;
	}

	public void setContentDigest(String contentDigest) {
		this.contentDigest = contentDigest;
	}

	public long getReceiptSequence() {
		return receiptSequence;
	}

	public void setReceiptSequence(long receiptSequence) {
		this.receiptSequence = receiptSequence;
	}

	public String getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(String receivedAt) {
		this.receivedAt = receivedAt;
	}

	public boolean isIdempotentReplay() {
		return idempotentReplay;
	}

	public void setIdempotentReplay(boolean idempotentReplay) {
		this.idempotentReplay = idempotentReplay;
	}

	public boolean isCoverageGapFlag() {
		return coverageGapFlag;
	}

	public void setCoverageGapFlag(boolean coverageGapFlag) {
		this.coverageGapFlag = coverageGapFlag;
	}

	public boolean isChainLinkFailureFlag() {
		return chainLinkFailureFlag;
	}

	public void setChainLinkFailureFlag(boolean chainLinkFailureFlag) {
		this.chainLinkFailureFlag = chainLinkFailureFlag;
	}

	public boolean isEquivocationFlag() {
		return equivocationFlag;
	}

	public void setEquivocationFlag(boolean equivocationFlag) {
		this.equivocationFlag = equivocationFlag;
	}

	public boolean isLateFlag() {
		return lateFlag;
	}

	public void setLateFlag(boolean lateFlag) {
		this.lateFlag = lateFlag;
	}

}
