/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One element of {@code POST /audit/trace/bulk}'s {@code results} array (design §11.2, decision
 * D-22): the submitted array's position and whether that item was accepted. An accepted entry
 * carries the single-record acceptance members inline ({@code producer_id} ... {@code late_flag});
 * a rejected entry carries {@link #getError()} plus {@code producer_id}/{@code record_id} when they
 * were readable from the element. Members that do not apply to the entry's outcome are omitted.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "One item's outcome within a TRACE bulk ingestion response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "index", "accepted", "producer_id", "record_id", "content_digest", "receipt_sequence",
		"received_at", "idempotent_replay", "coverage_gap_flag", "chain_link_failure_flag", "equivocation_flag",
		"late_flag", "error" })
public class TraceBulkResultEntry {

	@JsonProperty("index")
	@Schema(description = "Position of this assertion in the submitted array", example = "0")
	private int index;

	@JsonProperty("accepted")
	@Schema(description = "True when this item was verified and ingested")
	private boolean accepted;

	@JsonProperty("producer_id")
	@Schema(description = "Opaque stable producer id. Always present when accepted; present when rejected only if the element carried a readable top-level string producer", example = "cedarling-fleet-1")
	private String producerId;

	@JsonProperty("record_id")
	@Schema(description = "Signed record id. Always present when accepted; present when rejected only if the element carried a readable top-level string record_id", example = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41")
	private String recordId;

	@JsonProperty("content_digest")
	@Schema(description = "Present when accepted: sha256:<hex> digest of the full signed assertion", example = "sha256:abc123...")
	private String contentDigest;

	@JsonProperty("receipt_sequence")
	@Schema(description = "Present when accepted: position in the domain's receipt chain", example = "108422")
	private Long receiptSequence;

	@JsonProperty("received_at")
	@Schema(description = "Present when accepted: RFC 3339 UTC timestamp Lock received the record", example = "2026-06-11T00:41:02.123Z")
	private String receivedAt;

	@JsonProperty("idempotent_replay")
	@Schema(description = "Present when accepted: true when this identity already existed with the same content digest")
	private Boolean idempotentReplay;

	@JsonProperty("coverage_gap_flag")
	@Schema(description = "Present when accepted: true when this record's predecessor in its producer chain is missing")
	private Boolean coverageGapFlag;

	@JsonProperty("chain_link_failure_flag")
	@Schema(description = "Present when accepted: true when this record's prev_record_hash does not match its predecessor")
	private Boolean chainLinkFailureFlag;

	@JsonProperty("equivocation_flag")
	@Schema(description = "Present when accepted: true when another record already occupies this producer-chain position")
	private Boolean equivocationFlag;

	@JsonProperty("late_flag")
	@Schema(description = "Present when accepted: true when the record arrived after the configured lateness threshold")
	private Boolean lateFlag;

	@JsonProperty("error")
	@Schema(description = "Present when accepted=false: the structured error for this item only")
	private TraceErrorResponse error;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

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

	public Long getReceiptSequence() {
		return receiptSequence;
	}

	public void setReceiptSequence(Long receiptSequence) {
		this.receiptSequence = receiptSequence;
	}

	public String getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(String receivedAt) {
		this.receivedAt = receivedAt;
	}

	public Boolean getIdempotentReplay() {
		return idempotentReplay;
	}

	public void setIdempotentReplay(Boolean idempotentReplay) {
		this.idempotentReplay = idempotentReplay;
	}

	public Boolean getCoverageGapFlag() {
		return coverageGapFlag;
	}

	public void setCoverageGapFlag(Boolean coverageGapFlag) {
		this.coverageGapFlag = coverageGapFlag;
	}

	public Boolean getChainLinkFailureFlag() {
		return chainLinkFailureFlag;
	}

	public void setChainLinkFailureFlag(Boolean chainLinkFailureFlag) {
		this.chainLinkFailureFlag = chainLinkFailureFlag;
	}

	public Boolean getEquivocationFlag() {
		return equivocationFlag;
	}

	public void setEquivocationFlag(Boolean equivocationFlag) {
		this.equivocationFlag = equivocationFlag;
	}

	public Boolean getLateFlag() {
		return lateFlag;
	}

	public void setLateFlag(Boolean lateFlag) {
		this.lateFlag = lateFlag;
	}

	public TraceErrorResponse getError() {
		return error;
	}

	public void setError(TraceErrorResponse error) {
		this.error = error;
	}

}
