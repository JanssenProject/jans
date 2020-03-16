/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import com.google.common.collect.Sets;
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
        if (StringUtils.isBlank(scope)) {
            return Sets.newHashSet();
        }
        return checkScopesPolicy(client, Arrays.asList(scope.split(" ")));
    }

    public Set<String> checkScopesPolicy(Client client, List<String> scopesRequested) {
        log.debug("Checking scopes policy for: " + scopesRequested);
        Set<String> grantedScopes = new HashSet<>();

        if (scopesRequested == null || scopesRequested.isEmpty() || client == null) {
            return grantedScopes;
        }

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

                SpontaneousScopeExternalContext context = new SpontaneousScopeExternalContext(client, scopeRequested, grantedScopes, spontaneousScopeService);
                externalSpontaneousScopeService.executeExternalManipulateScope(context);

                if (context.isAllowSpontaneousScopePersistence()) {
                    spontaneousScopeService.createSpontaneousScopeIfNeeded(Sets.newHashSet(client.getAttributes().getSpontaneousScopes()), scopeRequested, client.getClientId());
                }
            }
        }

        log.debug("Granted scopes: " + grantedScopes);

        return grantedScopes;
    }
}