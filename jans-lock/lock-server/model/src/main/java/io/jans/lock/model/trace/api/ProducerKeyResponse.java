/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A registered producer key as returned by the admin API. {@code public_key_jwk} only ever
 * echoes {@code kty}/{@code crv}/{@code x} (task 15: responses never include private material).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Registered producer key")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerKeyResponse {

	@JsonProperty("evidence_domain_id")
	@Schema(description = "Evidence domain the key is registered in", example = "default")
	private String evidenceDomainId;

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("kid")
	@Schema(description = "Key id", example = "2026-01-key-1")
	private String kid;

	@JsonProperty("public_key_jwk")
	@Schema(description = "RFC 8037 OKP Ed25519 JWK (kty/crv/x only)", example = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"...\"}")
	private Map<String, String> publicKeyJwk;

	@JsonProperty("valid_from")
	@Schema(description = "RFC 3339 timestamp the key becomes valid", example = "2026-01-01T00:00:00Z")
	private String validFrom;

	@JsonProperty("valid_until")
	@Schema(description = "RFC 3339 timestamp the key expires, or null for no expiry", example = "2027-01-01T00:00:00Z")
	private String validUntil;

	@JsonProperty("revoked_at")
	@Schema(description = "RFC 3339 timestamp the key was revoked, or null if active", example = "2026-06-01T00:00:00Z")
	private String revokedAt;

	@JsonProperty("registered_by")
	@Schema(description = "Client id that registered the key", example = "2200.abcd")
	private String registeredBy;

	@JsonProperty("created_at")
	@Schema(description = "RFC 3339 timestamp the key was registered", example = "2026-01-01T00:00:00Z")
	private String createdAt;

	public String getEvidenceDomainId() {
		return evidenceDomainId;
	}

	public void setEvidenceDomainId(String evidenceDomainId) {
		this.evidenceDomainId = evidenceDomainId;
	}

	public String getProducerId() {
		return producerId;
	}

	public void setProducerId(String producerId) {
		this.producerId = producerId;
	}

	public String getKid() {
		return kid;
	}

	public void setKid(String kid) {
		this.kid = kid;
	}

	public Map<String, String> getPublicKeyJwk() {
		return publicKeyJwk;
	}

	public void setPublicKeyJwk(Map<String, String> publicKeyJwk) {
		this.publicKeyJwk = publicKeyJwk;
	}

	public String getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(String validFrom) {
		this.validFrom = validFrom;
	}

	public String getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(String validUntil) {
		this.validUntil = validUntil;
	}

	public String getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(String revokedAt) {
		this.revokedAt = revokedAt;
	}

	public String getRegisteredBy() {
		return registeredBy;
	}

	public void setRegisteredBy(String registeredBy) {
		this.registeredBy = registeredBy;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

}
