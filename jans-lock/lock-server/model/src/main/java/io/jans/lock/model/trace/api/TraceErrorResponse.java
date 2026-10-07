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
 * Swagger schema for the structured TRACE error body (design decision D-10). The actual body is
 * produced by {@code ErrorResponseFactory}/{@code DefaultErrorResponse}; this type exists only so
 * the TRACE OpenAPI responses document the {@code error}/{@code error_description}/{@code reason}
 * shape instead of the generic {@code LockApiError} schema.
 *
 * @author Yuriy Movchan
 */
@Schema(description = "Structured TRACE error response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceErrorResponse {

	@JsonProperty("error")
	@Schema(description = "TRACE error id (design decision D-10)", example = "invalid_assertion")
	private String error;

	@JsonProperty("error_description")
	@Schema(description = "Human-readable error description")
	private String errorDescription;

	@JsonProperty("reason")
	@Schema(description = "Machine-readable detail, present only when error reporting is enabled", example = "missing:trace.signed_at")
	private String reason;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
