/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.UnmodifiableAuthorizationGrant;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.ExternalUmaAuthorizationPolicyService;
import org.xdi.oxauth.uma.service.UmaScopeService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */
@Stateless
@Named("umaAuthorizationService")
public class UmaAuthorizationService {

    @Inject
    private Logger log;

    @Inject
    private UmaScopeService umaScopeService;

    @Inject
    private ExternalUmaAuthorizationPolicyService policyService;

    @Inject
	private AttributeService attributeService;

    public boolean allowToAddPermission(AuthorizationGrant grant, UmaRPT rpt, UmaPermission permission, HttpServletRequest httpRequest, Claims claims) {
        log.trace("Check policies for permission, id: '{}'", permission.getDn());
        List<UmaScopeDescription> scopes = umaScopeService.getScopesByDns(permission.getScopeDns());
        return allowToAddPermission(grant, rpt, scopes, permission, httpRequest, claims);
    }

    public boolean allowToAddPermission(AuthorizationGrant grant, UmaRPT rpt, List<UmaScopeDescription> scopes,
                                        UmaPermission permission, HttpServletRequest httpRequest, Claims claims) {
        log.trace("Check policies for scopes: '{}'", scopes);

        Set<String> authorizationPolicyDNs = getAuthorizationPolicyDNs(scopes);

        if (authorizationPolicyDNs == null || authorizationPolicyDNs.isEmpty()) {
            log.trace("No policies protection, allowed to grant permission.");
            return true;
        } else {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(grant);
            final UmaAuthorizationContext context = new UmaAuthorizationContext(attributeService, rpt, permission, unmodifiableAuthorizationGrant, httpRequest, claims);
            for (String authorizationPolicyDn : authorizationPolicyDNs) {
                // if at least one policy returns false then whole result is false
                if (!applyPolicy(authorizationPolicyDn, context)) {
                    log.trace("Reject access. Policy dn: '{}'", authorizationPolicyDn);
                    return false;
                }
            }

            log.trace("All policies are ok, grant access.");
            return true;
        }
    }

    public Set<String> getAuthorizationPolicyDNsByScopeIds(List<String> scopeIds) {
        return getAuthorizationPolicyDNs(umaScopeService.getScopesByIds(scopeIds));
    }

    public Set<String> getAuthorizationPolicyDNs(List<UmaScopeDescription> scopes) {
        HashSet<String> result = new HashSet<String>();

        for (UmaScopeDescription scope : scopes) {
            List<String> authorizationPolicies = scope.getAuthorizationPolicies();
            if (authorizationPolicies != null) {
                result.addAll(authorizationPolicies);
            }
        }

        return result;
    }

    private boolean applyPolicy(String authorizationPolicyDn, UmaAuthorizationContext authorizationContext) {
        log.trace("Apply policy dn: '{}' ...", authorizationPolicyDn);

        final CustomScriptConfiguration customScriptConfiguration = policyService.getAuthorizationPolicyByDn(authorizationPolicyDn);
        if (customScriptConfiguration != null) {
            final boolean result = policyService.authorize(customScriptConfiguration, authorizationContext);
            log.trace("Policy '{}' result: {}", authorizationPolicyDn, result);

            // if false check whether "need_info" objects are set, if yes then throw WebApplicationException directly here
            if (!result) {
                if (authorizationContext.getNeedInfoAuthenticationContext() != null || authorizationContext.getNeedInfoRequestingPartyClaims() != null) {
                    final String jsonEntity = NeedInfoResponseBuilder.entityForResponse(
                            authorizationContext.getNeedInfoAuthenticationContext(), authorizationContext.getNeedInfoRequestingPartyClaims());
                    throwForbiddenException(jsonEntity);
                }

            }
            return result;
        } else {
            log.error("Unable to load custom script dn: '{}'", authorizationPolicyDn);
        }

        return false;
    }

    private static void throwForbiddenException(String entity) {
        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .entity(entity).build());
    }

}
