/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.jans.as.model.config.Constants;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * JSON error response
 * 
 * @author Yuriy Movchan Date: 05/20/2015
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "status", "error" })
public class JsonErrorResponse {

    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = Constants.ERROR)
    private String error;

    @JsonProperty(value = Constants.ERROR_DESCRIPTION)
	private String errorDescription;

    @JsonProperty(value = Constants.ERROR_URI)
	private String errorUri;

	public JsonErrorResponse() {
    }

	public JsonErrorResponse(DefaultErrorResponse response) {
		this.error = response.getType().getParameter();
		this.errorDescription = response.getErrorDescription();
		this.errorUri = response.getErrorUri();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

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

	public String getErrorUri() {
		return errorUri;
	}

	public void setErrorUri(String errorUri) {
		this.errorUri = errorUri;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonErrorResponse [status=").append(status).append(", error=").append(error).append(", errorDescription=")
				.append(errorDescription).append(", errorUri=").append(errorUri).append("]");
		return builder.toString();
	}

}
