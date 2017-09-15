/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.model.conf;

import java.util.List;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class Configuration {

	private String issuer;
	private String baseEndpoint;
	private List<PlatformConfiguration> platform;

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

	public List<PlatformConfiguration> getPlatform() {
		return platform;
	}

	public void setPlatform(List<PlatformConfiguration> platform) {
		this.platform = platform;
	}

}
