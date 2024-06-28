package io.jans.kc.spi.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

import io.jans.kc.spi.ProviderIDs;
import io.jans.kc.spi.JansSpiInitException;
import io.jans.kc.spi.custom.JansThinBridgeProvider;

import org.jboss.logging.Logger;

public class JansUserStorageProviderFactory implements UserStorageProviderFactory<JansUserStorageProvider> {
    

    private static final String PROVIDER_ID = ProviderIDs.JANS_USER_STORAGE_PROVIDER;
    private static final Logger log = Logger.getLogger(JansUserStorageProviderFactory.class);

    @Override
    public String getId() {

        return PROVIDER_ID;
    }

    @Override
    public JansUserStorageProvider create(KeycloakSession session, ComponentModel model) {

        JansThinBridgeProvider jansThinBridgeProvider = session.getProvider(JansThinBridgeProvider.class);
        if(jansThinBridgeProvider == null) {
            throw new JansSpiInitException("Could not obtain reference to thin bridge provider");
        }
        return new JansUserStorageProvider(session,model,jansThinBridgeProvider);
    }
}
