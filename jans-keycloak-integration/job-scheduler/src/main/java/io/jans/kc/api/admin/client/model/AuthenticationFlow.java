package io.jans.kc.api.admin.client.model;

import org.keycloak.representations.idm.AuthenticationFlowRepresentation;

public class AuthenticationFlow {
    
    private AuthenticationFlowRepresentation flowRepresentation;

    public AuthenticationFlow(AuthenticationFlowRepresentation flowRepresentation) {

        this.flowRepresentation = flowRepresentation;
    }

    public String getId() {

        return flowRepresentation.getId();
    }

    public String getAlias() {

        return flowRepresentation.getAlias();
    }

    public String getDescription() {

        return flowRepresentation.getDescription();
    }
}
