package io.jans.kc.spi.rest;

import org.keycloak.Config.Scope;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class JansAuthResponseResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = "janssen-auth-response-bridge";

    @Override
    public String getId() {
        
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {

        return new JansAuthResponseResourceProvider(session);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}