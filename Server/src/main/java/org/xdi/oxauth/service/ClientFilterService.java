/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.configuration.Configuration;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version March 4, 2016
 */
@Scope(ScopeType.APPLICATION)
@Name("clientFilterService")
@AutoCreate
@Startup
public class ClientFilterService extends BaseAuthFilterService {

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    @Create
    public void init() {
        final Configuration conf = ConfigurationFactory.instance().getConfiguration();
        super.init(conf.getClientAuthenticationFilters(), Boolean.TRUE.equals(conf.getClientAuthenticationFiltersEnabled()), false);
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

        return resultDn;
    }

    public static ClientFilterService instance() {
        return (ClientFilterService) Component.getInstance(ClientFilterService.class);
    }
}
