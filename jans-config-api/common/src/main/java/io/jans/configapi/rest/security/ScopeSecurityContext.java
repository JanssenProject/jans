/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Set;

public class ScopeSecurityContext implements SecurityContext {

    private final Principal principal;
    private final Set<String> scopes;

    public ScopeSecurityContext(String subject, Set<String> scopes) {
        this.principal = () -> subject;
        this.scopes = scopes;
    }

    @Override public Principal getUserPrincipal() { return principal; }
    @Override public boolean isUserInRole(String role) { return scopes.contains(role); }
    @Override public boolean isSecure() { return true; }
    @Override public String getAuthenticationScheme() { return "Bearer"; }
}