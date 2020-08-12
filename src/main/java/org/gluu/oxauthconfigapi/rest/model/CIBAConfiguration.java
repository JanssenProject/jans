package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotBlank;

public class CIBAConfiguration implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min=1)
	private String apiKey;
	
	@NotBlank
	@Size(min=1)
    private String authDomain;
	
	@NotBlank
	@Size(min=1)
    private String databaseURL;
	
	@NotBlank
	@Size(min=1)
    private String projectId;
	
	@NotBlank
	@Size(min=1)
    private String storageBucket;
	
	@NotBlank
	@Size(min=1)
    private String messagingSenderId;
	
	@NotBlank
	@Size(min=1)
    private String appId;
	
	@NotBlank
	@Size(min=1)
    private String notificationUrl;
	
	@NotBlank
	@Size(min=1)
    private String notificationKey;
	
	@NotBlank
	@Size(min=1)
    private String publicVapidKey;
	
	private int cibaGrantLifeExtraTimeSec;
	
	private int cibaMaxExpirationTimeAllowedSec;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getAuthDomain() {
		return authDomain;
	}

	public void setAuthDomain(String authDomain) {
		this.authDomain = authDomain;
	}

	public String getDatabaseURL() {
		return databaseURL;
	}

	public void setDatabaseURL(String databaseURL) {
		this.databaseURL = databaseURL;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getStorageBucket() {
		return storageBucket;
	}

	public void setStorageBucket(String storageBucket) {
		this.storageBucket = storageBucket;
	}

	public String getMessagingSenderId() {
		return messagingSenderId;
	}

	public void setMessagingSenderId(String messagingSenderId) {
		this.messagingSenderId = messagingSenderId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getNotificationUrl() {
		return notificationUrl;
	}

	public void setNotificationUrl(String notificationUrl) {
		this.notificationUrl = notificationUrl;
	}

	public String getNotificationKey() {
		return notificationKey;
	}

	public void setNotificationKey(String notificationKey) {
		this.notificationKey = notificationKey;
	}

	public String getPublicVapidKey() {
		return publicVapidKey;
	}

	public void setPublicVapidKey(String publicVapidKey) {
		this.publicVapidKey = publicVapidKey;
	}

	public int getCibaGrantLifeExtraTimeSec() {
		return cibaGrantLifeExtraTimeSec;
	}

	public void setCibaGrantLifeExtraTimeSec(int cibaGrantLifeExtraTimeSec) {
		this.cibaGrantLifeExtraTimeSec = cibaGrantLifeExtraTimeSec;
	}

	public int getCibaMaxExpirationTimeAllowedSec() {
		return cibaMaxExpirationTimeAllowedSec;
	}

	public void setCibaMaxExpirationTimeAllowedSec(int cibaMaxExpirationTimeAllowedSec) {
		this.cibaMaxExpirationTimeAllowedSec = cibaMaxExpirationTimeAllowedSec;
	}
	
}
