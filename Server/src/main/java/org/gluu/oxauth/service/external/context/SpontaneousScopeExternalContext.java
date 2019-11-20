package org.gluu.oxauth.service.external.context;

import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.SpontaneousScopeService;

import java.util.List;

public class SpontaneousScopeExternalContext extends ExternalScriptContext {

    private Client client;
    private String scopeRequested;
    private List<String> grantedScopes;
    private SpontaneousScopeService spontaneousScopeService;

    public SpontaneousScopeExternalContext(Client client, String scopeRequested, List<String> grantedScopes, SpontaneousScopeService spontaneousScopeService) {
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

    public List<String> getGrantedScopes() {
        return grantedScopes;
    }

    public SpontaneousScopeService getSpontaneousScopeService() {
        return spontaneousScopeService;
    }

    @Override
    public String toString() {
        return "SpontaneousScopeExternalContext{" +
                "scopeRequested='" + scopeRequested + '\'' +
                ", grantedScopes=" + grantedScopes +
                ", contextVariables=" + super.getContextVariables() +
                '}';
    }
}
