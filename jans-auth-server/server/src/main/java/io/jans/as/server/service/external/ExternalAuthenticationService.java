/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.ReloadAuthScript;
import io.jans.as.server.service.external.internal.InternalDefaultPersonAuthenticationType;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.auth.AuthenticationCustomScript;
import io.jans.model.custom.script.type.BaseExternalType;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.service.custom.script.ExternalScriptService;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.*;
import java.util.Map.Entry;

/**
 * Provides factory methods needed to create external authenticator
 *
 * @author Yuriy Movchan Date: 21/08/2012
 */
@ApplicationScoped
public class ExternalAuthenticationService extends ExternalScriptService {

    public final static String MODIFIED_INTERNAL_TYPES_EVENT_TYPE = "CustomScriptModifiedInternlTypesEvent";

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_AUTH_CONFIG_NAME)
    private List<GluuLdapConfiguration> ldapAuthConfigs;

    @Inject
    private InternalDefaultPersonAuthenticationType internalDefaultPersonAuthenticationType;

    @Inject
    private AppConfiguration appConfiguration;

    private static final long serialVersionUID = 7339887464253044927L;

    private Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> customScriptConfigurationsMapByUsageType;
    private Map<AuthenticationScriptUsageType, CustomScriptConfiguration> defaultExternalAuthenticators;
    private Map<String, String> scriptAliasMap;

    public ExternalAuthenticationService() {
        super(CustomScriptType.PERSON_AUTHENTICATION);
    }

    public void reloadAuthScript(@Observes @ReloadAuthScript String event) {
        reload(event);
    }

    public String scriptName(String acr) {
        if (StringHelper.isEmpty(acr)) {
            return null;
        }

        if (scriptAliasMap.containsKey(acr)) {
            return scriptAliasMap.get(acr);
        }

        return acr;
    }

    @Override
    protected void reloadExternal() {
        // Group external authenticator configurations by usage type
        this.customScriptConfigurationsMapByUsageType = groupCustomScriptConfigurationsMapByUsageType(this.customScriptConfigurationsNameMap);

        // Build aliases map
        this.scriptAliasMap = buildScriptAliases();

        // Determine default authenticator for every usage type
        this.defaultExternalAuthenticators = determineDefaultCustomScriptConfigurationsMap(this.customScriptConfigurationsNameMap);
    }

    private HashMap<String, String> buildScriptAliases() {
        HashMap<String, String> newScriptAliases = new HashMap<String, String>();
        for (Entry<String, CustomScriptConfiguration> script : customScriptConfigurationsNameMap.entrySet()) {
            String name = script.getKey();
            CustomScript customScript = script.getValue().getCustomScript();

            newScriptAliases.put(name, name);

            List<String> aliases = customScript.getAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    if (StringUtils.isNotBlank(alias)) {
                        newScriptAliases.put(alias, name);
                    }
                }
            }
        }

        return newScriptAliases;
    }

    @Override
    protected void addExternalConfigurations(List<CustomScriptConfiguration> newCustomScriptConfigurations) {
        if ((ldapAuthConfigs == null) || (ldapAuthConfigs.size() == 0)) {
            // Add internal type only if there is no enabled scripts and external authentication configurations
            if (newCustomScriptConfigurations.size() == 0) {
                newCustomScriptConfigurations.add(getInternalCustomScriptConfiguration());
            }
        } else {
            for (GluuLdapConfiguration ldapAuthConfig : ldapAuthConfigs) {
                newCustomScriptConfigurations.add(getInternalCustomScriptConfiguration(ldapAuthConfig));
            }
        }
    }

    private Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> groupCustomScriptConfigurationsMapByUsageType(Map<String, CustomScriptConfiguration> customScriptConfigurationsMap) {
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

    private Map<AuthenticationScriptUsageType, CustomScriptConfiguration> determineDefaultCustomScriptConfigurationsMap(Map<String, CustomScriptConfiguration> customScriptConfigurationsMap) {
        Map<AuthenticationScriptUsageType, CustomScriptConfiguration> newDefaultCustomScriptConfigurationsMap = new HashMap<AuthenticationScriptUsageType, CustomScriptConfiguration>();

        for (AuthenticationScriptUsageType usageType : AuthenticationScriptUsageType.values()) {
            CustomScriptConfiguration defaultExternalAuthenticator = null;
            for (CustomScriptConfiguration customScriptConfiguration : customScriptConfigurationsMapByUsageType.get(usageType)) {
                // Determine default authenticator. It has bigger level than others
                if ((defaultExternalAuthenticator == null)
                        || (defaultExternalAuthenticator.getLevel() < customScriptConfiguration.getLevel())) {
                    defaultExternalAuthenticator = customScriptConfiguration;
                }
            }

            newDefaultCustomScriptConfigurationsMap.put(usageType, defaultExternalAuthenticator);
        }

        return newDefaultCustomScriptConfigurationsMap;
    }

    private boolean executeExternalIsValidAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.debug("Executing python 'isValidAuthenticationMethod' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.isValidAuthenticationMethod(usageType, configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    private String executeExternalGetAlternativeAuthenticationMethod(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.trace("Executing python 'getAlternativeAuthenticationMethod' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getAlternativeAuthenticationMethod(usageType, configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return null;
    }

    public int executeExternalGetCountAuthenticationSteps(CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.trace("Executing python 'getCountAuthenticationSteps' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getCountAuthenticationSteps(configurationAttributes);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return -1;
    }

    public boolean executeExternalAuthenticate(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
        try {
            log.trace("Executing python 'authenticate' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.authenticate(configurationAttributes, requestParameters, step);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public int getNextStep(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
        try {
            log.trace("Executing python 'getNextStep' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getNextStep(configurationAttributes, requestParameters, step);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return -1;
    }

    public boolean executeExternalLogout(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters) {
        try {
            log.trace("Executing python 'logout' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.logout(configurationAttributes, requestParameters);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public String getLogoutExternalUrl(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters) {
        try {
            log.trace("Executing python 'getLogouExternalUrl' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getLogoutExternalUrl(configurationAttributes, requestParameters);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return null;
    }

    public boolean executeExternalPrepareForStep(CustomScriptConfiguration customScriptConfiguration, Map<String, String[]> requestParameters, int step) {
        try {
            log.trace("Executing python 'prepareForStep' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.prepareForStep(configurationAttributes, requestParameters, step);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return false;
    }

    public List<String> executeExternalGetExtraParametersForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
        try {
            log.trace("Executing python 'getExtraParametersForStep' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getExtraParametersForStep(configurationAttributes, step);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return null;
    }

    public String executeExternalGetPageForStep(CustomScriptConfiguration customScriptConfiguration, int step) {
        try {
            log.trace("Executing python 'getPageForStep' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            return externalAuthenticator.getPageForStep(configurationAttributes, step);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);

            return null;
        }
    }

    public int executeExternalGetApiVersion(CustomScriptConfiguration customScriptConfiguration) {
        try {
            log.trace("Executing python 'getApiVersion' authenticator method");
            PersonAuthenticationType externalAuthenticator = (PersonAuthenticationType) customScriptConfiguration.getExternalType();
            return externalAuthenticator.getApiVersion();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            saveScriptError(customScriptConfiguration.getCustomScript(), ex);
        }

        return -1;
    }

    public boolean isEnabled(AuthenticationScriptUsageType usageType) {
        return this.customScriptConfigurationsMapByUsageType != null &&
                this.customScriptConfigurationsMapByUsageType.get(usageType).size() > 0;
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

    public CustomScriptConfiguration determineCustomScriptConfiguration(AuthenticationScriptUsageType usageType, int authStep, String acr) {
        CustomScriptConfiguration customScriptConfiguration;
        if (authStep == 1) {
            if (StringHelper.isNotEmpty(acr)) {
                customScriptConfiguration = getCustomScriptConfiguration(usageType, acr);
            } else {
                customScriptConfiguration = getDefaultExternalAuthenticator(usageType);
            }
        } else {
            customScriptConfiguration = getCustomScriptConfiguration(usageType, acr);
        }

        return customScriptConfiguration;
    }

    public CustomScriptConfiguration determineCustomScriptConfiguration(AuthenticationScriptUsageType usageType, List<String> acrValues) {
        List<String> authModes = getAuthModesByAcrValues(acrValues);

        if (authModes.size() > 0) {
            for (String authMode : authModes) {
                for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsMapByUsageType.get(usageType) ) {
                    if (StringHelper.equalsIgnoreCase(authMode, customScriptConfiguration.getName())) {
                        return customScriptConfiguration;
                    }
                }
            }
        }

        if (appConfiguration.getUseHighestLevelScriptIfAcrScriptNotFound()) {
            return getDefaultExternalAuthenticator(usageType);
        }
        return null;
    }

    public List<String> getAuthModesByAcrValues(List<String> acrValues) {
        List<String> authModes = new ArrayList<String>();

        for (String acrValue : acrValues) {
            if (StringHelper.isNotEmpty(acrValue)) {
                String customScriptName = StringHelper.toLowerCase(scriptName(acrValue));
                if (customScriptConfigurationsNameMap.containsKey(customScriptName)) {
                    CustomScriptConfiguration customScriptConfiguration = customScriptConfigurationsNameMap.get(customScriptName);
                    CustomScript customScript = customScriptConfiguration.getCustomScript();

                    // Handle internal authentication method
                    if (customScript.isInternal()) {
                        authModes.add(scriptName(acrValue));
                        continue;
                    }

                    CustomScriptType customScriptType = customScriptConfiguration.getCustomScript().getScriptType();
                    BaseExternalType defaultImplementation = customScriptType.getDefaultImplementation();
                    BaseExternalType pythonImplementation = customScriptConfiguration.getExternalType();
                    if ((pythonImplementation != null) && (defaultImplementation != pythonImplementation)) {
                        authModes.add(scriptName(acrValue));
                    }
                }
            }
        }
        return authModes;
    }

    public CustomScriptConfiguration determineExternalAuthenticatorForWorkflow(AuthenticationScriptUsageType usageType, CustomScriptConfiguration customScriptConfiguration) {
        String authMode = customScriptConfiguration.getName();
        log.trace("Validating acr_values: '{}'", authMode);

        boolean isValidAuthenticationMethod = executeExternalIsValidAuthenticationMethod(usageType, customScriptConfiguration);
        if (!isValidAuthenticationMethod) {
            log.warn("Current acr_values: '{}' isn't valid", authMode);

            String alternativeAuthenticationMethod = executeExternalGetAlternativeAuthenticationMethod(usageType, customScriptConfiguration);
            if (StringHelper.isEmpty(alternativeAuthenticationMethod)) {
                log.error("Failed to determine alternative authentication mode for acr_values: '{}'", authMode);
                return null;
            } else {
                CustomScriptConfiguration alternativeCustomScriptConfiguration = getCustomScriptConfiguration(AuthenticationScriptUsageType.INTERACTIVE, alternativeAuthenticationMethod);
                if (alternativeCustomScriptConfiguration == null) {
                    log.error("Failed to get alternative CustomScriptConfiguration '{}' for acr_values: '{}'", alternativeAuthenticationMethod, authMode);
                    return null;
                } else {
                    return alternativeCustomScriptConfiguration;
                }
            }
        }

        return customScriptConfiguration;
    }

    public CustomScriptConfiguration getDefaultExternalAuthenticator(AuthenticationScriptUsageType usageType) {
        if (this.defaultExternalAuthenticators != null) {
            return this.defaultExternalAuthenticators.get(usageType);
        }

        return null;
    }

    public CustomScriptConfiguration getCustomScriptConfiguration(AuthenticationScriptUsageType usageType, String name) {
        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurationsMapByUsageType.get(usageType)) {
            if (StringHelper.equalsIgnoreCase(scriptName(name), customScriptConfiguration.getName())) {
                return customScriptConfiguration;
            }
        }

        return null;
    }

    public CustomScriptConfiguration getCustomScriptConfigurationByName(String name) {
        for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurationsNameMap.entrySet()) {
            if (StringHelper.equalsIgnoreCase(scriptName(name), customScriptConfigurationEntry.getKey())) {
                return customScriptConfigurationEntry.getValue();
            }
        }

        return null;
    }

    public List<CustomScriptConfiguration> getCustomScriptConfigurationsMap() {
        if (this.customScriptConfigurationsNameMap == null) {
            return new ArrayList<>(0);
        }

        return new ArrayList<>(this.customScriptConfigurationsNameMap.values());
    }

    public List<String> getAcrValuesList() {
        return new ArrayList<String>(scriptAliasMap.keySet());
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

        return AuthenticationScriptUsageType.SERVICE.equals(usageType) && AuthenticationScriptUsageType.SERVICE.equals(externalAuthenticatorUsageType);
    }

    public Map<Integer, Set<String>> levelToAcrMapping() {
        Map<Integer, Set<String>> map = Maps.newHashMap();
        for (CustomScriptConfiguration script : getCustomScriptConfigurationsMap()) {
            int level = script.getLevel();
            String acr = script.getName();

            Set<String> acrs = map.get(level);
            if (acrs == null) {
                acrs = Sets.newHashSet();
                map.put(level, acrs);
            }
            acrs.add(acr);

            // Also publish alias configuration
            if (script.getCustomScript() != null && script.getCustomScript().getAliases() != null) {
                for (String alias : script.getCustomScript().getAliases()) {
                    if (StringUtils.isNotBlank(alias)) {
                        acrs.add(alias);
                    }
                }
            }
        }
        return map;
    }

    public Map<String, Integer> acrToLevelMapping() {
        Map<String, Integer> map = Maps.newHashMap();
        for (CustomScriptConfiguration script : getCustomScriptConfigurationsMap()) {
            if (script.getCustomScript().isInternal()) {
                map.put(script.getName(), -1);
                continue;
            }
            map.put(script.getName(), script.getLevel());
        }
        return map;
    }

    private CustomScriptConfiguration getInternalCustomScriptConfiguration(GluuLdapConfiguration ldapAuthConfig) {
        CustomScriptConfiguration customScriptConfiguration = getInternalCustomScriptConfiguration();
        customScriptConfiguration.getCustomScript().setName(ldapAuthConfig.getConfigId());

        return customScriptConfiguration;
    }

    private CustomScriptConfiguration getInternalCustomScriptConfiguration() {
        CustomScript customScript = new AuthenticationCustomScript() {
            @Override
            public AuthenticationScriptUsageType getUsageType() {
                return AuthenticationScriptUsageType.INTERACTIVE;
            }

        };
        customScript.setName(OxConstants.SCRIPT_TYPE_INTERNAL_RESERVED_NAME);
        customScript.setLevel(-1);
        customScript.setInternal(true);

        return new CustomScriptConfiguration(customScript, internalDefaultPersonAuthenticationType, new HashMap<>(0));
    }

    public void setCustomScriptConfigurationsMapByUsageType(Map<AuthenticationScriptUsageType, List<CustomScriptConfiguration>> customScriptConfigurationsMapByUsageType) {
        this.customScriptConfigurationsMapByUsageType = customScriptConfigurationsMapByUsageType;
    }

    public void setDefaultExternalAuthenticators(Map<AuthenticationScriptUsageType, CustomScriptConfiguration> defaultExternalAuthenticators) {
        this.defaultExternalAuthenticators = defaultExternalAuthenticators;
    }
}
