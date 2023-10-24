package io.jans.kc.spi.storage.service;

import io.jans.scim.model.scim2.user.UserResource;

import java.util.Properties;

import org.jboss.logging.Logger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class UsersApiLegacyService {

    private static Logger log = Logger.getLogger(UsersApiLegacyService.class);

    private ScimService scimService;
    protected Properties jansProperties = new Properties();

    public UsersApiLegacyService(KeycloakSession session, ComponentModel model, ScimService scimService) {

        log.debugv(" UsersApiLegacyService() - session:{0}, model:{1}", session, model);
        this.scimService = scimService;
    }

    public UserResource getUserById(String inum) {
        log.debugv("UsersApiLegacyService::getUserById() - inum:{0}", inum);
        try {
            return scimService.getUserById(inum);
        } catch (Exception ex) {
            log.errorv(ex,
                    "UsersApiLegacyService::getUserById() - Error fetching user based on inum:{0} from external service",
                    inum);

        }
        return null;
    }

    public UserResource getUserByName(String username) {
        log.debugv(" UsersApiLegacyService::getUserByName() - username:{0}", username);
        try {

            return scimService.getUserByName(username);
        } catch (Exception ex) {
            log.errorv(ex,
                    "UsersApiLegacyService::getUserByName() - Error fetching user based on username:{0} from external service",
                    username);

        }
        return null;
    }

    public UserResource getUserByEmail(String email) {
        log.infov(" UsersApiLegacyService::getUserByEmail() - email:{0}", email);
        try {

            return scimService.getUserByEmail(email);
        } catch (Exception ex) {
            log.errorv(
                    " UsersApiLegacyService::getUserByEmail() - Error fetching user based on email:{0}",
                    email);

        }
        return null;
    }

}
