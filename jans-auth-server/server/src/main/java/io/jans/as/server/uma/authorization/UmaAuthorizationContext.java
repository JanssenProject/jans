/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.as.server.uma.service.RedirectParameters;
import io.jans.as.server.uma.service.UmaPermissionService;
import io.jans.as.server.uma.service.UmaSessionService;
import io.jans.model.SimpleCustomProperty;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 */

public class UmaAuthorizationContext extends ExternalScriptContext {

    private final Claims claims;
    private final Map<Scope, Boolean> scopes; // scope and boolean, true - if client requested scope and false if it is permission ticket scope
    private final Set<UmaResource> resources;
    private final String scriptDn;
    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final RedirectParameters redirectUserParameters = new RedirectParameters();
    private final AppConfiguration configuration;

    private final UmaSessionService sessionService;
    private final UmaPermissionService permissionService;
    private final Client client;

    public UmaAuthorizationContext(AppConfiguration configuration, Map<Scope, Boolean> scopes,
                                   Set<UmaResource> resources, Claims claims, String scriptDn, HttpServletRequest httpRequest,
                                   Map<String, SimpleCustomProperty> configurationAttributes, UmaSessionService sessionService,
                                   UmaPermissionService permissionService, Client client) {
        super(httpRequest);

        this.configuration = configuration;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.client = client;
        this.scopes = new HashMap<>(scopes);
        this.resources = resources;
        this.claims = claims;
        this.scriptDn = scriptDn;
        this.configurationAttributes = configurationAttributes != null ? configurationAttributes : new HashMap<>();
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
        Set<String> result = new HashSet<>();
        for (Scope scope : getScopeMap().keySet()) {
            result.add(scope.getId());
        }
        return result;
    }

    /**
     * @return scopes that are bound to currently executed script
     */
    public Set<String> getScriptScopes() {
        Set<String> result = new HashSet<>();
        for (Scope scope : getScopeMap().keySet()) {
            if (scope.getUmaAuthorizationPolicies() != null && scope.getUmaAuthorizationPolicies().contains(scriptDn)) {
                result.add(scope.getId());
            }
        }
        return result;
    }

    public Map<Scope, Boolean> getScopeMap() {
        return Maps.newHashMap(scopes);
    }

    public Set<UmaResource> getResources() {
        return resources;
    }

    public Set<String> getResourceIds() {
        Set<String> result = new HashSet<>();
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

    public User getUser() {
        return sessionService.getUser(httpRequest);
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
}
