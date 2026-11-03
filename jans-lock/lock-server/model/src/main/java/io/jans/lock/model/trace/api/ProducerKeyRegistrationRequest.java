/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Admin request body to register a producer's Ed25519 verification key (design §6).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer key registration request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerKeyRegistrationRequest {

	@JsonProperty("producer_id")
	@Schema(description = "Opaque stable producer id", example = "cedarling-fleet-1")
	private String producerId;

	@JsonProperty("kid")
	@Schema(description = "Key id", example = "2026-01-key-1")
	private String kid;

	@JsonProperty("public_key_jwk")
	@Schema(description = "RFC 8037 OKP Ed25519 JWK", example = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"...\"}")
	private Map<String, String> publicKeyJwk;

	@JsonProperty("authorized_event_kinds")
	@Schema(description = "Event kinds the key may sign; required, non-empty, no duplicates", example = "[\"AUTHORIZATION_DECISION\"]", allowableValues = {
			"AUTHORIZATION_DECISION", "CAPABILITY_INVOKED", "RUNTIME_EFFECT" })
	private List<String> authorizedEventKinds;

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

	public List<String> getAuthorizedEventKinds() {
		return authorizedEventKinds;
	}

	public void setAuthorizedEventKinds(List<String> authorizedEventKinds) {
		this.authorizedEventKinds = authorizedEventKinds;
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
