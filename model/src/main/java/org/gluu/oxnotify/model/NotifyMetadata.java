/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Notify metadata configuration
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@JsonPropertyOrder({ "version", "issuer", "notifyEndpoint" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotifyMetadata {

	@JsonProperty(value = "version")
	private String version;

	@JsonProperty(value = "issuer")
	private String issuer;

	@JsonProperty(value = "notifyEndpoint")
	private String notifyEndpoint;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getNotifyEndpoint() {
		return notifyEndpoint;
	}

	public void setNotifyEndpoint(String notifyEndpoint) {
		this.notifyEndpoint = notifyEndpoint;
	}

	@Override
	public String toString() {
		return "NotifyMetadata [version=" + version + ", issuer=" + issuer + ", notifyEndpoint=" + notifyEndpoint + "]";
	}

}
