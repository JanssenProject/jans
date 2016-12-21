/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.AuthenticationException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.Configuration;
import org.xdi.util.StringHelper;

import java.util.Map;

/**
 * Provides operations with authentication filters
 *
 * @author Yuriy Movchan Date: 07.20.2012
 */
@Scope(ScopeType.APPLICATION)
@Name("authenticationFilterService")
@AutoCreate
@Startup
public class AuthenticationFilterService extends BaseAuthFilterService {

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private ConfigurationFactory configurationFactory;

    private Configuration configuration;

    @Create
    public void init() {
        updateConfiguration();
        super.init(configuration.getAuthenticationFilters(), Boolean.TRUE.equals(configuration.getAuthenticationFiltersEnabled()), true);
    }

    @Observer( ConfigurationFactory.CONFIGURATION_UPDATE_EVENT )
    public void updateConfiguration() {
        this.configuration = configurationFactory.getConfiguration();
    }

    public String processAuthenticationFilter(AuthenticationFilterWithParameters authenticationFilterWithParameters, Map<?, ?> attributeValues) {
        if (attributeValues == null) {
            return null;
        }
        final Map<String, String> normalizedAttributeValues = normalizeAttributeMap(attributeValues);
        final String resultDn = loadEntryDN(ldapEntryManager, authenticationFilterWithParameters, normalizedAttributeValues);
        if (StringUtils.isBlank(resultDn)) {
            return null;
        }

        if (!Boolean.TRUE.equals(authenticationFilterWithParameters.getAuthenticationFilter().getBind())) {
            return resultDn;
        }

        String bindPasswordAttribute = authenticationFilterWithParameters.getAuthenticationFilter().getBindPasswordAttribute();
        if (StringHelper.isEmpty(bindPasswordAttribute)) {
            log.error("Skipping authentication filter:\n '{0}'\n. It should contains not empty bind-password-attribute attribute. ", authenticationFilterWithParameters.getAuthenticationFilter());
            return null;
        }

        bindPasswordAttribute = StringHelper.toLowerCase(bindPasswordAttribute);

        try {
            boolean authenticated = ldapEntryManager.authenticate(resultDn, normalizedAttributeValues.get(bindPasswordAttribute));
            if (authenticated) {
                return resultDn;
            }
        } catch (AuthenticationException ex) {
            log.error("Invalid password", ex);
        }

        return null;
    }

    public static AuthenticationFilterService instance() {
        return (AuthenticationFilterService) Component.getInstance(AuthenticationFilterService.class);
    }
}