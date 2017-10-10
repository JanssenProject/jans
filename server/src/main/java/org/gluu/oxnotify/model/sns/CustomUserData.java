/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.model.sns;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Yuriy Movchan
 * @version October 09, 2017
 */
@JsonPropertyOrder({ "client_id", "client_ip", "creation_date", "app_user_data" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUserData {

	@JsonProperty(value = "client_id")
	private String clientId;

	@JsonProperty(value = "client_ip")
	private String clientIp;

	@JsonProperty(value = "creation_date")
	private Date creationDate;

	@JsonProperty(value = "app_user_data")
	private String appUserData;

	public CustomUserData(String clientId, String clientIp, Date creationDate, String appUserData) {
		this.clientId = clientId;
		this.clientIp = clientIp;
		this.creationDate = creationDate;
		this.appUserData = appUserData;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getAppUserData() {
		return appUserData;
	}

	public void setAppUserData(String appUserData) {
		this.appUserData = appUserData;
	}

	@Override
	public String toString() {
		return "CustomUserData [clientId=" + clientId + ", clientIp=" + clientIp + ", creationDate=" + creationDate + ", appUserData="
				+ appUserData + "]";
	}

}
