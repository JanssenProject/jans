/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.authorization;

import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.UnmodifiableAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.ClaimTokenList;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.ExternalUmaAuthorizationPolicyService;
import org.xdi.oxauth.service.uma.ScopeService;

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
public class AuthorizationService {

    @Inject
    private Logger log;

    @Inject
    private ScopeService umaScopeService;

    @Inject
    private ExternalUmaAuthorizationPolicyService externalUmaAuthorizationPolicyService;

    @Inject
	private AttributeService attributeService;

    public boolean allowToAddPermission(AuthorizationGrant grant, UmaRPT rpt, ResourceSetPermission permission, HttpServletRequest httpRequest, ClaimTokenList claims) {
        log.trace("Check policies for permission, id: '{}'", permission.getDn());
        List<ScopeDescription> scopes = umaScopeService.getScopesByDns(permission.getScopeDns());
        return allowToAddPermission(grant, rpt, scopes, permission, httpRequest, claims);
    }

    public boolean allowToAddPermissionForGat(AuthorizationGrant grant, UmaRPT rpt, List<String> scopes, HttpServletRequest httpRequest, ClaimTokenList claims) {
        List<ScopeDescription> scopesByUrls = umaScopeService.getScopesByUrls(scopes);
        return allowToAddPermission(grant, rpt, scopesByUrls, new ResourceSetPermission(), httpRequest, claims);
    }

    public boolean allowToAddPermission(AuthorizationGrant grant, UmaRPT rpt, List<ScopeDescription> scopes,
                                        ResourceSetPermission permission, HttpServletRequest httpRequest, ClaimTokenList claims) {
        log.trace("Check policies for scopes: '{}'", scopes);

        Set<String> authorizationPolicies = getAuthorizationPolicies(scopes);

        if (authorizationPolicies == null || authorizationPolicies.isEmpty()) {
            log.trace("No policies protection, allowed to grant permission.");
            return true;
        } else {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(grant);
            final AuthorizationContext context = new AuthorizationContext(attributeService, rpt, permission, unmodifiableAuthorizationGrant, httpRequest, claims);
            for (String authorizationPolicy : authorizationPolicies) {
                // if at least one policy returns false then whole result is false
                if (!applyPolicy(authorizationPolicy, context)) {
                    log.trace("Reject access. Policy dn: '{}'", authorizationPolicy);
                    return false;
                }
            }

            log.trace("All policies are ok, grant access.");
            return true;
        }
    }

    private Set<String> getAuthorizationPolicies(List<ScopeDescription> scopes) {
        HashSet<String> result = new HashSet<String>();

        for (ScopeDescription scope : scopes) {
            List<String> authorizationPolicies = scope.getAuthorizationPolicies();
            if (authorizationPolicies != null) {
                result.addAll(authorizationPolicies);
            }
        }

        return result;
    }

    private boolean applyPolicy(String authorizationPolicyDn, AuthorizationContext authorizationContext) {
        log.trace("Apply policy dn: '{}' ...", authorizationPolicyDn);

        final CustomScriptConfiguration customScriptConfiguration = externalUmaAuthorizationPolicyService.getAuthorizationPolicyByDn(authorizationPolicyDn);
        if (customScriptConfiguration != null) {
            final boolean result = externalUmaAuthorizationPolicyService.executeExternalAuthorizeMethod(customScriptConfiguration, authorizationContext);
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
