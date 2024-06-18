package io.jans.kc.spi.storage;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import io.jans.kc.model.JansUserModel;
import io.jans.kc.spi.custom.JansThinBridgeOperationException;
import io.jans.kc.spi.custom.JansThinBridgeProvider;

import org.jboss.logging.Logger;

public class JansUserStorageProvider implements UserStorageProvider, UserLookupProvider {
    
    private static final Logger log = Logger.getLogger(JansUserStorageProvider.class);

    private final JansThinBridgeProvider jansThinBridge;

    public JansUserStorageProvider(final JansThinBridgeProvider jansThinBridge) {

        this.jansThinBridge = jansThinBridge;
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {

        try {
            return jansThinBridge.getUserByUsername(username);
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with username " + username);
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {

        try {
            return jansThinBridge.getUserByEmail(email);
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with email " + email);
        }
        return null;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {

        try {
            StorageId storageId = new StorageId(id);
            final String inum = storageId.getExternalId();
            return jansThinBridge.getUserByInum(inum);
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with id " + id);
        }
        return null;
    }


}
