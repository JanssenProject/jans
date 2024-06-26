package io.jans.kc.spi.rest;

import org.keycloak.Config.Scope;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import io.jans.kc.spi.ProviderIDs;

public class JansAuthResponseResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = ProviderIDs.JANS_AUTH_RESPONSE_REST_PROVIDER;

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
        //nothing to do here post init 
    }

    @Override
    public void close() {
        //nothing to do here on close
    }
}