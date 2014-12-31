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
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.custom.script.CustomScriptType;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.model.custom.script.model.auth.AuthenticationCustomScript;
import org.xdi.model.custom.script.type.auth.CustomAuthenticatorType;
import org.xdi.oxauth.service.custom.ExtendedCustomScriptManager;
import org.xdi.service.custom.script.CustomScriptManager;
import org.xdi.util.StringHelper;

/**
 * Provides factory methods needed to create external authenticator
 *
 * @author Yuriy Movchan Date: 08.21.2012
 */
@Scope(ScopeType.APPLICATION)
@Name("externalAuthenticationService")
@AutoCreate
@Startup
public class ExternalAuthenticationService implements Serializable {

	private static final long serialVersionUID = -1225880597520443390L;

	public final static String ACR_METHOD_PREFIX = "https://schema.gluu.org/openid/acr/method/";

//	private transient CustomScriptConfiguration defaultExternalAuthenticator;

	private Map<String, CustomScriptConfiguration> customScriptConfigurations;
	private Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> customScriptConfigurationsByUsageType;
	private Map<AuthenticationScriptUsageType, CustomScriptConfiguration> defaultExternalAuthenticators;

	@Logger
	private Log log;
	
	@In
	private ExtendedCustomScriptManager extendedCustomScriptManager;

	@Observer(CustomScriptManager.MODIFIED_EVENT_TYPE)
	public void reload() {
		// Get actual list of external authenticator configurations
		List<CustomScriptConfiguration> customScriptConfigurations = extendedCustomScriptManager.getCustomScriptConfigurationsByScriptType(CustomScriptType.CUSTOM_AUTHENTICATION);

		// Store updated external authenticator configurations
		this.customScriptConfigurations = reloadExternalConfigurations(customScriptConfigurations);

		// Group external authenticator configurations by usage type
		this.customScriptConfigurationsByUsageType = groupCustomScriptConfigurationsByUsageType(this.customScriptConfigurations);

		// Determine default authenticator for every usage type
		this.defaultExternalAuthenticators = determineDefaultCustomScriptConfigurations(this.customScriptConfigurations);
	}

	private Map<String, CustomScriptConfiguration> reloadExternalConfigurations(List<CustomScriptConfiguration> customScriptConfigurations) {
		Map<String, CustomScriptConfiguration> reloadedExternalConfigurations = new HashMap<String, CustomScriptConfiguration>(customScriptConfigurations.size());
		
		// Convert CustomScript to old model
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
			reloadedExternalConfigurations.put(StringHelper.toLowerCase(customScriptConfiguration.getName()), customScriptConfiguration);
		}

		return reloadedExternalConfigurations;
	}


	public Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> groupCustomScriptConfigurationsByUsageType(Map<String,  CustomScriptConfiguration> customScriptConfigurations) {
		Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> newCustomScriptConfigurationsByUsageType = new HashMap<AuthenticationScriptUsageType, List<CustomScriptConfiguration>>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			List<CustomScriptConfiguration> currCustomScriptConfigurationsByUsageType = new ArrayList<CustomScriptConfiguration>();

			for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations.values()) {
				if (!isValidateUsageType(usageType, customScriptConfiguration)) {
					continue;
				}
				
				currCustomScriptConfigurationsByUsageType.add(customScriptConfiguration);
			}
			newCustomScriptConfigurationsByUsageType.put(usageType, currCustomScriptConfigurationsByUsageType);
		}
		
		return newCustomScriptConfigurationsByUsageType;
	}

	public Map<AuthenticationScriptUsageType, CustomScriptConfiguration> determineDefaultCustomScriptConfigurations(Map<String,  CustomScriptConfiguration> customScriptConfigurations) {
		Map<AuthenticationScriptUsageType, CustomScriptConfiguration> newDefaultCustomScriptConfigurations = new HashMap<AuthenticationScriptUsageType, CustomScriptConfiguration>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			CustomScriptConfiguration defaultExternalAuthenticator = null;
			for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurationsByUsageType.get(usageType)) {
				// Determine default authenticator
				if ((defaultExternalAuthenticator == null) ||
						(defaultExternalAuthenticator.getLevel() >= customScriptConfiguration.getLevel())) {
					defaultExternalAuthenticator = customScriptConfiguration;
				}
			}
			
			newDefaultCustomScriptConfigurations.put(usageType, defaultExternalAuthenticator);
		}
		
		return newDefaultCustomScriptConfigurations;
	}

	public boolean executeExternalAuthenticatorIsValidAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
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

	public String executeExternalAuthenticatorGetAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
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

	public int executeExternalAuthenticatorGetCountAuthenticationSteps(CustomScriptConfiguration customScriptConfiguration) {
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

	public boolean executeExternalAuthenticatorAuthenticate(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
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

	public boolean executeExternalAuthenticatorLogout(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters) {
    	// Validate API version
        int apiVersion = executeExternalAuthenticatorGetApiVersion(customScriptConfiguration);
        if (apiVersion > 2) {
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

        return true;
	}

	public boolean executeExternalAuthenticatorPrepareForStep(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
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

	public List<String> executeExternalAuthenticatorGetExtraParametersForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
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

	public String executeExternalAuthenticatorGetPageForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
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

	public int executeExternalAuthenticatorGetApiVersion(CustomScriptConfiguration customScriptConfiguration) {
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
		return this.customScriptConfigurationsByUsageType.get(usageType).size() > 0;
	}

	public CustomScriptConfiguration getExternalAuthenticatorByAuthLevel(AuthenticationScriptUsageType usageType, int authLevel) {
		CustomScriptConfiguration resultDefaultExternalAuthenticator = null;
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsByUsageType.get(usageType)) {
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
				for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsByUsageType.get(usageType)) {
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
				if (customScriptConfigurations.containsKey(StringHelper.toLowerCase(authMode))) {
					authModes.add(authMode);
				}
			}
		}
		
		return authModes;
	}

	public CustomScriptConfiguration determineExternalAuthenticatorForWorkflow(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
    	// Validate API version
        int apiVersion = executeExternalAuthenticatorGetApiVersion(customScriptConfiguration);
        if (apiVersion > 2) {
        	String authMode = customScriptConfiguration.getName();
        	log.debug("Validating auth_mode: '{0}'", authMode);

        	boolean isValidAuthenticationMethod = executeExternalAuthenticatorIsValidAuthenticationMethod(usageType, customScriptConfiguration);
            if (!isValidAuthenticationMethod) {
            	log.warn("Current auth_mode: '{0}' isn't valid", authMode);

            	String alternativeAuthenticationMethod = executeExternalAuthenticatorGetAlternativeAuthenticationMethod(usageType, customScriptConfiguration);
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
        }
        
        return customScriptConfiguration;
	}

	public CustomScriptConfiguration getDefaultExternalAuthenticator(AuthenticationScriptUsageType usageType) {
		return this.defaultExternalAuthenticators.get(usageType);
	}

	public CustomScriptConfiguration getCustomScriptConfiguration(AuthenticationScriptUsageType usageType, String name) {
		for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsByUsageType.get(usageType)) {
			if (StringHelper.equalsIgnoreCase(name, customScriptConfiguration.getName())) {
				return customScriptConfiguration;
			}
		}
		
		return null;
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

	public  List<String> getAcrValuesList() {
		List<String> acrValues = new ArrayList<String>();

		for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurations.entrySet()) {
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
