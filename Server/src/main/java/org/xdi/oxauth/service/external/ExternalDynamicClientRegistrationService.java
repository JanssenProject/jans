/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.log.Log;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.client.ClientRegistrationType;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.custom.ExtendedCustomScriptManager;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.util.StringHelper;

/**
 * Provides factory methods needed to create external dynamic client registration extension
 *
 * @author Yuriy Movchan Date: 08.21.2012
 */
@Scope(ScopeType.APPLICATION)
@Name("externalDynamicClientRegistrationService")
@AutoCreate
@Startup
public class ExternalDynamicClientRegistrationService implements Serializable {

	private static final long serialVersionUID = -3070021905197441202L;

	private Map<String, CustomScriptConfiguration> customScriptConfigurations;
	private CustomScriptConfiguration defaultExternalCustomScript;

	@Logger
	private Log log;
	
	@In
	private ExtendedCustomScriptManager extendedCustomScriptManager;


	@Observer(CustomScriptManager.MODIFIED_EVENT_TYPE)
	public void reload() {
		// Get actual list of external client registration configurations
		List<CustomScriptConfiguration> customScriptConfigurations = extendedCustomScriptManager.getCustomScriptConfigurationsByScriptType(CustomScriptType.CLIENT_REGISTRATION);

		// Store updated external client registration configurations
		this.customScriptConfigurations = reloadExternalConfigurations(customScriptConfigurations);

		// Determine default client registration configuration
		this.defaultExternalCustomScript = determineDefaultCustomScriptConfiguration(this.customScriptConfigurations);
	}

	private Map<String, CustomScriptConfiguration> reloadExternalConfigurations(List<CustomScriptConfiguration> customScriptConfigurations) {
		Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());
		
		// Convert CustomScript to old model
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
			reloadedExternalConfigurations.put(StringHelper.toLowerCase(customScriptConfiguration.getName()), customScriptConfiguration);
		}

		return reloadedExternalConfigurations;
	}


	public CustomScriptConfiguration determineDefaultCustomScriptConfiguration(Map<String,  CustomScriptConfiguration> customScriptConfigurations) {
		CustomScriptConfiguration defaultExternalCustomScript = null;
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations.values()) {
			// Determine default authenticator
			if ((defaultExternalCustomScript == null) ||
					(defaultExternalCustomScript.getLevel() >= customScriptConfiguration.getLevel())) {
				defaultExternalCustomScript = customScriptConfiguration;
			}
		}
		
		return defaultExternalCustomScript;
	}

	public boolean executeExternalClientRegistrationUpdateClientMethod(CustomScriptConfiguration customScriptConfiguration, RegisterRequest registerRequest, Client client) {
		try {
			log.debug("Executing python 'updateClient' authenticator method");
			ClientRegistrationType externalClientRegistrationType = (ClientRegistrationType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalClientRegistrationType.updateClient(registerRequest, client, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeDefaultExternalClientRegistrationUpdateClientMethod(RegisterRequest registerRequest, Client client) {
		return executeExternalClientRegistrationUpdateClientMethod(this.defaultExternalCustomScript, registerRequest, client);
	}

	public boolean isEnabled() {
		return this.customScriptConfigurations.size() > 0;
	}

	public CustomScriptConfiguration getCustomScriptConfiguration(String name) {
		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurations.entrySet()) {
			if (StringHelper.equalsIgnoreCase(name, customScriptConfigurationEntry.getKey())) {
				return customScriptConfigurationEntry.getValue();
			}
		}
		
		return null;
	}

	public List<CustomScriptConfiguration> getCustomScriptConfigurations() {
		return new ArrayList<CustomScriptConfiguration>(this.customScriptConfigurations.values());
	}

    public static ExternalDynamicClientRegistrationService instance() {
        return (ExternalDynamicClientRegistrationService) Component.getInstance(ExternalDynamicClientRegistrationService.class);
    }

}
