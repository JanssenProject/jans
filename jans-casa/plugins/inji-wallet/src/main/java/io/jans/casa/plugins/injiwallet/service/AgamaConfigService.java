package io.jans.casa.plugins.injiwallet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.casa.service.IPersistenceService;
import io.jans.casa.misc.Utils;
import io.jans.casa.conf.OIDCClientSettings;
import io.jans.casa.model.ApplicationConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Service for managing Agama project configuration
 * Uses API endpoints like acct-linking for realistic deployment
 */
public class AgamaConfigService {

    private static final Logger logger = LoggerFactory.getLogger(AgamaConfigService.class);
    private static final String PROJECT_NAME = "agama-inji-wallet";
    private static final String FLOW_CONFIG_KEY = "com.gluu.agama.inji.verifyCredential";
    private static final String READ_SCOPE = "https://jans.io/oauth/config/agama.readonly";
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private IPersistenceService ips;
    private String basicAuthnHeader;
    
    public AgamaConfigService() {
        ips = Utils.managedBean(IPersistenceService.class);
        
        // Set up authentication like acct-linking does
        OIDCClientSettings clSettings = ips.get(ApplicationConfiguration.class, "ou=casa,ou=configuration,o=jans")
                .getSettings().getOidcSettings().getClient();
        
        String authz = clSettings.getClientId() + ":" + clSettings.getClientSecret();
        authz = new String(Base64.getEncoder().encode(authz.getBytes(UTF_8)), UTF_8);
        basicAuthnHeader = "Basic " + authz;
    }
    
    /**
     * Get current configuration from the Agama deployment API
     * Following the same pattern as acct-linking
     */
    public Map<String, Object> getCurrentConfig() {
        try {
            // Always get from API - no fallback to defaults
            Map<String, Object> config = readConfigFromAPI();
            if (config != null && !config.isEmpty()) {
                return config;
            }
            
            logger.error("Could not read configuration from API - no config available");
            throw new RuntimeException("Failed to read configuration from Agama deployment API");
            
        } catch (Exception e) {
            logger.error("Error retrieving Agama configuration from API", e);
            throw new RuntimeException("Failed to retrieve configuration: " + e.getMessage(), e);
        }
    }
    
