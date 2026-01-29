/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config.cedarling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlAccessorType(XmlAccessType.FIELD)
public class PolicySource {

	@DocProperty(description = "Specify if policy source is enabled", defaultValue = "true")
	@Schema(description = "Specify if policy source is enabled")
	private boolean enabled = true;

	@DocProperty(description = "Authorization token to access URI")
    @Schema(description = "Authorization token to access URI")
    private String authorizationToken;

	@DocProperty(description = "URI to policy store. Policy store can be either json/zip")
	@Schema(description = "URI to policy store. Policy store can be either json/zip")
	private String policyStoreUri;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAuthorizationToken() {
		return authorizationToken;
	}

	public void setAuthorizationToken(String authorizationToken) {
		this.authorizationToken = authorizationToken;
	}

	public String getPolicyStoreUri() {
		return policyStoreUri;
	}

	public void setPolicyStoreUri(String policyStoreUri) {
		this.policyStoreUri = policyStoreUri;
	}

	@Override
	public String toString() {
		return "PolicySource [enabled=" + enabled + ", authorizationToken=" + "<REDACTED>" + ", policyStoreUri="
				+ policyStoreUri + "]";
	}

}
