/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.model.conf;

import java.util.List;

import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@Vetoed
public class Configuration {

	private String issuer;
	private String baseEndpoint;
	private List<PlatformConfiguration> platformConfigurations;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getBaseEndpoint() {
		return baseEndpoint;
	}

	public void setBaseEndpoint(String baseEndpoint) {
		this.baseEndpoint = baseEndpoint;
	}

	public List<PlatformConfiguration> getPlatformConfigurations() {
		return platformConfigurations;
	}

	public void setPlatformConfigurations(List<PlatformConfiguration> platformConfigurations) {
		this.platformConfigurations = platformConfigurations;
	}

}
