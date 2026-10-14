/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body of the producer-chain listing admin endpoint (task 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer chain list response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerChainListResponse {

	@JsonProperty("chains")
	@Schema(description = "Registered producer chains in the caller's evidence domain")
	private List<ProducerChainResponse> chains = new ArrayList<>();

	public List<ProducerChainResponse> getChains() {
		return chains;
	}

	public void setChains(List<ProducerChainResponse> chains) {
		this.chains = chains;
	}

}
