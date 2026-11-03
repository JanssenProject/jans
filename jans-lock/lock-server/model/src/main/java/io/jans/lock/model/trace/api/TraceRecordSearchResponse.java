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
 * TRACE records matching a policy store release search, ordered by receipt sequence.
 */
@Schema(description = "TRACE records matching a policy store release search, ordered by receipt sequence")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceRecordSearchResponse {

	private static final String ORDERING = "receipt_sequence";

	@JsonProperty("records")
	@Schema(description = "Matching records, ordered by receipt_sequence")
	private List<TraceRecordResponse> records = new ArrayList<>();

	@JsonProperty("ordering")
	@Schema(description = "Fixed: records are ordered by receipt_sequence", example = "receipt_sequence")
	public String getOrdering() {
		return ORDERING;
	}

	public List<TraceRecordResponse> getRecords() {
		return records;
	}

	public void setRecords(List<TraceRecordResponse> records) {
		this.records = records;
	}

}
