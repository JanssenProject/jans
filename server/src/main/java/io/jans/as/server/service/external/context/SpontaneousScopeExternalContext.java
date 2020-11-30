/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.service.SpontaneousScopeService;

import java.util.Set;

public class SpontaneousScopeExternalContext extends ExternalScriptContext {

    private Client client;
    private String scopeRequested;
    private Set<String> grantedScopes;
    private SpontaneousScopeService spontaneousScopeService;
    private boolean allowSpontaneousScopePersistence = true;

    public SpontaneousScopeExternalContext(Client client, String scopeRequested, Set<String> grantedScopes, SpontaneousScopeService spontaneousScopeService) {
        super(null, null);
        this.client = client;
        this.scopeRequested = scopeRequested;
        this.grantedScopes = grantedScopes;
        this.spontaneousScopeService = spontaneousScopeService;
    }

    public Client getClient() {
        return client;
    }

    public String getScopeRequested() {
        return scopeRequested;
    }

    public Set<String> getGrantedScopes() {
        return grantedScopes;
    }

    public SpontaneousScopeService getSpontaneousScopeService() {
        return spontaneousScopeService;
    }

    public boolean isAllowSpontaneousScopePersistence() {
        return allowSpontaneousScopePersistence;
    }

    public void setAllowSpontaneousScopePersistence(boolean allowSpontaneousScopePersistence) {
        this.allowSpontaneousScopePersistence = allowSpontaneousScopePersistence;
    }

    @Override
    public String toString() {
        return "SpontaneousScopeExternalContext{" +
                "scopeRequested='" + scopeRequested + '\'' +
                ", grantedScopes=" + grantedScopes +
                ", contextVariables=" + super.getContextVariables() +
                ", allowSpontaneousScopePersistence=" + allowSpontaneousScopePersistence +
                '}';
    }
}
