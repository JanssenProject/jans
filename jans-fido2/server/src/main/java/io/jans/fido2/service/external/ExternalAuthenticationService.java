/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.external;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.fido2.service.cdi.event.ReloadAuthScript;
import io.jans.fido2.service.external.internal.InternalDefaultPersonAuthenticationType;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.model.auth.AuthenticationCustomScript;
import io.jans.model.custom.script.type.auth.PersonAuthenticationType;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.service.custom.script.ExternalScriptService;
import io.jans.util.OxConstants;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public CustomScriptConfiguration getCustomScriptConfigurationByName(String name) {
        for (Entry<String, CustomScriptConfiguration> customScriptConfigurationEntry : this.customScriptConfigurationsNameMap.entrySet()) {
            if (StringHelper.equalsIgnoreCase(scriptName(name), customScriptConfigurationEntry.getKey())) {
                return customScriptConfigurationEntry.getValue();
            }
        }

        return null;
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

}
