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
 * The {@code verification} member of a TRACE record envelope (design §10, TRACE MVP task 21).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Signature verification outcome for a TRACE record")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceVerificationResponse {

	@JsonProperty("signature_valid")
	@Schema(description = "True when the Ed25519 signature verified against the registered key")
	private boolean signatureValid;

	@JsonProperty("key_id")
	@Schema(description = "kid of the key the record was verified against", example = "key-1")
	private String keyId;

	@JsonProperty("verified_at")
	@Schema(description = "RFC 3339 UTC timestamp verification was performed at", example = "2026-06-11T00:41:02Z")
	private String verifiedAt;

	@JsonProperty("algorithm")
	@Schema(description = "Signature algorithm", example = "Ed25519")
	private String algorithm;

	public boolean isSignatureValid() {
		return signatureValid;
	}

	public void setSignatureValid(boolean signatureValid) {
		this.signatureValid = signatureValid;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(String verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

}
