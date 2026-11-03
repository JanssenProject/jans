/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@code ingestion} member of a TRACE record envelope (design §10): the
 * receipt-chain position and the four Lock-derived flags (eventually consistent).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Receipt-chain position and ingestion flags for a TRACE record")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "receipt_sequence", "received_at", "prev_receipt_hash", "receipt_hash", "receipt_state" })
public class TraceIngestionResponse extends TraceFlagsResponse {

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

	@JsonProperty("receipt_state")
	@Schema(description = "State of this record's receipt entry: COMMITTED, or PENDING while the claim is not yet settled", example = "COMMITTED")
	private String receiptState;

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

	public String getReceiptState() {
		return receiptState;
	}

	public void setReceiptState(String receiptState) {
		this.receiptState = receiptState;
	}

}
