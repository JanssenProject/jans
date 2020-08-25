package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

public class KeyRegenerationConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	private Boolean keyRegenerationEnabled;

	@Min(value = 1)
	@Max(value = 2147483647)
	private int keyRegenerationInterval;
	private String defaultSignatureAlgorithm;
	
	public Boolean getKeyRegenerationEnabled() {
		return keyRegenerationEnabled;
	}
	
	public void setKeyRegenerationEnabled(Boolean keyRegenerationEnabled) {
		this.keyRegenerationEnabled = keyRegenerationEnabled;
	}
	
	public int getKeyRegenerationInterval() {
		return keyRegenerationInterval;
	}
	
	public void setKeyRegenerationInterval(int keyRegenerationInterval) {
		this.keyRegenerationInterval = keyRegenerationInterval;
	}
	
	public String getDefaultSignatureAlgorithm() {
		return defaultSignatureAlgorithm;
	}
	
	public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
		this.defaultSignatureAlgorithm = defaultSignatureAlgorithm;
	}
		
}
