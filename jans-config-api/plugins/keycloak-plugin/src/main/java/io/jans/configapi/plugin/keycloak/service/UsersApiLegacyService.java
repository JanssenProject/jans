package io.jans.configapi.plugin.keycloak.service;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import io.jans.as.common.model.common.User;
import io.jans.as.common.util.AttributeConstants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;

import java.io.IOException;
import javax.ws.rs.PathParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersApiLegacyService {

    private static Logger LOG = LoggerFactory.getLogger(UsersApiLegacyService.class);
    private static String AUTH_USER_ENDPOINT = "http://localhost:8080/jans-config-api/mgt/configuser/";

    PersistenceEntryManager persistenceEntryManager;
    private KeycloakSession session;
    
    
    public UsersApiLegacyService(KeycloakSession session) {
        LOG.error(" session:{}", session);
        this.session = session;
    }
    
    public User getUserById(String inum) {
        LOG.error(" inum:{}", inum);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + inum, this.session).asJson(User.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on inum:{} from external service is:{} - {} ", inum, ex.getMessage(), ex);
        }
        return null;
    }
        
    public User getUserByName(String username) {
        LOG.error(" username:{}", username);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + username, this.session).asJson(User.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on username:{} from external service is:{} - {} ", username, ex.getMessage(), ex);
        }
        return null;
    }
    
    public User getUserByEmail(String email) {
        LOG.error(" email:{}", email);
        try {
            return SimpleHttp.doGet(AUTH_USER_ENDPOINT + email, this.session).asJson(User.class);
        } catch (Exception ex) {
            LOG.error("Error fetching user based on email:{} from external service is:{} - {} ", email, ex.getMessage(), ex);
        }
        return null;
    }

}
