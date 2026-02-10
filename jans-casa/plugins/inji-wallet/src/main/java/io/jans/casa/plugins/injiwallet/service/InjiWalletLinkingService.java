package io.jans.casa.plugins.injiwallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.injiwallet.model.PersonWithCredentials;
import io.jans.casa.service.IPersistenceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing Inji Wallet credentials
 * Handles credential status checking and removal
 */
public class InjiWalletLinkingService {
    
    private Logger logger = LoggerFactory.getLogger(getClass());

    private static InjiWalletLinkingService instance;
    private IPersistenceService ips;
    private ObjectMapper mapper;
    
    public static InjiWalletLinkingService getInstance(String pluginId) {
        if (instance == null && pluginId != null) {
            instance = new InjiWalletLinkingService();
        }
        return instance;
    }
    
    public static InjiWalletLinkingService getInstance() {
        return instance;
    }
    
    /**
     * Get verifiable credentials from user attribute
     * Returns a map of credential types (NID, TAX, etc.) to their credential data
     */
    public Map<String, Object> getVerifiableCredentials(String userId) {
        try {
            PersonWithCredentials person = ips.get(PersonWithCredentials.class, ips.getPersonDn(userId));
            String verifiableCredentialsJson = person.getVerifiableCredentials();
            
            if (verifiableCredentialsJson != null && !verifiableCredentialsJson.isEmpty()) {
                logger.debug("Found verifiableCredentials for user {}", userId);
                
                Map<String, Object> credentials = mapper.readValue(
                    verifiableCredentialsJson, 
                    new TypeReference<Map<String, Object>>(){}
                );
                
                return credentials;
            }
        } catch (Exception e) {
            logger.error("Error getting verifiable credentials for user: {}", userId, e);
        }
        
        return Collections.emptyMap();
    }
    
    /**
     * Check if user has a specific credential type (NID, TAX, etc.)
     */
    public boolean hasCredentialType(String userId, String credentialType) {
        try {
            Map<String, Object> credentials = getVerifiableCredentials(userId);
            return credentials.containsKey(credentialType);
        } catch (Exception e) {
            logger.error("Error checking credential type {} for user: {}", credentialType, userId, e);
            return false;
        }
    }
    
    /**
     * Remove a specific credential type from user's verifiableCredentials
     */
    public boolean removeCredentialType(String userId, String credentialType) {
        try {
            PersonWithCredentials person = ips.get(PersonWithCredentials.class, ips.getPersonDn(userId));
            String verifiableCredentialsJson = person.getVerifiableCredentials();
            
            if (verifiableCredentialsJson == null || verifiableCredentialsJson.isEmpty()) {
                logger.warn("No verifiableCredentials found for user: {}", userId);
                return false;
            }
            
            Map<String, Object> credentials = mapper.readValue(
                verifiableCredentialsJson, 
                new TypeReference<Map<String, Object>>(){}
            );
            
            if (credentials.containsKey(credentialType)) {
                credentials.remove(credentialType);
                logger.info("Removed {} credential for user: {}", credentialType, userId);
                
                String updatedJson = credentials.isEmpty() ? null : mapper.writeValueAsString(credentials);
                person.setVerifiableCredentials(updatedJson);
                ips.modify(person);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error removing credential type {} for user: {}", credentialType, userId, e);
            return false;
        }
    }
    
    private InjiWalletLinkingService() {
        logger.info("Initializing InjiWalletLinkingService");
        ips = Utils.managedBean(IPersistenceService.class);
        mapper = new ObjectMapper();
    }
}
