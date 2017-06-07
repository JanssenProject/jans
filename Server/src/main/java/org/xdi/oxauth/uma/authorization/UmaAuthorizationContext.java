/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.context.ExternalScriptContext;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */

public class UmaAuthorizationContext extends ExternalScriptContext {

    private final Claims claims;
    private final Map<UmaScopeDescription, Boolean> scopes; // scope and boolean, true - if client requested scope and false if it is permission ticket scope
    private final Set<UmaResource> resources;
    private final String scriptDn;

    private AttributeService attributeService;

    public UmaAuthorizationContext(AttributeService attributeService, Map<UmaScopeDescription, Boolean> scopes,
                                   Set<UmaResource> resources, Claims claims, String scriptDn, HttpServletRequest httpRequest) {
    	super(httpRequest);

    	this.attributeService = attributeService;
        this.scopes = new HashMap<UmaScopeDescription, Boolean>(scopes);
        this.resources = resources;
        this.claims = claims;
        this.scriptDn = scriptDn;
    }

    public Set<String> getScopes() {
        Set<String> result = new HashSet<String>();
        for (UmaScopeDescription scope : getScopeMap().keySet()) {
            result.add(scope.getId());
        }
        return result;
    }

    /**
     * @return scopes that are bound to currently executed script
     */
    public Set<String> getScriptScopes() {
        Set<String> result = new HashSet<String>();
        for (UmaScopeDescription scope : getScopeMap().keySet()) {
            if (scope.getAuthorizationPolicies() != null && scope.getAuthorizationPolicies().contains(scriptDn)) {
                result.add(scope.getId());
            }
        }
        return result;
    }

    public Map<UmaScopeDescription, Boolean> getScopeMap() {
        return scopes;
    }

    public Set<UmaResource> getResources() {
        return resources;
    }

    public Set<String> getResourceIds() {
        Set<String> result = new HashSet<String>();
        for (UmaResource resource : resources) {
            result.add(resource.getId());
        }
        return result;
    }

    public Object getClaim(String claimName) {
        if (StringUtils.isNotBlank(claimName) && claims != null) {
            return claims.get(claimName);
        }
        return null;
    }

//    public String getClientClaim(String p_claimName) {
//        return getEntryAttributeValue(getGrant().getClientDn(), p_claimName);
//    }
//
//    public String getUserClaim(String p_claimName) {
//        GluuAttribute gluuAttribute = attributeService.getByClaimName(p_claimName);
//
//        if (gluuAttribute != null) {
//            String ldapClaimName = gluuAttribute.getName();
//            return getEntryAttributeValue(getGrant().getUserDn(), ldapClaimName);
//        }
//
//        return null;
//    }
//
//    public String getUserClaimByLdapName(String p_ldapName) {
//        return getEntryAttributeValue(getGrant().getUserDn(), p_ldapName);
//    }
//
//    public CustomEntry getUserClaimEntryByLdapName(String ldapName) {
//        return getEntryByDn(getGrant().getUserDn(), ldapName);
//    }
//
//    public CustomEntry getClientClaimEntry(String ldapName) {
//        return getEntryByDn(getGrant().getClientDn(), ldapName);
//    }

}
