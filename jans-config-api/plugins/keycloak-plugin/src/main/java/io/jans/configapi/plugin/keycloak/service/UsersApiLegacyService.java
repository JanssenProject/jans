package io.jans.configapi.plugin.keycloak.service;

import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import java.io.IOException;
import javax.ws.rs.PathParam;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.apache.commons.lang.StringUtils;

@ApplicationScoped
public class UsersApiLegacyService {

    @Inject
    Logger logger;    

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    
    public UsersApiLegacyService(KeycloakSession session) {
        logger.error(" session:{}", session);
        this.session = session;
    }
    private static final Logger LOG = Logger.getLogger(UsersApiLegacyService.class);
    
  //  User getUserByUserName(String username) {
    TrustRelationship getUserByUserName(String username) {
        logger.error(" username:{}", username);
        try {
            return getAllTrustRelationshipByName(username);
        } catch (IOException e) {
            LOG.warn("Error fetching user " + username + " from external service: " + e.getMessage(), e);
        }
        return null;
    }
    
    TrustRelationship getUserByEmail(String email) {
        logger.error(" email:{}", email);
        try {
            return getAllTrustRelationshipByName(email);
        } catch (IOException e) {
            LOG.warn("Error fetching user " + email + " from external service: " + e.getMessage(), e);
        }
        return null;
    }

    public List<TrustRelationship> getAllTrustRelationshipByName(String name) {
        log.info("Search TrustRelationship with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        log.debug("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }

}
