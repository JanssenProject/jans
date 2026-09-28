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
 * Admin request body to register a producer's Ed25519 verification key (design §6, TRACE MVP
 * task 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer key registration request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerKeyRegistrationRequest {

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("kid")
	@Schema(description = "Key id", example = "2026-01-key-1")
	private String kid;

	@JsonProperty("public_key_jwk")
	@Schema(description = "RFC 8037 OKP Ed25519 JWK", example = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"...\"}")
	private Map<String, String> publicKeyJwk;

	@JsonProperty("valid_from")
	@Schema(description = "RFC 3339 timestamp the key becomes valid", example = "2026-01-01T00:00:00Z")
	private String validFrom;

	@JsonProperty("valid_until")
	@Schema(description = "RFC 3339 timestamp the key expires, or null for no expiry", example = "2027-01-01T00:00:00Z")
	private String validUntil;

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

}
