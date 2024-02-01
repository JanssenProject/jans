package io.jans.kc.api.admin.client.model;

import org.keycloak.representations.idm.ClientRepresentation;

public class ManagedSamlClient {
    
    private String trustRelationshipInum;
    private ClientRepresentation clientRepresentation;

    public ManagedSamlClient(ClientRepresentation clientRepresentation, String trustRelationshipInum) {

        this.clientRepresentation = clientRepresentation;
        this.trustRelationshipInum = trustRelationshipInum;
    }

    public String trustRelationshipInum() {

        return this.trustRelationshipInum;
    }

    public String keycloakId() {

        return this.clientRepresentation.getId();
    }
}
