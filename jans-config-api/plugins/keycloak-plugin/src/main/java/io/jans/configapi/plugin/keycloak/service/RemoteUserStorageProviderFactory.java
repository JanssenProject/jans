package io.jans.configapi.plugin.keycloak.service;

import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;


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
import org.keycloak.storage.user.UserStorageProviderFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class RemoteUserStorageProviderFactory implements UserStorageProviderFactory<RemoteUserStorageProvider> {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;
    

    public static final String PROVIDER_NAME = "jans-remote-user-storage-provider";
       
    @Override
    public RemoteUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        logger.error("session:{}, model:{}",session, model);
        return new RemoteUserStorageProvider(session, model, new UsersApiLegacyService(session));
    }
    
    @Override
    public String getId() {
        String id = PROVIDER_NAME;
        logger.error("id:{}",id);
        return id;
    }

}
