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
 * A registered producer chain as returned by the admin API (design §8, T-3, task 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Registered producer chain")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerChainResponse {

	@JsonProperty("evidence_domain_id")
	@Schema(description = "Evidence domain the chain is registered in", example = "default")
	private String evidenceDomainId;

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("producer_instance_id")
	@Schema(description = "Producer instance id", example = "instance-1")
	private String producerInstanceId;

	@JsonProperty("producer_chain_id")
	@Schema(description = "Producer chain id", example = "chain-1")
	private String producerChainId;

	@JsonProperty("registered_by")
	@Schema(description = "Client id that registered the chain", example = "2200.abcd")
	private String registeredBy;

	@JsonProperty("created_at")
	@Schema(description = "RFC 3339 timestamp the chain was registered", example = "2026-01-01T00:00:00Z")
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

	public String getProducerInstanceId() {
		return producerInstanceId;
	}

	public void setProducerInstanceId(String producerInstanceId) {
		this.producerInstanceId = producerInstanceId;
	}

	public String getProducerChainId() {
		return producerChainId;
	}

	public void setProducerChainId(String producerChainId) {
		this.producerChainId = producerChainId;
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
