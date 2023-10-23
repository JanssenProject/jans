package io.jans.idp.keycloak.service;

import io.jans.scim.model.scim2.user.UserResource;

import java.util.Properties;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersApiLegacyService {

    private static Logger logger = LoggerFactory.getLogger(UsersApiLegacyService.class);
    private ScimService scimService = new ScimService();

    private KeycloakSession session;
    private ComponentModel model;
    protected Properties jansProperties = new Properties();

    public UsersApiLegacyService(KeycloakSession session, ComponentModel model) {
        logger.info(" UsersApiLegacyService() - session:{}, model:{}", session, model);

        this.session = session;
        this.model = model;
    }

    public UserResource getUserById(String inum) {
        logger.info("UsersApiLegacyService::getUserById() - inum:{}", inum);
        try {
            return scimService.getUserById(inum);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    "UsersApiLegacyService::getUserById() - Error fetching user based on inum:{} from external service is:{} - {} ",
                    inum, ex.getMessage(), ex);

        }
        return null;
    }

    public UserResource getUserByName(String username) {
        logger.info(" UsersApiLegacyService::getUserByName() - username:{}", username);
        try {

            return scimService.getUserByName(username);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    "UsersApiLegacyService::getUserByName() - Error fetching user based on username:{} from external service is:{} - {} ",
                    username, ex.getMessage(), ex);

        }
        return null;
    }

    public UserResource getUserByEmail(String email) {
        logger.info(" UsersApiLegacyService::getUserByEmail() - email:{}", email);
        try {

            return scimService.getUserByEmail(email);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(
                    " UsersApiLegacyService::getUserByEmail() - Error fetching user based on email:{} from external service is:{} - {} ",
                    email, ex.getMessage(), ex);

        }
        return null;
    }

}
