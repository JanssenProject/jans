/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.ScopeService;
import org.gluu.oxauth.service.SpontaneousScopeService;
import org.gluu.oxauth.service.external.ExternalSpontaneousScopeService;
import org.gluu.oxauth.service.external.context.SpontaneousScopeExternalContext;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    @Inject
    private SpontaneousScopeService spontaneousScopeService;

    @Inject
    private ExternalSpontaneousScopeService externalSpontaneousScopeService;

    public Set<String> checkScopesPolicy(Client client, String scope) {
        log.debug("Checking scopes policy for: " + scope);
        Set<String> grantedScopes = new HashSet<>();

        if (scope == null || client == null) {
            return grantedScopes;
        }

        final String[] scopesRequested = scope.split(" ");
        String[] scopesAllowed = client.getScopes() != null ? client.getScopes() : new String[0];

        for (String scopeRequested : scopesRequested) {
            if (StringUtils.isBlank(scopeRequested)) {
                continue;
            }

            List<String> scopesAllowedIds = scopeService.getScopeIdsByDns(Arrays.asList(scopesAllowed));
            if (scopesAllowedIds.contains(scopeRequested)) {
                grantedScopes.add(scopeRequested);
                continue;
            }

            if (spontaneousScopeService.isAllowedBySpontaneousScopes(client, scopeRequested)) {
                grantedScopes.add(scopeRequested);

                spontaneousScopeService.createSpontaneousScopeIfNeeded(scopeRequested);
            }

            SpontaneousScopeExternalContext context = new SpontaneousScopeExternalContext(client, scopeRequested, scopesAllowedIds, spontaneousScopeService);
            externalSpontaneousScopeService.executeExternalManipulateScope(context);
        }

        log.debug("Granted scopes: " + grantedScopes);

        return grantedScopes;
    }
}