/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

	@JsonProperty("key_thumbprint")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "RFC 7638 thumbprint of the key the record was verified against, sha256:<hex>; absent on records stored before it was recorded", example = "sha256:90facafea9b1556698540f70c0117a22ea37bd5cf3ed3c47093c1707282b4b89")
	private String keyThumbprint;

	@JsonProperty("key_authorized")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "True when the key existed, was valid and unrevoked at ingestion, was registered for the producer and authorized for the event kind; absent on records stored before it was recorded")
	private Boolean keyAuthorized;

	@JsonProperty("verified_at")
	@Schema(description = "RFC 3339 UTC timestamp verification was performed at", example = "2026-06-11T00:41:02.123Z")
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

	public String getKeyThumbprint() {
		return keyThumbprint;
	}

	public void setKeyThumbprint(String keyThumbprint) {
		this.keyThumbprint = keyThumbprint;
	}

	public Boolean getKeyAuthorized() {
		return keyAuthorized;
	}

	public void setKeyAuthorized(Boolean keyAuthorized) {
		this.keyAuthorized = keyAuthorized;
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
