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
 * Send message response
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResponse implements Serializable {

	private static final long serialVersionUID = -1113719742487477953L;

	@JsonProperty
	private String messageId;

	@JsonProperty
	private String requestId;

	@JsonProperty
	private int statusCode;

	public NotificationResponse() {
	}

	public NotificationResponse(@JsonProperty String requestId, @JsonProperty int statusCode,
			@JsonProperty String messageId) {
		this.requestId = requestId;
		this.statusCode = statusCode;
		this.messageId = messageId;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
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
		return "RegisterDeviceResponse [messageId=" + messageId + ", requestId=" + requestId + ", statusCode="
				+ statusCode + "]";
	}

}
