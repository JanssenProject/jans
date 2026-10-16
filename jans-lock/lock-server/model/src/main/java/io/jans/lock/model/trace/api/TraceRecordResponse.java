/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single TRACE record envelope (design §10, §11.2): the stored assertion plus
 * the verification outcome and the Lock-derived ingestion metadata. {@code assertion} is the
 * request body text re-parsed as a JSON value, never re-serialized by hand:
 * Jackson re-emits it, so semantic identity with the originally signed bytes is preserved, not
 * byte-for-byte identity. {@code assertionRaw} carries the exact accepted text (design §10, §14).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "A single TRACE record envelope")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceRecordResponse {

	@JsonProperty("producer_id")
	@Schema(description = "Opaque stable producer id", example = "cedarling-fleet-1")
	private String producerId;

	@JsonProperty("record_id")
	@Schema(description = "Signed record id", example = "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41")
	private String recordId;

	@JsonProperty("assertion")
	@Schema(description = "The complete producer-signed assertion as a parsed JSON value. A re-emission: member order, whitespace and number formatting can differ from the received text. Use assertion_raw for the exact text")
	private JsonNode assertion;

	@JsonProperty("assertion_raw")
	@Schema(description = "The exact accepted assertion text, byte for byte as received. Verify the signature and content_digest over its JCS form")
	private String assertionRaw;

	@JsonProperty("content_digest")
	@Schema(description = "sha256:<hex> digest of the full signed assertion", example = "sha256:abc123...")
	private String contentDigest;

	@JsonProperty("verification")
	@Schema(description = "Signature verification outcome")
	private TraceVerificationResponse verification;

	@JsonProperty("ingestion")
	@Schema(description = "Receipt-chain position and ingestion flags")
	private TraceIngestionResponse ingestion;

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

	public JsonNode getAssertion() {
		return assertion;
	}

	public void setAssertion(JsonNode assertion) {
		this.assertion = assertion;
	}

	public String getAssertionRaw() {
		return assertionRaw;
	}

	public void setAssertionRaw(String assertionRaw) {
		this.assertionRaw = assertionRaw;
	}

	public String getContentDigest() {
		return contentDigest;
	}

	public void setContentDigest(String contentDigest) {
		this.contentDigest = contentDigest;
	}

	public TraceVerificationResponse getVerification() {
		return verification;
	}

	public void setVerification(TraceVerificationResponse verification) {
		this.verification = verification;
	}

	public TraceIngestionResponse getIngestion() {
		return ingestion;
	}

	public void setIngestion(TraceIngestionResponse ingestion) {
		this.ingestion = ingestion;
	}

}
