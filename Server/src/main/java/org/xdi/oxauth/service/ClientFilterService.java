/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.Configuration;
import org.xdi.oxauth.model.config.ConfigurationFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/08/2012
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
        if (attributeValues != null) {
            return loadEntryDN(ldapEntryManager, authenticationFilterWithParameters, normalizeAttributeMap(attributeValues));
        }
        return null;
    }

    public String processFilters(String p_clientId) {
        return processFilters(p_clientId, "");
    }

    public String processFilters(String p_clientId, String p_clientSecret) {
        try {
            final Map<String, String> map = createMap(p_clientId, p_clientSecret);
            final List<AuthenticationFilterWithParameters> filters = getFilterWithParameters();
            if (filters != null && !filters.isEmpty()) {
                for (AuthenticationFilterWithParameters p : filters) {
                    final List<String> variableNames = p.getVariableNames();
                    if (variableNames != null && !variableNames.isEmpty()) {
                        for (String variable : variableNames) {
                            map.put(variable, p_clientId);
                        }
                    }
                }
            }
            return processAuthenticationFilters(map);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
        return "";
    }

    public static Map<String, String> createMap(String p_clientId, String p_clientSecret) {
        final Map<String, String> result = new HashMap<String, String>();
        if (StringUtils.isNotBlank(p_clientId)) {
            result.put("client_id", p_clientId);
        }
        if (StringUtils.isNotBlank(p_clientSecret)) {
            result.put("client_secret", p_clientSecret);
        }
        return result;
    }

    public static ClientFilterService instance() {
        return (ClientFilterService) Component.getInstance(ClientFilterService.class);
    }
}
