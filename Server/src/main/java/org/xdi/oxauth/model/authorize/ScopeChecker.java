/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.authorize;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import javax.enterprise.context.ApplicationScoped;
import org.jboss.seam.annotations.AutoCreate;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import javax.inject.Named;
import org.jboss.seam.annotations.Scope;

import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.ScopeService;

/**
 * Validates the scopes received for the authorize web service.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version June 3, 2015
 */
@Stateless
@Named("scopeChecker")
public class ScopeChecker {

    @Inject
    private Logger log;

    @Inject
    private ScopeService scopeService;

    public Set<String> checkScopesPolicy(Client client, String scope) {
        log.debug("Checking scopes policy for: " + scope);
        Set<String> grantedScopes = new HashSet<String>();

        final String[] scopesRequested = scope.split(" ");
        final String[] scopesAllowed = client.getScopes();

        for (String scopeRequested : scopesRequested) {
            if (StringUtils.isNotBlank(scopeRequested)) {
                for (String scopeAllowedDn : scopesAllowed) {
                    org.xdi.oxauth.model.common.Scope scopeAllowed = scopeService.getScopeByDnSilently(scopeAllowedDn);
                    if (scopeAllowed != null) {
                        String scopeAllowedName = scopeAllowed.getDisplayName();
                        if (scopeRequested.equals(scopeAllowedName)) {
                            grantedScopes.add(scopeRequested);
                        }
                    }
                }
            }
        }

        log.debug("Granted scopes: " + grantedScopes);

        return grantedScopes;
    }

    /**
     * Get ScopeChecker instance
     *
     * @return ScopeChecker instance
     */
    public static ScopeChecker instance() {
        return (ScopeChecker) Component.getInstance(ScopeChecker.class);
    }

}