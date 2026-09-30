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
 * Response body of the producer-key listing admin endpoint (task 15).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Producer key list response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerKeyListResponse {

	@JsonProperty("keys")
	@Schema(description = "Registered producer keys in the caller's evidence domain")
	private List<ProducerKeyResponse> keys = new ArrayList<>();

	public List<ProducerKeyResponse> getKeys() {
		return keys;
	}

	public void setKeys(List<ProducerKeyResponse> keys) {
		this.keys = keys;
	}

}
