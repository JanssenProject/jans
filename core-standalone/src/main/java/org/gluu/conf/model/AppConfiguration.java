/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.conf.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Base application configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 11/02/2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Serializable {

	private static final long serialVersionUID = -587414854758989561L;

	private String applicationName;
	private String openIdProviderUrl;
	private String openIdClientId;
	private String openIdClientPassword;
	private List<String> openIdScopes;
	private String openIdRedirectUrl;
	
	private String openIdPostLogoutRedirectUri;

	private List<ClaimToAttributeMapping> openIdClaimMapping;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getOpenIdProviderUrl() {
		return openIdProviderUrl;
	}

	public void setOpenIdProviderUrl(String openIdProviderUrl) {
		this.openIdProviderUrl = openIdProviderUrl;
	}

	public String getOpenIdClientId() {
		return openIdClientId;
	}

	public void setOpenIdClientId(String openIdClientId) {
		this.openIdClientId = openIdClientId;
	}

	public String getOpenIdClientPassword() {
		return openIdClientPassword;
	}

	public void setOpenIdClientPassword(String openIdClientPassword) {
		this.openIdClientPassword = openIdClientPassword;
	}

	public List<String> getOpenIdScopes() {
		return openIdScopes;
	}

	public void setOpenIdScopes(List<String> openIdScopes) {
		this.openIdScopes = openIdScopes;
	}

	public String getOpenIdRedirectUrl() {
		return openIdRedirectUrl;
	}

	public void setOpenIdRedirectUrl(String openIdRedirectUrl) {
		this.openIdRedirectUrl = openIdRedirectUrl;
	}

	public List<ClaimToAttributeMapping> getOpenIdClaimMapping() {
		return openIdClaimMapping;
	}

	public void setOpenIdClaimMapping(List<ClaimToAttributeMapping> openIdClaimMapping) {
		this.openIdClaimMapping = openIdClaimMapping;
	}

	public String getOpenIdPostLogoutRedirectUri() {
		return openIdPostLogoutRedirectUri;
	}

	public void setOpenIdPostLogoutRedirectUri(String openIdPostLogoutRedirectUri) {
		this.openIdPostLogoutRedirectUri = openIdPostLogoutRedirectUri;
	}

}
