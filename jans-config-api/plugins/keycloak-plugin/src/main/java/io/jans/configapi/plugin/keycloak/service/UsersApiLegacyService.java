package io.jans.configapi.plugin.keycloak.service;

import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.service.SamlService;
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
import org.slf4j.Logger;

@ApplicationScoped
public class UsersApiLegacyService {

    @Inject
    Logger logger;    

    @Inject
    PersistenceEntryManager persistenceEntryManager;
    
    @Inject
    SamlService samlService;

    private KeycloakSession session;
    public UsersApiLegacyService(KeycloakSession session) {
        logger.error(" session:{}", session);
        this.session = session;
    }
        
  //  User getUserByUserName(String username) {
    List<TrustRelationship> getUserByUserName(String username) {
        logger.error(" username:{}", username);
        try {
            return samlService.getAllTrustRelationshipByName(username);
        } catch (Exception ex) {
            logger.error("Error fetching user " + username + " from external service: " + ex.getMessage(), ex);
        }
        return null;
    }
    
    List<TrustRelationship> getUserByEmail(String email) {
        logger.error(" email:{}", email);
        try {
            return getAllTrustRelationshipByName(email);
        } catch (Exception ex) {
            logger.error("Error fetching user " + email + " from external service: " + ex.getMessage(), ex);
        }
        return null;
    }

    public List<TrustRelationship> getAllTrustRelationshipByName(String name) {
        logger.error("Search TrustRelationship with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        logger.error("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(samlService.getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }

}
