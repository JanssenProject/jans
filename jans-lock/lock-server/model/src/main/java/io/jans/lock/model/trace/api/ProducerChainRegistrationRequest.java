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
 * Admin request body to pre-register a producer chain (design §8, TRACE MVP design decision D-5,
 * task 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer chain registration request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerChainRegistrationRequest {

	@JsonProperty("producer_id")
	@Schema(description = "Producer id, name/semver", example = "cedarling-fleet-1/1.0.0")
	private String producerId;

	@JsonProperty("producer_instance_id")
	@Schema(description = "Producer instance id", example = "instance-1")
	private String producerInstanceId;

	@JsonProperty("producer_chain_id")
	@Schema(description = "Producer chain id", example = "chain-1")
	private String producerChainId;

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

}