    /**
     * Read configuration from the Agama deployment API
     */
    private Map<String, Object> readConfigFromAPI() {
        try {
            logger.info("Reading configuration from Agama deployment API");
            
            String issuer = ips.getIssuerUrl();
            String endpoint = issuer + "/jans-config-api/api/v1/agama-deployment/configs/" + PROJECT_NAME;
            
            HTTPRequest request = new HTTPRequest(HTTPRequest.Method.GET, new URL(endpoint));
            setTimeouts(request);
            request.setAuthorization("Bearer " + getAToken());

            HTTPResponse r = request.send();
            r.ensureStatusCode(200);
            
            // Parse the response
            Map<String, Map<String, Object>> response = objectMapper.readValue(
                    r.getBody(), new TypeReference<Map<String, Map<String, Object>>>(){});
                    
            Map<String, Object> flowConfig = Optional.ofNullable(response)
                    .map(m -> m.get(FLOW_CONFIG_KEY)).orElse(null);
            
            if (flowConfig != null) {
                logger.info("Successfully loaded configuration from API with keys: {}", flowConfig.keySet());
                return flowConfig;
            } else {
                logger.warn("Flow config '{}' not found in API response", FLOW_CONFIG_KEY);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error reading configuration from API", e);
            return null;
        }
    }
    
    /**
     * Get access token for API calls
     */
    private String getAToken() throws IOException {
        StringJoiner joiner = new StringJoiner("&");
        Map.of("grant_type", "client_credentials", "scope", URLEncoder.encode(READ_SCOPE, UTF_8))
                .forEach((k, v) -> joiner.add(k + "=" + v));

        logger.info("Calling token endpoint");

        HTTPRequest request = new HTTPRequest(
                HTTPRequest.Method.POST, new URL(ips.getIssuerUrl() + "/jans-auth/restv1/token"));
        setTimeouts(request);
        request.setQuery(joiner.toString());
        request.setAuthorization(basicAuthnHeader);

        try {
            Map<String, Object> jobj = request.send().getContentAsJSONObject();
            logger.info("Successful token call");
            return jobj.get("access_token").toString();
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void setTimeouts(HTTPRequest request) {
        request.setConnectTimeout(3500);
        request.setReadTimeout(3500);
    }
    
    /**
     * Get default configuration based on your project.json structure
     * NOTE: This is kept for reference only - NOT used by getCurrentConfig()
     * The system always fetches configuration from the Agama deployment API
     */
    @Deprecated
    private Map<String, Object> getDefaultConfig() {
        logger.warn("getDefaultConfig() called - this should not be used in production");
        Map<String, Object> config = new HashMap<>();
        
        // Basic configuration from your project.json
        config.put("injiWebBaseURL", "https://injiweb.collab.mosip.net");
        config.put("injiVerifyBaseURL", "https://injiverify.collab.mosip.net");
        config.put("agamaCallBackUrl", "https://mosip-demo.gluu.info/jans-auth/fl/callback");
        config.put("clientId", "agama-app");
        
        // NEW FORMAT: credentialMappings array with multiple credential types
        java.util.List<Map<String, Object>> credentialMappings = new java.util.ArrayList<>();
        
        // NID credential mapping
        Map<String, Object> nidMapping = new HashMap<>();
        nidMapping.put("credentialType", "NID");
        Map<String, String> vcMapping = new HashMap<>();
        vcMapping.put("fullName", "displayName");
        vcMapping.put("phone", "mobile");
        vcMapping.put("gender", "gender");
        vcMapping.put("email", "mail");
        vcMapping.put("dateOfBirth", "birthdate");
        nidMapping.put("vcToGluuMapping", vcMapping);
        
        credentialMappings.add(nidMapping);
        config.put("credentialMappings", credentialMappings);
        
        // Presentation definition from your project.json
        Map<String, Object> presentationDef = new HashMap<>();
        presentationDef.put("id", "c4822b58-7fb4-454e-b827-f8758fe27f9a");
        presentationDef.put("purpose", "Relying party is requesting your digital ID for the purpose of Self-Authentication");
        
        Map<String, Object> format = new HashMap<>();
        Map<String, Object> ldpVc = new HashMap<>();
        ldpVc.put("proof_type", new String[]{"Ed25519Signature2020"});
        format.put("ldp_vc", ldpVc);
        presentationDef.put("format", format);
        
        config.put("presentationDefinition", presentationDef);
        
        // Client metadata from your project.json
        Map<String, Object> clientMetadata = new HashMap<>();
        clientMetadata.put("client_name", "Agama Application");
        clientMetadata.put("logo_uri", "https://mosip.github.io/inji-config/logos/StayProtectedInsurance.png");
        config.put("clientMetadata", clientMetadata);
        
        return config;
    }
    
    /**
     * Get specific configuration value
     */
    public Object getConfigValue(String key) {
        Map<String, Object> config = getCurrentConfig();
        return config.get(key);
    }
    
    /**
     * Update specific configuration value
     */
    public boolean updateConfigValue(String key, Object value) {
        try {
            Map<String, Object> config = getCurrentConfig();
            config.put(key, value);
            // TODO: Implement API-based config update
            logger.warn("Configuration update not implemented for API-based approach");
            return false;
        } catch (Exception e) {
            logger.error("Error updating config value: {} = {}", key, value, e);
            return false;
        }
    }
    
    /**
     * Update configuration (for admin interface)
     */
    public boolean updateConfig(Map<String, Object> newConfig) {
        try {
            // TODO: Implement API-based configuration update
            // For now, just log the attempt
            logger.info("Configuration update requested with keys: {}", newConfig.keySet());
            logger.warn("Configuration update not yet implemented for API-based approach");
            
            // In a real implementation, this would call an API endpoint to update the deployed configuration
            // For now, return false to indicate it's not implemented
            return false;
            
        } catch (Exception e) {
            logger.error("Error updating Agama configuration", e);
            return false;
        }
    }
    
    /**
     * Reset configuration to defaults
     */
    public boolean resetToDefaults() {
        logger.error("Reset to defaults is not supported - configuration must be managed via Agama deployment API");
        throw new UnsupportedOperationException("Configuration reset not supported. Please update configuration through the Agama deployment API or TUI.");
    }
    
    /**
     * Test connection to Inji services
     */
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> config = getCurrentConfig();
            String injiWebUrl = (String) config.get("injiWebBaseURL");
            String injiVerifyUrl = (String) config.get("injiVerifyBaseURL");
            
            logger.info("Testing connection to Inji Web: {}", injiWebUrl);
            logger.info("Testing connection to Inji Verify: {}", injiVerifyUrl);
            
            // TODO: Implement actual HTTP connection testing
            // For now, just validate URLs
            boolean webUrlValid = injiWebUrl != null && injiWebUrl.startsWith("https://");
            boolean verifyUrlValid = injiVerifyUrl != null && injiVerifyUrl.startsWith("https://");
            
            if (webUrlValid && verifyUrlValid) {
                result.put("success", true);
                result.put("injiWebStatus", "URL Valid");
                result.put("injiVerifyStatus", "URL Valid");
                result.put("message", "Configuration URLs are valid (actual connection test not implemented)");
            } else {
                result.put("success", false);
                result.put("message", "Invalid URLs in configuration");
            }
            
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            result.put("success", false);
            result.put("message", "Connection test failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get project information
     */
    public Map<String, Object> getProjectInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("projectName", PROJECT_NAME);
        info.put("version", "0.0.1");
        info.put("author", "mmrraju");
        info.put("description", "Agama Project to validate the MOSIP Inji web wallet");
        return info;
    }
}