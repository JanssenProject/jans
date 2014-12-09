/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model;

import java.util.Map;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType;

/**
 * External authenticator configuration
 *
 * @author Yuriy Movchan Date: 01.24.2013
 */
@Deprecated
public class ExternalAuthenticatorConfiguration {
	private CustomAuthenticationConfiguration customAuthenticationConfiguration;
	private CustomAuthenticatorType externalAuthenticatorType;
	private Map<String, SimpleCustomProperty> configurationAttributes;

	public ExternalAuthenticatorConfiguration(
			CustomAuthenticationConfiguration customAuthenticationConfiguration,
			CustomAuthenticatorType externalAuthenticatorType, Map<String, SimpleCustomProperty> configurationAttributes) {
		this.customAuthenticationConfiguration = customAuthenticationConfiguration;
		this.externalAuthenticatorType = externalAuthenticatorType;
		this.configurationAttributes = configurationAttributes;
	}

	public String getName() {
		return customAuthenticationConfiguration.getName();
	}

	public int getLevel() {
		return customAuthenticationConfiguration.getLevel();
	}

	@Deprecated
	public int getPriority() {
		return customAuthenticationConfiguration.getPriority();
	}

	public CustomAuthenticationConfiguration getCustomAuthenticationConfiguration() {
		return customAuthenticationConfiguration;
	}

	public CustomAuthenticatorType getExternalAuthenticatorType() {
		return externalAuthenticatorType;
	}

	public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
		return configurationAttributes;
	}

}