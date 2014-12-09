/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
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
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.xdi.exception.PythonException;
import org.xdi.model.AuthenticationScriptUsageType;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.model.config.CustomAuthenticationConfiguration;
import org.xdi.oxauth.model.ExternalAuthenticatorConfiguration;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.custom.CustomScriptManager;
import org.xdi.oxauth.service.custom.conf.CustomScript;
import org.xdi.oxauth.service.custom.conf.CustomScriptType;
import org.xdi.oxauth.service.custom.interfaces.auth.CustomAuthenticatorType;
import org.xdi.oxauth.service.custom.interfaces.auth.DummyCustomAuthenticatorType;
import org.xdi.oxauth.service.custom.model.CustomScriptConfiguration;
import org.xdi.service.PythonService;
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

	private static final CustomAuthenticatorType DUMMY_AUTHENTICATOR_TYPE = new DummyCustomAuthenticatorType();

	private static final String PYTHON_ENTRY_INTERCEPTOR_TYPE = "ExternalAuthenticator";

//	private transient ExternalAuthenticatorConfiguration defaultExternalAuthenticator;

	private Map<String, ExternalAuthenticatorConfiguration> externalAuthenticatorConfigurations;
	private Map<AuthenticationScriptUsageType, List<ExternalAuthenticatorConfiguration>> externalAuthenticatorConfigurationsByUsageType;
	private Map<AuthenticationScriptUsageType, ExternalAuthenticatorConfiguration> defaultExternalAuthenticators;

	@Logger
	private Log log;

	@In
	private PythonService pythonService;
	
	@In
	private CustomScriptManager customScriptManager;

	@Observer(CustomScriptManager.MODIFIED_EVENT_TYPE)
	public void reload() {
		// Get actual list of external authenticator configurations
		List<CustomScriptConfiguration> customScriptConfigurations = customScriptManager.getCustomScriptConfigurationsByScriptType(CustomScriptType.CUSTOM_AUTHENTICATION);

		// Store updated external authenticator configurations
		this.externalAuthenticatorConfigurations = reloadExternalConfigurations(customScriptConfigurations);

		// Group external authenticator configurations by usage type
		this.externalAuthenticatorConfigurationsByUsageType = groupExternalAuthenticatorConfigurationsByUsageType(this.externalAuthenticatorConfigurations);

		// Determine default authenticator for every usage type
		this.defaultExternalAuthenticators = determineDefaultExternalAuthenticatorConfigurations(this.externalAuthenticatorConfigurations);
	}

	private Map<String, ExternalAuthenticatorConfiguration> reloadExternalConfigurations(List<CustomScriptConfiguration> customScriptConfigurations) {
		Map<String, ExternalAuthenticatorConfiguration> reloadedExternalConfigurations = new HashMap<String, ExternalAuthenticatorConfiguration>(customScriptConfigurations.size());
		
		// Convert CustomScript to old model
		for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurations) {
			ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration = new ExternalAuthenticatorConfiguration(customScriptConfiguration);

			reloadedExternalConfigurations.put(StringHelper.toLowerCase(customScriptConfiguration.getCustomScript().getName()), externalAuthenticatorConfiguration);
		}

		return reloadedExternalConfigurations;
	}


	public Map<AuthenticationScriptUsageType, List<ExternalAuthenticatorConfiguration>> groupExternalAuthenticatorConfigurationsByUsageType(Map<String,  ExternalAuthenticatorConfiguration> externalAuthenticatorConfigurations) {
		Map<AuthenticationScriptUsageType, List<ExternalAuthenticatorConfiguration>> newExternalAuthenticatorConfigurationsByUsageType = new HashMap<AuthenticationScriptUsageType, List<ExternalAuthenticatorConfiguration>>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			List<ExternalAuthenticatorConfiguration> currExternalAuthenticatorConfigurationsByUsageType = new ArrayList<ExternalAuthenticatorConfiguration>();

			for (ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration : externalAuthenticatorConfigurations.values()) {
				if (!isValidateUsageType(usageType, externalAuthenticatorConfiguration)) {
					continue;
				}
				
				currExternalAuthenticatorConfigurationsByUsageType.add(externalAuthenticatorConfiguration);
			}
			newExternalAuthenticatorConfigurationsByUsageType.put(usageType, currExternalAuthenticatorConfigurationsByUsageType);
		}
		
		return newExternalAuthenticatorConfigurationsByUsageType;
	}

	public Map<AuthenticationScriptUsageType, ExternalAuthenticatorConfiguration> determineDefaultExternalAuthenticatorConfigurations(Map<String,  ExternalAuthenticatorConfiguration> externalAuthenticatorConfigurations) {
		Map<AuthenticationScriptUsageType, ExternalAuthenticatorConfiguration> newDefaultExternalAuthenticatorConfigurations = new HashMap<AuthenticationScriptUsageType, ExternalAuthenticatorConfiguration>();
		
		for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
			ExternalAuthenticatorConfiguration defaultExternalAuthenticator = null;
			for (ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration : externalAuthenticatorConfigurationsByUsageType.get(usageType)) {
				// Determine default authenticator
				if ((defaultExternalAuthenticator == null) ||
						(defaultExternalAuthenticator.getLevel() >= externalAuthenticatorConfiguration.getLevel()) ||
						((defaultExternalAuthenticator.getLevel() == externalAuthenticatorConfiguration.getLevel()) &&
								(defaultExternalAuthenticator.getPriority() > externalAuthenticatorConfiguration.getPriority()))) {
					defaultExternalAuthenticator = externalAuthenticatorConfiguration;
				}
			}
			
			newDefaultExternalAuthenticatorConfigurations.put(usageType, defaultExternalAuthenticator);
		}
		
		return newDefaultExternalAuthenticatorConfigurations;
	}

	public CustomAuthenticatorType createExternalAuthenticator(CustomAuthenticationConfiguration customAuthenticationConfiguration, Map<String, SimpleCustomProperty> configurationAttributes) {
		CustomAuthenticatorType externalAuthenticator;
		try {
			externalAuthenticator = createExternalAuthenticatorFromStringWithPythonException(customAuthenticationConfiguration, configurationAttributes);
		} catch (PythonException ex) {
			log.error("Failed to prepare external authenticator", ex);
			return null;
		}

		if (externalAuthenticator == null) {
			log.debug("Using default external authenticator class");
			externalAuthenticator = DUMMY_AUTHENTICATOR_TYPE;
		}

		return externalAuthenticator;
	}

	public boolean executeExternalAuthenticatorIsValidAuthenticationMethod(AuthenticationScriptUsageType usageType, ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
		try {
			log.debug("Executing python 'isValidAuthenticationMethod' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public String executeExternalAuthenticatorGetAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
		try {
			log.debug("Executing python 'getAlternativeAuthenticationMethod' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getAlternativeAuthenticationMethod(usageType, configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public int executeExternalAuthenticatorGetCountAuthenticationSteps(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
		try {
			log.debug("Executing python 'getCountAuthenticationSteps' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getCountAuthenticationSteps(configurationAttributes);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return -1;
	}

	public boolean executeExternalAuthenticatorAuthenticate(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration, Map<String, String[]> requestParameters, int step) {
		try {
			log.debug("Executing python 'authenticate' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.authenticate(configurationAttributes, requestParameters, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public boolean executeExternalAuthenticatorLogout(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration, Map<String, String[]> requestParameters) {
    	// Validate API version
        int apiVersion = executeExternalAuthenticatorGetApiVersion(externalAuthenticatorConfiguration);
        if (apiVersion > 2) {
			try {
				log.debug("Executing python 'logout' authenticator method");
				CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
				Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
				return externalAuthenticator.logout(configurationAttributes, requestParameters);
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex);
			}
			return false;
        }

        return true;
	}

	public boolean executeExternalAuthenticatorPrepareForStep(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration, Map<String, String[]> requestParameters, int step) {
		try {
			log.debug("Executing python 'prepareForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return false;
	}

	public List<String> executeExternalAuthenticatorGetExtraParametersForStep(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration, int step) {
		try {
			log.debug("Executing python 'getPageForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getExtraParametersForStep(configurationAttributes, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public String executeExternalAuthenticatorGetPageForStep(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration, int step) {
		try {
			log.debug("Executing python 'getPageForStep' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			Map<String, SimpleCustomProperty> configurationAttributes = externalAuthenticatorConfiguration.getConfigurationAttributes();
			return externalAuthenticator.getPageForStep(configurationAttributes, step);
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return null;
	}

	public int executeExternalAuthenticatorGetApiVersion(ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
		try {
			log.debug("Executing python 'getApiVersion' authenticator method");
			CustomAuthenticatorType externalAuthenticator = externalAuthenticatorConfiguration.getExternalAuthenticatorType();
			return externalAuthenticator.getApiVersion();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		
		return -1;
	}

	public CustomAuthenticatorType createExternalAuthenticatorFromStringWithPythonException(CustomAuthenticationConfiguration customAuthenticationConfiguration, Map<String, SimpleCustomProperty> configurationAttributes) throws PythonException {
		String customAuthenticationScript = customAuthenticationConfiguration.getCustomAuthenticationScript();
		if (customAuthenticationScript == null) {
			return null;
		}

		CustomAuthenticatorType externalAuthenticatorType = null;

		externalAuthenticatorType = createExternalAuthenticator(customAuthenticationScript, externalAuthenticatorType);
		if (externalAuthenticatorType == null) {
			return null;
		}

		boolean initialized = false;
		try {
			initialized = externalAuthenticatorType.init(configurationAttributes);
		} catch (Exception ex) {
            log.error("Failed to initialize custom authenticator", ex);
		}

		if (initialized) {
			return externalAuthenticatorType;
		}
		
		return null;
	}

	private CustomAuthenticatorType createExternalAuthenticator(String customAuthenticationScript,
			CustomAuthenticatorType externalAuthenticatorType) throws PythonException {
		InputStream bis = null;
		try {
            bis = new ByteArrayInputStream(customAuthenticationScript.getBytes(Util.UTF8_STRING_ENCODING));
            externalAuthenticatorType = pythonService.loadPythonScript(bis, PYTHON_ENTRY_INTERCEPTOR_TYPE,
					CustomAuthenticatorType.class, new PyObject[] { new PyLong(System.currentTimeMillis()) });
		} catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        } finally {
			IOUtils.closeQuietly(bis);
		}
		return externalAuthenticatorType;
	}

	public boolean isEnabled(AuthenticationScriptUsageType usageType) {
		return this.externalAuthenticatorConfigurationsByUsageType.get(usageType).size() > 0;
	}

	public ExternalAuthenticatorConfiguration getExternalAuthenticatorByAuthLevel(AuthenticationScriptUsageType usageType, int authLevel) {
		ExternalAuthenticatorConfiguration resultDefaultExternalAuthenticator = null;
		for (ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration : this.externalAuthenticatorConfigurationsByUsageType.get(usageType)) {
			// Determine authenticator
			if (externalAuthenticatorConfiguration.getLevel() != authLevel) {
				continue;
			}

			if ((resultDefaultExternalAuthenticator == null) ||
					(resultDefaultExternalAuthenticator.getPriority() > externalAuthenticatorConfiguration.getPriority())) {
				resultDefaultExternalAuthenticator = externalAuthenticatorConfiguration;
			}
		}

		return resultDefaultExternalAuthenticator;
	}

	public ExternalAuthenticatorConfiguration determineExternalAuthenticatorConfiguration(AuthenticationScriptUsageType usageType, int authStep, String authLevel, String authMode) {
        ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration = null;
        if (authStep == 1) {
            if (StringHelper.isNotEmpty(authMode)) {
                externalAuthenticatorConfiguration = getExternalAuthenticatorConfiguration(usageType, authMode);
            } else {
            	if (StringHelper.isNotEmpty(authLevel)) {
            		externalAuthenticatorConfiguration = getExternalAuthenticatorByAuthLevel(usageType, StringHelper.toInteger(authLevel));
            	} else {
            		externalAuthenticatorConfiguration = getDefaultExternalAuthenticator(usageType);
            	}
            }
        } else {
            externalAuthenticatorConfiguration = getExternalAuthenticatorConfiguration(usageType, authMode);
        }
        
        return externalAuthenticatorConfiguration;
	}

	public ExternalAuthenticatorConfiguration determineExternalAuthenticatorConfiguration(AuthenticationScriptUsageType usageType, List<String> acrValues) {
		List<String> authModes = getAuthModesByAcrValues(acrValues);
		if (authModes.size() > 0) {
			for (String authMode : authModes) {
				for (ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration : this.externalAuthenticatorConfigurationsByUsageType.get(usageType)) {
					if (StringHelper.equalsIgnoreCase(authMode, externalAuthenticatorConfiguration.getName())) {
						return externalAuthenticatorConfiguration;
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
				if (externalAuthenticatorConfigurations.containsKey(StringHelper.toLowerCase(authMode))) {
					authModes.add(authMode);
				}
			}
		}
		
		return authModes;
	}

	public ExternalAuthenticatorConfiguration determineExternalAuthenticatorForWorkflow(AuthenticationScriptUsageType usageType, ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
    	// Validate API version
        int apiVersion = executeExternalAuthenticatorGetApiVersion(externalAuthenticatorConfiguration);
        if (apiVersion > 2) {
        	String authMode = externalAuthenticatorConfiguration.getName();
        	log.debug("Validating auth_mode: '{0}'", authMode);

        	boolean isValidAuthenticationMethod = executeExternalAuthenticatorIsValidAuthenticationMethod(usageType, externalAuthenticatorConfiguration);
            if (!isValidAuthenticationMethod) {
            	log.warn("Current auth_mode: '{0}' isn't valid", authMode);

            	String alternativeAuthenticationMethod = executeExternalAuthenticatorGetAlternativeAuthenticationMethod(usageType, externalAuthenticatorConfiguration);
                if (StringHelper.isEmpty(alternativeAuthenticationMethod)) {
                	log.error("Failed to determine alternative authentication mode for auth_mode: '{0}'", authMode);
                    return null;
                } else {
                	ExternalAuthenticatorConfiguration alternativeExternalAuthenticatorConfiguration = getExternalAuthenticatorConfiguration(AuthenticationScriptUsageType.INTERACTIVE, alternativeAuthenticationMethod);
                    if (alternativeExternalAuthenticatorConfiguration == null) {
                        log.error("Failed to get alternative ExternalAuthenticatorConfiguration '{0}' for auth_mode: '{1}'", alternativeAuthenticationMethod, authMode);
                        return null;
                    } else {
                        return alternativeExternalAuthenticatorConfiguration;
                    }
                }
            }
        }
        
        return externalAuthenticatorConfiguration;
	}

	public ExternalAuthenticatorConfiguration getDefaultExternalAuthenticator(AuthenticationScriptUsageType usageType) {
		return this.defaultExternalAuthenticators.get(usageType);
	}

	public ExternalAuthenticatorConfiguration getExternalAuthenticatorConfiguration(AuthenticationScriptUsageType usageType, String name) {
		for (ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration : this.externalAuthenticatorConfigurationsByUsageType.get(usageType)) {
			if (StringHelper.equalsIgnoreCase(name, externalAuthenticatorConfiguration.getName())) {
				return externalAuthenticatorConfiguration;
			}
		}
		
		return null;
	}

	public ExternalAuthenticatorConfiguration getExternalAuthenticatorConfiguration(String name) {
		for (Entry<String, ExternalAuthenticatorConfiguration> externalAuthenticatorConfigurationEntry : this.externalAuthenticatorConfigurations.entrySet()) {
			if (StringHelper.equalsIgnoreCase(name, externalAuthenticatorConfigurationEntry.getKey())) {
				return externalAuthenticatorConfigurationEntry.getValue();
			}
		}
		
		return null;
	}

	public List<ExternalAuthenticatorConfiguration> getExternalAuthenticatorConfigurations() {
		return new ArrayList<ExternalAuthenticatorConfiguration>(this.externalAuthenticatorConfigurations.values());
	}

	public  List<String> getAcrValuesList() {
		List<String> acrValues = new ArrayList<String>();

		for (Entry<String, ExternalAuthenticatorConfiguration> externalAuthenticatorConfigurationEntry : this.externalAuthenticatorConfigurations.entrySet()) {
			String acrValue = ACR_METHOD_PREFIX + externalAuthenticatorConfigurationEntry.getKey();
			acrValues.add(acrValue);
		}
		
		return acrValues;
	}

	private boolean isValidateUsageType(AuthenticationScriptUsageType usageType, ExternalAuthenticatorConfiguration externalAuthenticatorConfiguration) {
		if (externalAuthenticatorConfiguration == null) {
			return false;
		}
		
		AuthenticationScriptUsageType externalAuthenticatorUsageType = externalAuthenticatorConfiguration.getUsageType();
		
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
