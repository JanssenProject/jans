package io.jans.configapi.plugin.keycloak.service;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.plugin.mgt.model.user.CustomUser;
import io.jans.orm.search.filter.Filter;

import java.io.IOException;
import javax.ws.rs.PathParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersApiLegacyService {

    private static Logger LOG = LoggerFactory.getLogger(UsersApiLegacyService.class);
    private static String AUTH_USER_ENDPOINT = "http://localhost:8080/jans-config-api/mgt/configuser/";

    private KeycloakSession session;
    
    
    public UsersApiLegacyService(KeycloakSession session) {
        LOG.error(" session:{}", session);
        this.session = session;
    }
    
    public CustomUser getUserById(String inum) {
        LOG.error(" inum:{}", inum);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + inum, this.session).asJson(CustomUser.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on inum:{} from external service is:{} - {} ", inum, ex.getMessage(), ex);
        }
        return null;
    }
        
    public CustomUser getUserByName(String username) {
        LOG.error(" username:{}", username);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + username, this.session).asJson(CustomUser.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on username:{} from external service is:{} - {} ", username, ex.getMessage(), ex);
        }
        return null;
    }
    
    public CustomUser getUserByEmail(String email) {
        LOG.error(" email:{}", email);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + email, this.session).asJson(CustomUser.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on email:{} from external service is:{} - {} ", email, ex.getMessage(), ex);
        }
        return null;
    }

}
