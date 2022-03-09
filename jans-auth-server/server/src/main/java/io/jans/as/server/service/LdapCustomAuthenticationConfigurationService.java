/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.as.common.service.common.ConfigurationService;
import io.jans.as.persistence.model.configuration.CustomProperty;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.as.persistence.model.configuration.IDPAuthConf;
import io.jans.model.AuthenticationScriptUsageType;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.config.CustomAuthenticationConfiguration;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides service methods methods with LDAP configuration
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@Stateless
@Named
public class LdapCustomAuthenticationConfigurationService implements Serializable {

    private static final long serialVersionUID = -2225890597520443390L;

    private static final String CUSTOM_AUTHENTICATION_SCRIPT_PROPERTY_NAME = "script.__$__customAuthenticationScript__$__";
    private static final String CUSTOM_AUTHENTICATION_PROPERTY_PREFIX = "property.";
    private static final String CUSTOM_AUTHENTICATION_SCRIPT_USAGE_TYPE = "usage.";

    @Inject
    private Logger log;

    @Inject
    private ConfigurationService configurationService;

    public List<CustomAuthenticationConfiguration> getCustomAuthenticationConfigurations() {
        GluuConfiguration gluuConfiguration = configurationService.getConfiguration();
        List<IDPAuthConf> authConfigurations = gluuConfiguration.getIdpAuthn();

        List<CustomAuthenticationConfiguration> customAuthenticationConfigurations = new ArrayList<CustomAuthenticationConfiguration>();

        if (authConfigurations == null) {
            return customAuthenticationConfigurations;
        }

        for (IDPAuthConf authConfiguration : authConfigurations) {
            if (authConfiguration.getEnabled() && authConfiguration.getType().equalsIgnoreCase("customAuthentication")) {
                CustomAuthenticationConfiguration customAuthenticationConfiguration = mapCustomAuthentication(authConfiguration);
                customAuthenticationConfigurations.add(customAuthenticationConfiguration);
            }
        }

        return customAuthenticationConfigurations;
    }

    private CustomAuthenticationConfiguration mapCustomAuthentication(IDPAuthConf oneConf) {
        CustomAuthenticationConfiguration customAuthenticationConfig = new CustomAuthenticationConfiguration();
        customAuthenticationConfig.setName(oneConf.getName());
        customAuthenticationConfig.setLevel(oneConf.getLevel());
        customAuthenticationConfig.setPriority(oneConf.getPriority());
        customAuthenticationConfig.setEnabled(oneConf.getEnabled());
        customAuthenticationConfig.setVersion(oneConf.getVersion());

        for (CustomProperty customProperty : oneConf.getFields()) {
            if ((customProperty.getValues() == null) || (customProperty.getValues().size() == 0)) {
                continue;
            }

            String attrName = StringHelper.toLowerCase(customProperty.getName());

            if (StringHelper.isEmpty(attrName)) {
                continue;
            }

            String value = customProperty.getValues().get(0);

            if (attrName.startsWith(CUSTOM_AUTHENTICATION_PROPERTY_PREFIX)) {
                String key = customProperty.getName().substring(CUSTOM_AUTHENTICATION_PROPERTY_PREFIX.length());
                SimpleCustomProperty property = new SimpleCustomProperty(key, value);
                customAuthenticationConfig.getCustomAuthenticationAttributes().add(property);
            } else if (StringHelper.equalsIgnoreCase(attrName, CUSTOM_AUTHENTICATION_SCRIPT_PROPERTY_NAME)) {
                customAuthenticationConfig.setCustomAuthenticationScript(value);
            } else if (StringHelper.equalsIgnoreCase(attrName, CUSTOM_AUTHENTICATION_SCRIPT_USAGE_TYPE)) {
                if (StringHelper.isNotEmpty(value)) {
                    AuthenticationScriptUsageType authenticationScriptUsageType = AuthenticationScriptUsageType.getByValue(value);
                    customAuthenticationConfig.setUsageType(authenticationScriptUsageType);
                }
            }
        }

        return customAuthenticationConfig;
    }

    private Object jsonToObject(String json, Class<?> clazz) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

}
