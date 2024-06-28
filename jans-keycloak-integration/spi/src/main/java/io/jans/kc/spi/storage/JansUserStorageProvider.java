package io.jans.kc.spi.storage;

import org.keycloak.component.ComponentModel;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import io.jans.kc.model.JansUserModel;
import io.jans.kc.model.internal.JansPerson;
import io.jans.kc.spi.custom.JansThinBridgeOperationException;
import io.jans.kc.spi.custom.JansThinBridgeProvider;

import org.jboss.logging.Logger;

public class JansUserStorageProvider implements UserStorageProvider, UserLookupProvider {
    
    private static final Logger log = Logger.getLogger(JansUserStorageProvider.class);

    private final JansThinBridgeProvider jansThinBridge;
    private final ComponentModel model;
    private final KeycloakSession session;

    public JansUserStorageProvider(KeycloakSession session,ComponentModel model, JansThinBridgeProvider jansThinBridge) {

        this.session = session;
        this.model = model;
        this.jansThinBridge = jansThinBridge;
    }

    @Override
    public void close() {

    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {

        try {
            log.infov("getUserByUsername(). Username: {0}",username);
            JansPerson person = jansThinBridge.getJansUserByUsername(username);
            if(person != null) {
                return new JansUserModel(model,person);
            }
            return null;
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with username " + username);
            return null;
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {

        try {
            log.infov("getUserByEmail(). Email : {0}",email);
            JansPerson person = jansThinBridge.getJansUserByEmail(email);
            if(person != null) {
                return new JansUserModel(model,person);
            }
            return null;
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with email " + email);
            return null;
        }
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {

        try {
            log.infov("getUserById(). Id: {0}",id);
            StorageId storageId = new StorageId(id);
            final String inum = storageId.getExternalId();
            JansPerson person = jansThinBridge.getJansUserByInum(inum);
            if(person != null) {
                return new JansUserModel(model,person);
            }
            return null;
        }catch(JansThinBridgeOperationException e) {
            log.errorv(e,"Error fetching jans user with id " + id);
            return null;
        }
    }


}
