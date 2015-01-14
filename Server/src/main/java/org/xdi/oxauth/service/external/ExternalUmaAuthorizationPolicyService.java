/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.uma.AuthorizationPolicyType;
import org.xdi.oxauth.service.uma.authorization.AuthorizationContext;
import org.xdi.service.LookupService;
import org.xdi.service.custom.script.ExternalScriptService;
import org.xdi.util.StringHelper;

/**
 * Provides factory methods needed to create external UMA authorization policies extension
 *
 * @author Yuriy Movchan Date: 01/14/2015
 */
@Scope(ScopeType.APPLICATION)
@Name("externalUmaAuthorizationPolicyService")
@AutoCreate
@Startup
public class ExternalUmaAuthorizationPolicyService extends ExternalScriptService {

	private static final long serialVersionUID = -8609727759114795432L;
	
	@In
	private LookupService lookupService;

	protected Map<String, CustomScriptConfiguration> customScriptConfigurationsInumMap;

	public ExternalUmaAuthorizationPolicyService() {
		super(CustomScriptType.UMA_AUTHORIZATION_POLICY);
	}

	@Override
	protected void reloadExternal() {
		this.customScriptConfigurationsInumMap = buildExternalConfigurationsInumMap(this.customScriptConfigurations);
	}

	private Map<String, CustomScriptConfiguration> buildExternalConfigurationsInumMap(List<CustomScriptConfiguration> customScriptConfigurations) {
		Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());
		
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
			reloadedExternalConfigurations.put(customScriptConfiguration.getInum(), customScriptConfiguration);
		}

		return reloadedExternalConfigurations;
	}

	public CustomScriptConfiguration getAuthorizationPolicyByDn(String authorizationPolicyDn) {
		String authorizationPolicyInum = lookupService.getInumFromDn(authorizationPolicyDn);
		
		return getCustomScriptConfigurationByInum(authorizationPolicyInum);
	}

	public CustomScriptConfiguration getCustomScriptConfigurationByInum(String inum) {
		if (StringHelper.isEmpty(inum)) {
			return null;
		}

		return this.customScriptConfigurationsInumMap.get(inum);
	}

	public boolean executeExternalAuthorizeMethod(CustomScriptConfiguration customScriptConfiguration, AuthorizationContext authorizationContext) {
		try {
			log.debug("Executing python 'authorize' method");
			AuthorizationPolicyType externalType = (AuthorizationPolicyType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalType.authorize(authorizationContext, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

    public static ExternalUmaAuthorizationPolicyService instance() {
        return (ExternalUmaAuthorizationPolicyService) Component.getInstance(ExternalUmaAuthorizationPolicyService.class);
    }

}
