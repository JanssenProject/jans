/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Device registration response
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterDeviceResponse implements Serializable {

	private static final long serialVersionUID = -1113719742487477953L;

	@JsonProperty
	private String endpointArn;

	@JsonProperty
	private String requestId;

	@JsonProperty
	private int statusCode;

	public RegisterDeviceResponse() {
	}

	public RegisterDeviceResponse(@JsonProperty String requestId, @JsonProperty int statusCode,
			@JsonProperty String endpointArn) {
		this.requestId = requestId;
		this.statusCode = statusCode;
		this.endpointArn = endpointArn;
	}

	public String getEndpointArn() {
		return endpointArn;
	}

	public void setEndpointArn(String endpointArn) {
		this.endpointArn = endpointArn;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public String toString() {
		return "RegisterDeviceResponse [endpointArn=" + endpointArn + ", requestId=" + requestId + ", statusCode="
				+ statusCode + "]";
	}

}
