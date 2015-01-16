/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.model.auth.AuthenticationCustomScript;
import org.xdi.model.custom.script.type.auth.CustomAuthenticatorType;
import org.xdi.service.custom.script.ExternalScriptService;
import org.xdi.util.StringHelper;

/**
 * Provides factory methods needed to create external authenticator
 *
 * @author Yuriy Movchan Date: 21/08/2012
 */
@Scope(ScopeType.APPLICATION)
@Name("externalAuthenticationService")
@AutoCreate
@Startup
public class ExternalAuthenticationService extends ExternalScriptService {

	private static final long serialVersionUID = 7339887464253044927L;

	public final static String ACR_METHOD_PREFIX = "https://schema.gluu.org/openid/acr/method/";

	private Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> customScriptConfigurationsMapByUsageType;
	private Map<AuthenticationScriptUsageType, CustomScriptConfiguration> defaultExternalAuthenticators;

	public ExternalAuthenticationService() {
		super(CustomScriptType.CUSTOM_AUTHENTICATION);
	}

	@Override
	protected void reloadExternal() {
		// Group external authenticator configurations by usage type
		this.customScriptConfigurationsMapByUsageType = groupCustomScriptConfigurationsMapByUsageType(this.customScriptConfigurationsNameMap);

		// Determine default authenticator for every usage type
		this.defaultExternalAuthenticators = determineDefaultCustomScriptConfigurationsMap(this.customScriptConfigurationsNameMap);
	}

