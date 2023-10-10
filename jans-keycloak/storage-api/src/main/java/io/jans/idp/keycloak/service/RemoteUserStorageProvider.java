package io.jans.idp.keycloak.service;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUserStorageProvider implements CredentialInputValidator, UserLookupProvider, UserStorageProvider {

    private static Logger logger = LoggerFactory.getLogger(RemoteUserStorageProvider.class);

    private KeycloakSession session;
    private ComponentModel model;
    private UsersApiLegacyService usersService;
    private CredentialAuthenticatingService credentialAuthenticatingService = new CredentialAuthenticatingService();

    public RemoteUserStorageProvider(KeycloakSession session, ComponentModel model) {
        logger.info("RemoteUserStorageProvider() -  session:{}, model:{}", session, model);

        this.session = session;
        this.model = model;
        this.usersService = new UsersApiLegacyService(session, model);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        logger.info("RemoteUserStorageProvider::supportsCredentialType() - credentialType:{}", credentialType);
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.info("RemoteUserStorageProvider::isConfiguredFor() - realm:{}, user:{}, credentialType:{} ", realm, user,
                credentialType);
        return user.credentialManager().isConfiguredFor(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        logger.info(
                "RemoteUserStorageProvider::isValid() - realm:{}, user:{}, credentialInput:{}, user.getUsername():{}, credentialInput.getChallengeResponse():{}",
                realm, user, credentialInput, user.getUsername(), credentialInput.getChallengeResponse());

        boolean valid = credentialAuthenticatingService.authenticateUser(user.getUsername(),
                credentialInput.getChallengeResponse());

        logger.info("RemoteUserStorageProvider::isValid() - valid:{}", valid);

        return valid;

    }

    /**
     * Get user based on id
     */
    public UserModel getUserById(RealmModel paramRealmModel, String id) {
        logger.info("RemoteUserStorageProvider::getUserById() - paramRealmModel:{}, id:{}", paramRealmModel, id);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserById(StorageId.externalId(id));
            logger.info("RemoteUserStorageProvider::getUserById() - user fetched based on  id:{} is user:{}", id, user);
            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                logger.info(" RemoteUserStorageProvider::getUserById() - userModel:{}", userModel);

                if (userModel != null) {
                    logger.info(
                            "RemoteUserStorageProvider::getUserById() - Final userModel fetched with id:{},  userModel:{}, userModel.getAttributes(:{})",
                            id, userModel, userModel.getAttributes());
                }
            }

            logger.info(
                    "RemoteUserStorageProvider::getUserById() - User fetched with id:{} from external service is:{}",
                    id, user);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    "RemoteUserStorageProvider::getUserById() - Error fetching user id:{} from external service is:{} - {} ",
                    id, ex.getMessage(), ex);

        }

        return userModel;
    }

    /**
     * Get user based on name
     */
    public UserModel getUserByUsername(RealmModel paramRealmModel, String name) {
        logger.info("RemoteUserStorageProvider::getUserByUsername() - paramRealmModel:{}, name:{}", paramRealmModel,
                name);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserByName(name);
            logger.info(
                    "RemoteUserStorageProvider::getUserByUsername() - User fetched with name:{} from external service is:{}",
                    name, user);

            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                logger.info("RemoteUserStorageProvider::getUserByUsername() - userModel:{}", userModel);
            }
            if (userModel != null) {
                logger.info(
                        "RemoteUserStorageProvider::getUserByUsername() - Final User fetched with name:{},  userModel:{}, userModel.getAttributes():{}",
                        name, userModel, userModel.getAttributes());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    "\n RemoteUserStorageProvider::getUserByUsername() -  Error fetching user name:{}, from external service is:{} - {} ",
                    name, ex.getMessage(), ex);

        }
        return userModel;
    }

    public UserModel getUserByEmail(RealmModel paramRealmModel, String email) {
        logger.info("RemoteUserStorageProvider::getUserByEmail() - paramRealmModel:{}, email:{}", paramRealmModel,
                email);

        UserModel userModel = null;
        try {
            UserResource user = usersService.getUserByEmail(email);
            logger.info(
                    "RemoteUserStorageProvider::getUserByEmail() - User fetched with email:{} from external service is:{}",
                    email, user);

            if (user != null) {
                userModel = createUserModel(paramRealmModel, user);
                logger.info("RemoteUserStorageProvider::getUserByEmail() - userModel:{}", userModel);
            }

            if (userModel != null) {
                logger.info(
                        "RemoteUserStorageProvider::getUserByEmail() - Final User fetched with email:{},  userModel:{}, userModel.getAttributes(:{})",
                        email, userModel, userModel.getAttributes());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    "\n RemoteUserStorageProvider::getUserByEmail() -  Error fetching user email:{}, from external service is:{} - {} ",
                    email, ex.getMessage(), ex);

        }
        return userModel;
    }

    public void close() {
        logger.info("RemoteUserStorageProvider::close()");

    }

    private UserModel createUserModel(RealmModel realm, UserResource user) {
        logger.info("RemoteUserStorageProvider::createUserModel() - realm:{} , user:{}", realm, user);

        UserModel userModel = new UserAdapter(session, realm, model, user);
        logger.info("Final RemoteUserStorageProvider::createUserModel() - userModel:{}", userModel);

        return userModel;
    }
}
