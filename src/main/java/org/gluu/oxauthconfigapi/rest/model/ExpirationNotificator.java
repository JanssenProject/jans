package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.Positive;

public class ExpirationNotificator implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Boolean expirationNotificatorEnabled;
	
	@Positive
	private int expirationNotificatorMapSizeLimit;
	
	@Positive
	private int expirationNotificatorIntervalInSeconds;
	
	
	public Boolean getExpirationNotificatorEnabled() {
		return expirationNotificatorEnabled;
	}
	
	public void setExpirationNotificatorEnabled(Boolean expirationNotificatorEnabled) {
		this.expirationNotificatorEnabled = expirationNotificatorEnabled;
	}
	
	public int getExpirationNotificatorMapSizeLimit() {
		return expirationNotificatorMapSizeLimit;
	}
	
	public void setExpirationNotificatorMapSizeLimit(int expirationNotificatorMapSizeLimit) {
		this.expirationNotificatorMapSizeLimit = expirationNotificatorMapSizeLimit;
	}
	
	public int getExpirationNotificatorIntervalInSeconds() {
		return expirationNotificatorIntervalInSeconds;
	}
	
	public void setExpirationNotificatorIntervalInSeconds(int expirationNotificatorIntervalInSeconds) {
		this.expirationNotificatorIntervalInSeconds = expirationNotificatorIntervalInSeconds;
	}	

}
