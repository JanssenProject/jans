/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.AuthenticationException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * Provides operations with authentication filters
 *
 * @author Yuriy Movchan Date: 07.20.2012
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class AuthenticationFilterService extends BaseAuthFilterService {

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

    @PostConstruct
    public void init() {
        super.init(appConfiguration.getAuthenticationFilters(), Boolean.TRUE.equals(appConfiguration.getAuthenticationFiltersEnabled()), true);
    }

    public String processAuthenticationFilter(AuthenticationFilterWithParameters authenticationFilterWithParameters, Map<?, ?> attributeValues) throws SearchException {
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
            log.error("Skipping authentication filter:\n '{}'\n. It should contains not empty bind-password-attribute attribute. ", authenticationFilterWithParameters.getAuthenticationFilter());
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

}