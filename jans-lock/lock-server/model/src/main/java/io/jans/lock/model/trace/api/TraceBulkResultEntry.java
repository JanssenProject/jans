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
 * One element of {@code POST /audit/trace/bulk}'s {@code results} array (design decision D-16):
 * the submitted array's position, whether that item was accepted, and either its acceptance body
 * ({@link #getRecord()}) or its structured error ({@link #getError()}) &mdash; exactly one of the
 * two is non-null.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "One item's outcome within a TRACE bulk ingestion response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceBulkResultEntry {

	@JsonProperty("index")
	@Schema(description = "Position of this assertion in the submitted array", example = "0")
	private int index;

	@JsonProperty("accepted")
	@Schema(description = "True when this item was verified and ingested")
	private boolean accepted;

	@JsonProperty("record")
	@Schema(description = "Present when accepted=true: the same body a standalone POST /audit/trace would return")
	private TraceAcceptanceResponse record;

	@JsonProperty("error")
	@Schema(description = "Present when accepted=false: the structured error for this item only")
	private TraceErrorResponse error;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public boolean isAccepted() {
		return accepted;
	}

	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	public TraceAcceptanceResponse getRecord() {
		return record;
	}

	public void setRecord(TraceAcceptanceResponse record) {
		this.record = record;
	}

	public TraceErrorResponse getError() {
		return error;
	}

	public void setError(TraceErrorResponse error) {
		this.error = error;
	}

}
