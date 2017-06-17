/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.service;

import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.type.uma.UmaRptPolicyType;
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
public class ExternalUmaRptPolicyService extends ExternalScriptService {

	private static final long serialVersionUID = -8609727759114795433L;
	
	@Inject
	private LookupService lookupService;

	protected Map<String, CustomScriptConfiguration> scriptInumMap;

	public ExternalUmaRptPolicyService() {
		super(CustomScriptType.UMA_RPT_POLICY);
	}

	@Override
	protected void reloadExternal() {
		this.scriptInumMap = buildExternalConfigurationsInumMap(this.customScriptConfigurations);
	}

	private Map<String, CustomScriptConfiguration> buildExternalConfigurationsInumMap(List<CustomScriptConfiguration> customScriptConfigurations) {
		Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());
		
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
			reloadedExternalConfigurations.put(customScriptConfiguration.getInum(), customScriptConfiguration);
		}

		return reloadedExternalConfigurations;
	}

	public CustomScriptConfiguration getScriptByDn(String scriptDn) {
		String authorizationPolicyInum = lookupService.getInumFromDn(scriptDn);
		
		return getScriptByInum(authorizationPolicyInum);
	}

	public CustomScriptConfiguration getScriptByInum(String inum) {
		if (StringHelper.isEmpty(inum)) {
			return null;
		}

		return this.scriptInumMap.get(inum);
	}

	public boolean authorize(CustomScriptConfiguration script, UmaAuthorizationContext context) {
		try {
			log.debug("Executing python 'authorize' method, script: " + script.getName());
			UmaRptPolicyType externalType = (UmaRptPolicyType) script.getExternalType();
			return externalType.authorize(context);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return false;
	}

	public List<ClaimDefinition> getRequiredClaims(CustomScriptConfiguration script, UmaAuthorizationContext context) {
		try {
			log.debug("Executing python 'getRequiredClaims' method, script: " + script.getName());
			UmaRptPolicyType externalType = (UmaRptPolicyType) script.getExternalType();
			return externalType.getRequiredClaims(context);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return new ArrayList<ClaimDefinition>();
	}

	public String getClaimsGatheringScriptName(CustomScriptConfiguration script, UmaAuthorizationContext context) {
		try {
			log.debug("Executing python 'getClaimsGatheringScriptName' method, script: " + script.getName());
			UmaRptPolicyType externalType = (UmaRptPolicyType) script.getExternalType();
			return externalType.getClaimsGatheringScriptName(context);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return "";
	}


}
