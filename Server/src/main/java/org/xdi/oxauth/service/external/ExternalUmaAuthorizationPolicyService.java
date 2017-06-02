/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.uma.UmaAuthorizationPolicyType;
import org.xdi.model.uma.ClaimDefinition;
import org.xdi.oxauth.uma.authorization.UmaAuthorizationContext;
import org.xdi.service.LookupService;
import org.xdi.service.custom.script.ExternalScriptService;
import org.xdi.util.StringHelper;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides factory methods needed to create external UMA authorization policies extension
 *
 * @author Yuriy Movchan Date: 01/14/2015
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalUmaAuthorizationPolicyService extends ExternalScriptService {

	private static final long serialVersionUID = -8609727759114795432L;
	
	@Inject
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

	public boolean authorize(CustomScriptConfiguration script, UmaAuthorizationContext authorizationContext) {
		try {
			log.debug("Executing python 'authorize' method");
			UmaAuthorizationPolicyType externalType = (UmaAuthorizationPolicyType) script.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = script.getConfigurationAttributes();
			return externalType.authorize(authorizationContext, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return false;
	}

	public List<ClaimDefinition> getRequiredClaims(CustomScriptConfiguration script) {
		try {
			log.debug("Executing python 'getRequiredClaims' method");
			UmaAuthorizationPolicyType externalType = (UmaAuthorizationPolicyType) script.getExternalType();
			return externalType.getRequiredClaims();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return new ArrayList<ClaimDefinition>();
	}


}
