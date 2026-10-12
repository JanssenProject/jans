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
 * The {@code ingestion} member of a TRACE record envelope (design §10, TRACE MVP task 21): the
 * receipt-chain position and the four Lock-derived flags (design decision D-9, eventually
 * consistent).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Receipt-chain position and ingestion flags for a TRACE record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceIngestionResponse {

	@JsonProperty("receipt_sequence")
	@Schema(description = "Position in the domain's receipt chain", example = "108422")
	private long receiptSequence;

	@JsonProperty("received_at")
	@Schema(description = "RFC 3339 UTC timestamp Lock received the record", example = "2026-06-11T00:41:02.123Z")
	private String receivedAt;

	@JsonProperty("prev_receipt_hash")
	@Schema(description = "sha256:<hex> hash of the previous receipt in the domain's chain")
	private String prevReceiptHash;

	@JsonProperty("receipt_hash")
	@Schema(description = "sha256:<hex> hash of this receipt")
	private String receiptHash;

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

	public String getPrevReceiptHash() {
		return prevReceiptHash;
	}

	public void setPrevReceiptHash(String prevReceiptHash) {
		this.prevReceiptHash = prevReceiptHash;
	}

	public String getReceiptHash() {
		return receiptHash;
	}

	public void setReceiptHash(String receiptHash) {
		this.receiptHash = receiptHash;
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
