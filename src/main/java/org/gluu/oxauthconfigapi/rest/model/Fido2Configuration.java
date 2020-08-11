package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

public class Fido2Configuration implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String authenticatorCertsFolder;
	private String mdsAccessToken;
	private String mdsCertsFolder;
	private String mdsTocsFolder;
	private String serverMetadataFolder;
	private Boolean userAutoEnrollment;
	private Integer unfinishedRequestExpiration;
	private Integer authenticationHistoryExpiration;
	private Boolean disableFido2;
	
	public String getAuthenticatorCertsFolder() {
		return authenticatorCertsFolder;
	}
	
	public void setAuthenticatorCertsFolder(String authenticatorCertsFolder) {
		this.authenticatorCertsFolder = authenticatorCertsFolder;
	}
	
	public String getMdsAccessToken() {
		return mdsAccessToken;
	}
	
	public void setMdsAccessToken(String mdsAccessToken) {
		this.mdsAccessToken = mdsAccessToken;
	}
	
	public String getMdsCertsFolder() {
		return mdsCertsFolder;
	}
	
	public void setMdsCertsFolder(String mdsCertsFolder) {
		this.mdsCertsFolder = mdsCertsFolder;
	}
	
	public String getMdsTocsFolder() {
		return mdsTocsFolder;
	}
	
	public void setMdsTocsFolder(String mdsTocsFolder) {
		this.mdsTocsFolder = mdsTocsFolder;
	}
	
	public String getServerMetadataFolder() {
		return serverMetadataFolder;
	}
	
	public void setServerMetadataFolder(String serverMetadataFolder) {
		this.serverMetadataFolder = serverMetadataFolder;
	}
	
	public Boolean getUserAutoEnrollment() {
		return userAutoEnrollment;
	}
	
	public void setUserAutoEnrollment(Boolean userAutoEnrollment) {
		this.userAutoEnrollment = userAutoEnrollment;
	}
	public Integer getUnfinishedRequestExpiration() {
		return unfinishedRequestExpiration;
	}
	
	public void setUnfinishedRequestExpiration(Integer unfinishedRequestExpiration) {
		this.unfinishedRequestExpiration = unfinishedRequestExpiration;
	}
	
	public Integer getAuthenticationHistoryExpiration() {
		return authenticationHistoryExpiration;
	}
	
	public void setAuthenticationHistoryExpiration(Integer authenticationHistoryExpiration) {
		this.authenticationHistoryExpiration = authenticationHistoryExpiration;
	}
	
	public Boolean getDisableFido2() {
		return disableFido2;
	}
	
	public void setDisableFido2(Boolean disableFido2) {
		this.disableFido2 = disableFido2;
	}
}
