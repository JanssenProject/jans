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
 * Admin request body to revoke a producer's Ed25519 verification key (design decision D-4, task
 * 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer key revocation request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerKeyRevokeRequest {

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("kid")
	@Schema(description = "Key id", example = "2026-01-key-1")
	private String kid;

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

}
