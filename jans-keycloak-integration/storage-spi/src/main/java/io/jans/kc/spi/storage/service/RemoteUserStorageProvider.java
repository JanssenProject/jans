package io.jans.kc.spi.storage.service;

import io.jans.kc.spi.storage.config.PluginConfiguration;
import io.jans.kc.spi.storage.util.JansUtil;
import io.jans.scim.model.scim2.user.UserResource;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import org.jboss.logging.Logger;


public class RemoteUserStorageProvider implements CredentialInputValidator, UserLookupProvider, UserStorageProvider {

    private static Logger log = Logger.getLogger(RemoteUserStorageProvider.class);

    private KeycloakSession session;
    private ComponentModel model;
    private UsersApiLegacyService usersService;
    private CredentialAuthenticatingService credentialAuthenticatingService;

    public RemoteUserStorageProvider(KeycloakSession session, ComponentModel model, PluginConfiguration pluginConfiguration) {
        log.debugv("RemoteUserStorageProvider() -  session:{0}, model:{1}", session, model);
        JansUtil jansUtil = new JansUtil(pluginConfiguration);
        this.session = session;
        this.model = model;
        this.usersService = new UsersApiLegacyService(session, model,new ScimService(jansUtil));
        this.credentialAuthenticatingService = new CredentialAuthenticatingService(jansUtil);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        log.debugv("RemoteUserStorageProvider::supportsCredentialType() - credentialType:{0}", credentialType);
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.debugv("RemoteUserStorageProvider::isConfiguredFor() - realm:{0}, user:{1}, credentialType:{2} ", realm, user,
                credentialType);
        return user.credentialManager().isConfiguredFor(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        log.debugv(
                "RemoteUserStorageProvider::isValid() - realm:{0}, user:{1}, credentialInput:{2}, user.getUsername():{2}, credentialInput.getChallengeResponse():{}",
                realm, user, credentialInput, user.getUsername(), credentialInput.getChallengeResponse());

        boolean valid = credentialAuthenticatingService.authenticateUser(user.getUsername(),
                credentialInput.getChallengeResponse());

        log.debugv("RemoteUserStorageProvider::isValid() - valid:{0}", valid);

        return valid;

    }

    /**
     * Get user based on id
     */
    public UserModel getUserById(RealmModel paramRealmModel, String id) {
        log.debugv("RemoteUserStorageProvider::getUserById() - paramRealmModel:{0}, id:{1}", paramRealmModel, id);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserById(StorageId.externalId(id));
            log.debugv("RemoteUserStorageProvider::getUserById() - user fetched based on  id:{0} is user:{1}", id, user);
            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                log.debugv(" RemoteUserStorageProvider::getUserById() - userModel:{0}", userModel);

                if (userModel != null) {
                    log.debugv(
                            "RemoteUserStorageProvider::getUserById() - Final userModel fetched with id:{0},  userModel:{1}, userModel.getAttributes(:{2})",
                            id, userModel, userModel.getAttributes());
                }
            }

            log.debugv(
                    "RemoteUserStorageProvider::getUserById() - User fetched with id:{0} from external service is:{1}",
                    id, user);

        } catch (Exception ex) {
            log.errorv(ex,
                    "RemoteUserStorageProvider::getUserById() - Error fetching user id:{0} from external service",
                    id);

        }

        return userModel;
    }

    /**
     * Get user based on name
     */
    public UserModel getUserByUsername(RealmModel paramRealmModel, String name) {
        log.debugv("RemoteUserStorageProvider::getUserByUsername() - paramRealmModel:{0}, name:{1}", paramRealmModel,
                name);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserByName(name);
            log.debugv(
                    "RemoteUserStorageProvider::getUserByUsername() - User fetched with name:{0} from external service is:{1}",
                    name, user);

            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                log.debugv("RemoteUserStorageProvider::getUserByUsername() - userModel:{0}", userModel);
            }
            if (userModel != null) {
                log.debugv(
                        "RemoteUserStorageProvider::getUserByUsername() - Final User fetched with name:{0},  userModel:{1}, userModel.getAttributes():{2}",
                        name, userModel, userModel.getAttributes());
            }

        } catch (Exception ex) {
            log.errorv(ex,
                    "\n RemoteUserStorageProvider::getUserByUsername() -  Error fetching user name:{0}",
                    name);

        }
        return userModel;
    }

    public UserModel getUserByEmail(RealmModel paramRealmModel, String email) {
        log.debugv("RemoteUserStorageProvider::getUserByEmail() - paramRealmModel:{0}, email:{1}", paramRealmModel,
                email);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserByEmail(email);
            log.debugv(
                    "RemoteUserStorageProvider::getUserByEmail() - User fetched with email:{0} from external service is:{1}",
                    email, user);

            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                log.debugv("RemoteUserStorageProvider::getUserByEmail() - userModel:{0}", userModel);
            }

            if (userModel != null) {
                log.debugv(
                        "RemoteUserStorageProvider::getUserByEmail() - Final User fetched with email:{0},  userModel:{1}, userModel.getAttributes(:{2})",
                        email, userModel, userModel.getAttributes());
            }

        } catch (Exception ex) {
            log.errorv(ex,
                    "RemoteUserStorageProvider::getUserByEmail() -  Error fetching user email:{0}",
                    email);

        }
        return userModel;
    }

    public void close() {
        log.debug("RemoteUserStorageProvider::close()");

    }

    private UserModel createUserModel(RealmModel realm, UserResource user) {
        log.debugv("RemoteUserStorageProvider::createUserModel() - realm:{0} , user:{1}", realm, user);

        UserModel userModel = new UserAdapter(session, realm, model, user);
        log.debugv("Final RemoteUserStorageProvider::createUserModel() - userModel:{0}", userModel);

        return userModel;
    }
}
