/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.uma.persistence.UmaPermission;
import org.gluu.oxauth.model.uma.persistence.UmaResource;
import org.gluu.oxauth.model.uma.persistence.UmaScopeDescription;
import org.gluu.oxauth.service.AttributeService;
import org.gluu.oxauth.service.external.context.ExternalScriptContext;
import org.gluu.oxauth.uma.service.RedirectParameters;
import org.gluu.oxauth.uma.service.UmaPermissionService;
import org.gluu.oxauth.uma.service.UmaSessionService;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.service.UserService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 */

public class UmaAuthorizationContext extends ExternalScriptContext {

    private final Claims claims;
    private final Map<UmaScopeDescription, Boolean> scopes; // scope and boolean, true - if client requested scope and false if it is permission ticket scope
    private final Set<UmaResource> resources;
    private final String scriptDn;
    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final AppConfiguration configuration;

    private final AttributeService attributeService;
    private final UmaSessionService sessionService;
    private final UserService userService;
    private final UmaPermissionService permissionService;
    private final Client client;

    public UmaAuthorizationContext(AppConfiguration configuration, AttributeService attributeService, Map<UmaScopeDescription, Boolean> scopes,
                                   Set<UmaResource> resources, Claims claims, String scriptDn, HttpServletRequest httpRequest,
                                   Map<String, SimpleCustomProperty> configurationAttributes, UmaSessionService sessionService,
                                   UserService userService, UmaPermissionService permissionService, Client client) {
    	super(httpRequest);

        this.configuration = configuration;
    	this.attributeService = attributeService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.permissionService = permissionService;
        this.client = client;
        this.scopes = new HashMap<UmaScopeDescription, Boolean>(scopes);
        this.resources = resources;
        this.claims = claims;
        this.scriptDn = scriptDn;
        this.configurationAttributes = configurationAttributes != null ? configurationAttributes : new HashMap<String, SimpleCustomProperty>();
    }

    public String getClaimToken() {
        return getClaims().getClaimsTokenAsString();
    }

    public Object getClaimTokenClaim(String key) {
        return getClaims().getClaimTokenClaim(key);
    }

    public Object getPctClaim(String key) {
        return getClaims().getPctClaim(key);
    }

    public String getIssuer() {
        return configuration.getIssuer();
    }

    public String getScriptDn() {
        return scriptDn;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
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
        return Maps.newHashMap(scopes);
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

    public Claims getClaims() {
        return claims;
    }

    public Object getClaim(String claimName) {
        return claims.get(claimName);
    }

    public void putClaim(String claimName, Object claimValue) {
        claims.put(claimName, claimValue);
    }

    public boolean hasClaim(String claimName) {
        return claims.has(claimName);
    }

    public void removeClaim(String claimName) {
        claims.removeClaim(claimName);
    }

    public void addRedirectUserParam(String paramName, String paramValue) {
        redirectUserParameters.add(paramName, paramValue);
    }

    public void removeRedirectUserParameter(String paramName) {
        redirectUserParameters.remove(paramName);
    }

    public RedirectParameters getRedirectUserParameters() {
        return redirectUserParameters;
    }

    public Map<String, Set<String>> getRedirectUserParametersMap() {
        return redirectUserParameters.map();
    }

    public User getUser(String... returnAttributes) {
        return sessionService.getUser(httpRequest, returnAttributes);
    }

    public boolean isAuthenticated() {
        return getUser() != null;
    }

    public String getUserDn() {
        return sessionService.getUserDn(httpRequest);
    }

    public Client getClient() {
        return client;
    }

    public List<UmaPermission> getPermissions() {
        SessionId session = sessionService.getSession(httpRequest, httpResponse);
        if (session == null) {
            getLog().trace("No UMA session set.");
            return Lists.newArrayList();
        }
        return permissionService.getPermissionsByTicket(sessionService.getTicket(session));
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
