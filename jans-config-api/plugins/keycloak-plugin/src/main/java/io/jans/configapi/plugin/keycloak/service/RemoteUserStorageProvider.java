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
import org.keycloak.storage.user.UserLookupProvider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class RemoteUserStorageProvider implements UserLookupProvider, UserStorageProvider {

    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceManager;

    private KeycloakSession session;
    private ComponentModel model;
    private UsersApiLegacyService usersService;


    public RemoteUserStorageProvider(KeycloakSession session, ComponentModel model,
            UsersApiLegacyService usersService) {
        logger.error(" session:{}, model:{}, usersService:{}",session, model, usersService);
    
        this.session = session;
        this.model = model;
        this.usersService = usersService;
    }
    
    public UserModel getUserById(RealmModel paramRealmModel, String paramString) {return null;}
    public UserModel getUserByUsername(RealmModel paramRealmModel, String paramString) {return null;}
    public UserModel getUserByEmail(RealmModel paramRealmModel, String paramString) {return null;}
    public void close(){}
}
