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
 * The direct TRACE records of one execution, ordered by receipt sequence (design §10, §11.3,
 * TRACE MVP task 21). Carries no completeness claim beyond the fixed {@code "not_assessed"}
 * value and never includes child executions (design §14).
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Direct TRACE records of one execution, ordered by receipt sequence")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceExecutionResponse {

	@JsonProperty("execution_authority")
	@Schema(description = "The execution's authority", example = "spiffe://example.org/agent/planner")
	private String executionAuthority;

	@JsonProperty("trace_execution_id")
	@Schema(description = "Execution id", example = "exec-01J...")
	private String traceExecutionId;

	@JsonProperty("ordering")
	@Schema(description = "Fixed: records are ordered by receipt_sequence", example = "receipt_sequence")
	private final String ordering = "receipt_sequence";

	@JsonProperty("completeness")
	@Schema(description = "Fixed: Lock never asserts completeness of an execution's records", example = "not_assessed")
	private final String completeness = "not_assessed";

	@JsonProperty("records")
	@Schema(description = "The execution's direct records, ordered by receipt_sequence")
	private List<TraceRecordResponse> records = new ArrayList<>();

	public String getExecutionAuthority() {
		return executionAuthority;
	}

	public void setExecutionAuthority(String executionAuthority) {
		this.executionAuthority = executionAuthority;
	}

	public String getTraceExecutionId() {
		return traceExecutionId;
	}

	public void setTraceExecutionId(String traceExecutionId) {
		this.traceExecutionId = traceExecutionId;
	}

	public String getOrdering() {
		return ordering;
	}

	public String getCompleteness() {
		return completeness;
	}

	public List<TraceRecordResponse> getRecords() {
		return records;
	}

	public void setRecords(List<TraceRecordResponse> records) {
		this.records = records;
	}

}
