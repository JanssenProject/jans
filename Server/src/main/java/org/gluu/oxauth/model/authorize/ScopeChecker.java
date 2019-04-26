/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.ScopeService;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates the scopes received for the authorize web service.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version January 30, 2018
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

        if (scope == null || client == null) {
            return grantedScopes;
        }

        final String[] scopesRequested = scope.split(" ");
        String[] scopesAllowed = client.getScopes();

        // ocAuth #955
        if (scopesAllowed == null) {
            return grantedScopes;
        }

        for (String scopeRequested : scopesRequested) {
            if (StringUtils.isNotBlank(scopeRequested)) {
                for (String scopeAllowedDn : scopesAllowed) {
                    org.oxauth.persistence.model.Scope scopeAllowed = scopeService.getScopeByDnSilently(scopeAllowedDn);
                    if (scopeAllowed != null) {
                        String scopeAllowedName = scopeAllowed.getId();
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

}