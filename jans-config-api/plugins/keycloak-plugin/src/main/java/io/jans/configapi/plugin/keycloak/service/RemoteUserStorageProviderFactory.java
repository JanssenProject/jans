package io.jans.configapi.plugin.keycloak.service;

import io.jans.util.exception.InvalidConfigurationException;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.UserStorageProviderFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUserStorageProviderFactory implements UserStorageProviderFactory<RemoteUserStorageProvider> {
   
    private static Logger LOG = LoggerFactory.getLogger(RemoteUserStorageProviderFactory.class);
   

    public static final String PROVIDER_NAME = "jans-remote-user-storage-provider";
       
    @Override
    public RemoteUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        LOG.debug("session:{}, model:{}",session, model);
        return new RemoteUserStorageProvider(session, model, new UsersApiLegacyService(session));
    }
    
    @Override
    public String getId() {
        String id = PROVIDER_NAME;
        LOG.debug("id:{}",id);
        return id;
    }

}
