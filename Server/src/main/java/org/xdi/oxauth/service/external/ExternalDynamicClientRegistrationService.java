/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.Map;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.client.ClientRegistrationType;
import org.xdi.oxauth.client.RegisterRequest;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.service.custom.script.ExternalScriptService;

/**
 * Provides factory methods needed to create external dynamic client registration extension
 *
 * @author Yuriy Movchan Date: 01/08/2015
 */
@Scope(ScopeType.APPLICATION)
@Name("externalDynamicClientRegistrationService")
@AutoCreate
@Startup
public class ExternalDynamicClientRegistrationService extends ExternalScriptService {

	private static final long serialVersionUID = 1416361273036208685L;

	public ExternalDynamicClientRegistrationService() {
		super(CustomScriptType.CLIENT_REGISTRATION);
	}

	public boolean executeExternalUpdateClientMethod(CustomScriptConfiguration customScriptConfiguration, RegisterRequest registerRequest, Client client) {
		try {
			log.debug("Executing python 'updateClient' method");
			ClientRegistrationType externalClientRegistrationType = (ClientRegistrationType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalClientRegistrationType.updateClient(registerRequest, client, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeExternalUpdateClientMethods(RegisterRequest registerRequest, Client client) {
		boolean result = true;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
			result &= executeExternalUpdateClientMethod(customScriptConfiguration, registerRequest, client);
			if (!result) {
				return result;
			}
		}

		return result;
	}

    public static ExternalDynamicClientRegistrationService instance() {
        return (ExternalDynamicClientRegistrationService) Component.getInstance(ExternalDynamicClientRegistrationService.class);
    }

}
