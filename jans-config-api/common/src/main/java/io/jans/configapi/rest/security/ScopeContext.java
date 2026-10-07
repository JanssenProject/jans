/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.security;

import jakarta.enterprise.context.RequestScoped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RequestScoped
public class ScopeContext {

    private Set<String> scopes = Collections.emptySet();
    private String subject;

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes == null ? Collections.emptySet() : new HashSet<>(scopes);
    }

    public Set<String> getScopes() {
        return Collections.unmodifiableSet(scopes);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }

    public boolean hasAnyScope(String... required) {
        for (String s : required) {
            if (scopes.contains(s)) return true;
        }
        return false;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }
}
