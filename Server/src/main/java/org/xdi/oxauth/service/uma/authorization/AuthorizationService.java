/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.authorization;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.UnmodifiableAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.service.external.ExternalUmaAuthorizationPolicyService;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.PythonService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */
@Scope(ScopeType.STATELESS)
@Name("umaAuthorizationService")
@AutoCreate
public class AuthorizationService {

    @Logger
    private Log log;
    
    @In
    private ScopeService umaScopeService;
    
    @In
    private ExternalUmaAuthorizationPolicyService externalUmaAuthorizationPolicyService;

    public boolean allowToAddPermission(AuthorizationGrant p_grant, UmaRPT p_rpt, ResourceSetPermission p_permission, HttpServletRequest httpRequest, RptAuthorizationRequest rptAuthorizationRequest) {
        log.trace("Check policies for permission, id: '{0}'", p_permission.getDn());
        List<ScopeDescription> scopes = umaScopeService.getScopesByDns(p_permission.getScopeDns());
        Set<String> authorizationPolicies = getAuthorizationPolicies(scopes);

        if (authorizationPolicies == null || authorizationPolicies.isEmpty()) {
            log.trace("No policies protection, allowed to grant permission.");
            return true;
        } else {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(p_grant);
            final AuthorizationContext context = new AuthorizationContext(p_rpt, p_permission, unmodifiableAuthorizationGrant, httpRequest, rptAuthorizationRequest.getClaims());
            for (String authorizationPolicy : authorizationPolicies) {
                // if at least one policy returns false then whole result is false
                if (!applyPolicy(authorizationPolicy, context)) {
                    log.trace("Reject access. Policy dn: '{0}'", authorizationPolicy);
                    return false;
                }
            }

            log.trace("All policies are ok, grant access.");
            return true;
        }
    }

    private Set<String> getAuthorizationPolicies(List<ScopeDescription> scopes) {
    	HashSet<String> result = new HashSet<String>();
    	
    	for (ScopeDescription scope : scopes)  {
    		List<String> authorizationPolicies = scope.getAuthorizationPolicies(); 
    		if (authorizationPolicies != null) {
    			result.addAll(authorizationPolicies);
    		}
    	}
    	
		return result;
	}

	private boolean applyPolicy(String authorizationPolicyDn, AuthorizationContext authorizationContext) {
        log.trace("Apply policy dn: '{0}' ...", authorizationPolicyDn);

        final CustomScriptConfiguration customScriptConfiguration = externalUmaAuthorizationPolicyService.getAuthorizationPolicyByDn(authorizationPolicyDn);
        if (customScriptConfiguration != null) {
        	final boolean result = externalUmaAuthorizationPolicyService.executeExternalAuthorizeMethod(customScriptConfiguration, authorizationContext);
			log.trace("Policy '{0}' result: {1}", authorizationPolicyDn, result);
			
			return result;
        } else {
            log.error("Unable to load cusom script dn: '{0}'", authorizationPolicyDn);
        }

        return false;
    }

    public static AuthorizationService instance() {
        return ServerUtil.instance(AuthorizationService.class);
    }
}