	public Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> groupCustomScriptConfigurationsMapByUsageType(Map<String,  CustomScriptConfiguration> customScriptConfigurationsMap) {
		Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> newCustomScriptConfigurationsMapByUsageType = new HashMap<AuthenticationScriptUsageType, List<CustomScriptConfiguration>>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			List<CustomScriptConfiguration> currCustomScriptConfigurationsMapByUsageType = new ArrayList<CustomScriptConfiguration>();

			for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurationsMap.values()) {
				if (!isValidateUsageType(usageType, customScriptConfiguration)) {
					continue;
				}
				
				currCustomScriptConfigurationsMapByUsageType.add(customScriptConfiguration);
			}
			newCustomScriptConfigurationsMapByUsageType.put(usageType, currCustomScriptConfigurationsMapByUsageType);
		}
		
		return newCustomScriptConfigurationsMapByUsageType;
	}

	public Map<AuthenticationScriptUsageType, CustomScriptConfiguration> determineDefaultCustomScriptConfigurationsMap(Map<String,  CustomScriptConfiguration> customScriptConfigurationsMap) {
		Map<AuthenticationScriptUsageType, CustomScriptConfiguration> newDefaultCustomScriptConfigurationsMap = new HashMap<AuthenticationScriptUsageType, CustomScriptConfiguration>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			CustomScriptConfiguration defaultExternalAuthenticator = null;
			for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurationsMapByUsageType.get(usageType)) {
				// Determine default authenticator
				if ((defaultExternalAuthenticator == null) ||
						(defaultExternalAuthenticator.getLevel() >= customScriptConfiguration.getLevel())) {
					defaultExternalAuthenticator = customScriptConfiguration;
				}
			}
			
			newDefaultCustomScriptConfigurationsMap.put(usageType, defaultExternalAuthenticator);
		}
		
		return newDefaultCustomScriptConfigurationsMap;
	}

	public boolean executeExternalIsValidAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'isValidAuthenticationMethod' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public String executeExternalGetAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'getAlternativeAuthenticationMethod' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getAlternativeAuthenticationMethod(usageType, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public int executeExternalGetCountAuthenticationSteps(CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'getCountAuthenticationSteps' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getCountAuthenticationSteps(configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return -1;
	}

	public boolean executeExternalAuthenticate(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
		try {
			log.debug("Executing python 'authenticate' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.authenticate(configurationAttributes, requestParameters, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeExternalLogout(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters) {
		try {
			log.debug("Executing python 'logout' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.logout(configurationAttributes, requestParameters);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return false;
	}

	public boolean executeExternalPrepareForStep(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
		try {
			log.debug("Executing python 'prepareForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public List<String> executeExternalGetExtraParametersForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
		try {
			log.debug("Executing python 'getPageForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getExtraParametersForStep(configurationAttributes, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public String executeExternalGetPageForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
		try {
			log.debug("Executing python 'getPageForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getPageForStep(configurationAttributes, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public int executeExternalGetApiVersion(CustomScriptConfiguration customScriptConfiguration) {
		try {
			log.debug("Executing python 'getApiVersion' authenticator method");
			CustomAuthenticatorType externalAuthenticator = (CustomAuthenticatorType) customScriptConfiguration.getExternalType();
			return externalAuthenticator.getApiVersion();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return -1;
	}

	public boolean isEnabled(AuthenticationScriptUsageType usageType) {
		return this.customScriptConfigurationsMapByUsageType.get(usageType).size() > 0;
	}

	public CustomScriptConfiguration getExternalAuthenticatorByAuthLevel(AuthenticationScriptUsageType usageType, int authLevel) {
		CustomScriptConfiguration resultDefaultExternalAuthenticator = null;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsMapByUsageType.get(usageType)) {
			// Determine authenticator
			if (customScriptConfiguration.getLevel() != authLevel) {
				continue;
			}

			if (resultDefaultExternalAuthenticator == null) {
				resultDefaultExternalAuthenticator = customScriptConfiguration;
			}
		}

		return resultDefaultExternalAuthenticator;
	}

	public CustomScriptConfiguration determineCustomScriptConfiguration(AuthenticationScriptUsageType usageType, int authStep, String authLevel, String authMode) {
        CustomScriptConfiguration customScriptConfiguration = null;
        if (authStep == 1) {
            if (StringHelper.isNotEmpty(authMode)) {
                customScriptConfiguration = getCustomScriptConfiguration(usageType, authMode);
            } else {
            	if (StringHelper.isNotEmpty(authLevel)) {
            		customScriptConfiguration = getExternalAuthenticatorByAuthLevel(usageType, StringHelper.toInteger(authLevel));
            	} else {
            		customScriptConfiguration = getDefaultExternalAuthenticator(usageType);
            	}
            }
        } else {
            customScriptConfiguration = getCustomScriptConfiguration(usageType, authMode);
        }
        
        return customScriptConfiguration;
	}

	public CustomScriptConfiguration determineCustomScriptConfiguration(AuthenticationScriptUsageType usageType, List<String> acrValues) {
		List<String> authModes = getAuthModesByAcrValues(acrValues);
		if (authModes.size() > 0) {
			for (String authMode : authModes) {
				for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsMapByUsageType.get(usageType)) {
					if (StringHelper.equalsIgnoreCase(authMode, customScriptConfiguration.getName())) {
						return customScriptConfiguration;
					}
				}
			}
		}

		return null;
	}

	private List<String> getAuthModesByAcrValues(List<String> acrValues) {
		List<String> authModes = new ArrayList<String>();

		for (String acrValue : acrValues) {
			if (StringHelper.isNotEmpty(acrValue) && StringHelper.toLowerCase(acrValue).startsWith(ACR_METHOD_PREFIX)) {
				String authMode = acrValue.substring(ACR_METHOD_PREFIX.length());
				if (customScriptConfigurationsNameMap.containsKey(StringHelper.toLowerCase(authMode))) {
					authModes.add(authMode);
				}
			}
		}
		
		return authModes;
	}

	public CustomScriptConfiguration determineExternalAuthenticatorForWorkflow(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
    	String authMode = customScriptConfiguration.getName();
    	log.debug("Validating auth_mode: '{0}'", authMode);

    	boolean isValidAuthenticationMethod = executeExternalIsValidAuthenticationMethod(usageType, customScriptConfiguration);
        if (!isValidAuthenticationMethod) {
        	log.warn("Current auth_mode: '{0}' isn't valid", authMode);

        	String alternativeAuthenticationMethod = executeExternalGetAlternativeAuthenticationMethod(usageType, customScriptConfiguration);
            if (StringHelper.isEmpty(alternativeAuthenticationMethod)) {
            	log.error("Failed to determine alternative authentication mode for auth_mode: '{0}'", authMode);
                return null;
            } else {
            	CustomScriptConfiguration alternativeCustomScriptConfiguration = getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, alternativeAuthenticationMethod);
                if (alternativeCustomScriptConfiguration == null) {
                    log.error("Failed to get alternative CustomScriptConfiguration '{0}' for auth_mode: '{1}'", alternativeAuthenticationMethod, authMode);
                    return null;
                } else {
                    return alternativeCustomScriptConfiguration;
                }
            }
        }
        
        return customScriptConfiguration;
    }

	public CustomScriptConfiguration getDefaultExternalAuthenticator(AuthenticationScriptUsageType usageType) {
		return this.defaultExternalAuthenticators.get(usageType);
	}

	public CustomScriptConfiguration getCustomScriptConfiguration(AuthenticationScriptUsageType usageType, String name) {
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsMapByUsageType.get(usageType)) {
			if (StringHelper.equalsIgnoreCase(name, customScriptConfiguration.getName())) {
				return customScriptConfiguration;
			}
		}
		
		return null;
	}

	public CustomScriptConfiguration getCustomScriptConfigurationByName(String name) {
		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurationsNameMap.entrySet()) {
			if (StringHelper.equalsIgnoreCase(name, customScriptConfigurationEntry.getKey())) {
				return customScriptConfigurationEntry.getValue();
			}
		}
		
		return null;
	}

	public List<CustomScriptConfiguration> getcustomScriptConfigurationsMap() {
		return new ArrayList<CustomScriptConfiguration>(this.customScriptConfigurationsNameMap.values());
	}

	public  List<String> getAcrValuesList() {
		List<String> acrValues = new ArrayList<String>();

		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurationsNameMap.entrySet()) {
			String acrValue = ACR_METHOD_PREFIX + customScriptConfigurationEntry.getKey();
			acrValues.add(acrValue);
		}
		
		return acrValues;
	}

	private boolean isValidateUsageType(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
		if (customScriptConfiguration == null) {
			return false;
		}
		
		AuthenticationScriptUsageType externalAuthenticatorUsageType = ((AuthenticationCustomScript) customScriptConfiguration.getCustomScript()).getUsageType();
		
		// Set default usage type
		if (externalAuthenticatorUsageType == null) {
			externalAuthenticatorUsageType = AuthenticationScriptUsageType.INTERACTIVE;
		}
		
		if (AuthenticationScriptUsageType.BOTH.equals(externalAuthenticatorUsageType)) {
			return true;
		}

		if (AuthenticationScriptUsageType.INTERACTIVE.equals(usageType) && AuthenticationScriptUsageType.INTERACTIVE.equals(externalAuthenticatorUsageType)) {
			return true;
		}

		if (AuthenticationScriptUsageType.SERVICE.equals(usageType) && AuthenticationScriptUsageType.SERVICE.equals(externalAuthenticatorUsageType)) {
			return true;
		}

		if (AuthenticationScriptUsageType.LOGOUT.equals(usageType) && AuthenticationScriptUsageType.LOGOUT.equals(externalAuthenticatorUsageType)) {
			return true;
		}
		
		return false;
	}

    public static ExternalAuthenticationService instance() {
        return (ExternalAuthenticationService) Component.getInstance(ExternalAuthenticationService.class);
    }

}
