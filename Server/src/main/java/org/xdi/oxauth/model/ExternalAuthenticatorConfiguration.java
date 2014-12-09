/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model;

import java.util.Map;

import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.oxauth.service.custom.conf.CustomScript;
import org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType;
import org.xdi.oxauth.service.custom.model.CustomScriptConfiguration;
import org.xdi.util.StringHelper;

/**
 * External authenticator configuration
 *
 * @author Yuriy Movchan Date: 01/24/2013
 */
public class ExternalAuthenticatorConfiguration {
	private AuthenticationScriptUsageType usageType;
	private CustomScriptConfiguration customScriptConfiguration;

	public ExternalAuthenticatorConfiguration(CustomScriptConfiguration customScriptConfiguration) {
		this.customScriptConfiguration = customScriptConfiguration;
		initUsageType();
	}

	private void initUsageType() {
		AuthenticationScriptUsageType tmpUsageType = null;
		for (SimpleCustomProperty moduleProperty : this.customScriptConfiguration.getCustomScript().getModuleProperties()) {
			if (StringHelper.equalsIgnoreCase(moduleProperty.getValue1(), "usage_type")) {
				tmpUsageType = AuthenticationScriptUsageType.getByValue(moduleProperty.getValue2());
				break;
			}
		}
		this.usageType = tmpUsageType;
	}

	public String getName() {
		return this.customScriptConfiguration.getCustomScript().getName();
	}

	public int getLevel() {
		return this.customScriptConfiguration.getCustomScript().getLevel();
	}

	@Deprecated
	public int getPriority() {
		return 0;
	}

	public AuthenticationScriptUsageType getUsageType() {
		return usageType;
	}

	public CustomScript getCustomScript() {
		return this.customScriptConfiguration.getCustomScript();
	}

	public CustomAuthenticatorType getExternalAuthenticatorType() {
		return (CustomAuthenticatorType) this.customScriptConfiguration.getExternalType();
	}

	public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
		return this.customScriptConfiguration.getConfigurationAttributes();
	}

}