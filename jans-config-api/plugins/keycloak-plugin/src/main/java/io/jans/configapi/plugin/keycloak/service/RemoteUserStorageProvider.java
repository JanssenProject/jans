package io.jans.configapi.plugin.keycloak.service;

import io.jans.configapi.plugin.mgt.model.user.CustomUser;

import java.io.IOException;
import java.util.List;

import org.keycloak.broker.provider.util.SimpleHttp;
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
import org.slf4j.LoggerFactory;

public class RemoteUserStorageProvider implements UserLookupProvider, UserStorageProvider {

    private static Logger LOG = LoggerFactory.getLogger(RemoteUserStorageProvider.class);
    private static String AUTH_USER_ENDPOINT = "http://localhost:8080/jans-config-api/mgt/configuser/";

    private KeycloakSession session;
    private ComponentModel model;
    private UsersApiLegacyService usersService;

    public RemoteUserStorageProvider(KeycloakSession session, ComponentModel model,
            UsersApiLegacyService usersService) {
        LOG.error(" session:{}, model:{}, usersService:{}", session, model, usersService);

        this.session = session;
        this.model = model;
        this.usersService = usersService;
    }

    /**
     * Get user based on id
     */
    public UserModel getUserById(RealmModel paramRealmModel, String id) {
        LOG.error("getUserById() paramRealmModel:{}, id:{}", paramRealmModel, id);
        UserModel userModel = null;
        try {
            CustomUser user = usersService.getUserById(id);
            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                System.out.println("New UserModel:");
                System.out.println(userModel.toString());
                LOG.error("userModel:{}", userModel);
            }
         
            LOG.error("User fetched with id:{} from external service is:{}", id, user);

        } catch (Exception ex) {
            LOG.error("Error fetching user id:{} from external service is:{} - {} ", id, ex.getMessage(), ex);
        }
        return userModel;
    }

    /**
     * Get user based on name
     */
    public UserModel getUserByUsername(RealmModel paramRealmModel, String name) {
        LOG.error("getUserByUsername() paramRealmModel:{}, name:{}", paramRealmModel, name);
        UserModel userModel = null;
        try {
            CustomUser user = usersService.getUserByName(name);
            LOG.error("User fetched with name:{} from external service is:{}", name, user);
        } catch (Exception ex) {
            LOG.error("Error fetching user name:{}, from external service is:{} -{} ", name, ex.getMessage(), ex);
        }
        return userModel;
    }

    public UserModel getUserByEmail(RealmModel paramRealmModel, String paramString) {
        return null;
    }

    public void close() {
    }

    private UserModel createUserModel(RealmModel realm, CustomUser user) {
        return new UserAdapter(session, realm, model, user);
    }
}
