package io.jans.casa.plugins.injiwallet.vm;

import io.jans.casa.plugins.injiwallet.service.AgamaConfigService;

import java.util.HashMap;
import java.util.Map;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

/**
 * ViewModel for Inji Wallet admin configuration page
 * Handles all admin interactions and configuration management
 */
public class InjiWalletAdminVM {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private AgamaConfigService configService;
    
    // Configuration properties
    private String injiWebBaseUrl;
    private String injiVerifyBaseUrl;
    private String agamaCallbackUrl;
    private String clientId;
    private boolean enableRegistration;
    private String credentialMappings;  // Changed from vcToGluuMapping to credentialMappings
    private String presentationDefinition;
    
    // Status properties
    private String statusMessage;
    private boolean configurationValid;
    
    // Project information
    private Map<String, Object> projectInfo;
    
    // Parsed credential mappings for display
    private List<Map<String, Object>> credentialMappingsList;
    
    @Init
    public void init() {
        logger.info("Initializing Inji Wallet Admin ViewModel");
        configService = new AgamaConfigService();
        loadProjectInfo();
        loadCurrentConfiguration();
    }
    
    /**
     * Load project information from project.json
     */
    private void loadProjectInfo() {
        try {
            projectInfo = configService.getProjectInfo();
            logger.info("Loaded project info: {}", projectInfo);
        } catch (Exception e) {
            logger.error("Error loading project info", e);
            projectInfo = new HashMap<>();
        }
    }
    
    /**
     * Load current configuration from Agama project
     */
    private void loadCurrentConfiguration() {
        try {
            Map<String, Object> config = configService.getCurrentConfig();
            
            if (config == null || config.isEmpty()) {
                configurationValid = false;
                statusMessage = "Error: No configuration available from API";
                logger.error("Configuration is null or empty from API");
                return;
            }
            
            injiWebBaseUrl = (String) config.get("injiWebBaseURL");
            injiVerifyBaseUrl = (String) config.get("injiVerifyBaseURL");
            agamaCallbackUrl = (String) config.get("agamaCallBackUrl");
            clientId = (String) config.get("clientId");
            enableRegistration = (Boolean) config.getOrDefault("enableRegistration", true);
            
            // Convert complex objects to JSON strings for display
            // Handle both old format (vcToGluuMapping) and new format (credentialMappings)
            if (config.containsKey("credentialMappings")) {
                credentialMappingsList = (List<Map<String, Object>>) config.get("credentialMappings");
                credentialMappings = convertToJsonString(credentialMappingsList);
            } else if (config.containsKey("vcToGluuMapping")) {
                // Convert old format to new format for display
                Map<String, String> oldMapping = (Map<String, String>) config.get("vcToGluuMapping");
                credentialMappingsList = new java.util.ArrayList<>();
                Map<String, Object> defaultCredential = new HashMap<>();
                defaultCredential.put("credentialType", "NID");
                defaultCredential.put("vcToGluuMapping", oldMapping);
                credentialMappingsList.add(defaultCredential);
                credentialMappings = convertToJsonString(credentialMappingsList);
            } else {
                credentialMappingsList = new java.util.ArrayList<>();
                credentialMappings = "[]";
            }
            
            presentationDefinition = convertToJsonString(config.get("presentationDefinition"));
            
            configurationValid = true;
            statusMessage = "Configuration loaded successfully from API";
            logger.info("Configuration loaded successfully from Agama deployment API");
            
        } catch (Exception e) {
            logger.error("Error loading configuration from API", e);
            configurationValid = false;
            statusMessage = "Error loading configuration from API: " + e.getMessage();
            
            // Set empty values
            injiWebBaseUrl = "";
            injiVerifyBaseUrl = "";
            agamaCallbackUrl = "";
            clientId = "";
            credentialMappings = "[]";
            credentialMappingsList = new java.util.ArrayList<>();
            presentationDefinition = "{}";
        }
    }
    
    /**
     * Save configuration command
     */
    @Command
    @NotifyChange({"statusMessage", "configurationValid"})
    public void saveConfig() {
        try {
            logger.info("Saving Inji Wallet configuration");
            
            // Validate configuration
            if (!validateConfiguration()) {
                return;
            }
            
            // Build configuration map
            Map<String, Object> config = buildConfigurationMap();
            
            // Save to Agama project
            boolean success = configService.updateConfig(config);
            
            if (success) {
                statusMessage = "Configuration saved successfully!";
                configurationValid = true;
                
                // Show success message
                Messagebox.show("Configuration saved successfully!", "Success", 
                               Messagebox.OK, Messagebox.INFORMATION);
                
            } else {
                statusMessage = "Failed to save configuration";
                configurationValid = false;
                
                Messagebox.show("Failed to save configuration. Please check the logs.", "Error", 
                               Messagebox.OK, Messagebox.ERROR);
            }
            
        } catch (Exception e) {
            logger.error("Error saving configuration", e);
            statusMessage = "Error saving configuration: " + e.getMessage();
            configurationValid = false;
            
            Messagebox.show("Error saving configuration: " + e.getMessage(), "Error", 
                           Messagebox.OK, Messagebox.ERROR);
        }
    }
    
    /**
     * Reset configuration command
     */
    @Command
    @NotifyChange({"injiWebBaseUrl", "injiVerifyBaseUrl", "agamaCallbackUrl", "clientId", 
                   "enableRegistration", "credentialMappings", "presentationDefinition", "statusMessage"})
    public void resetConfig() {
        try {
            boolean success = configService.resetToDefaults();
            
            if (success) {
                loadCurrentConfiguration(); // Reload from defaults
                statusMessage = "Configuration reset to defaults";
                
                Messagebox.show("Configuration reset to defaults successfully!", "Success", 
                               Messagebox.OK, Messagebox.INFORMATION);
            } else {
                statusMessage = "Failed to reset configuration";
                
                Messagebox.show("Failed to reset configuration", "Error", 
                               Messagebox.OK, Messagebox.ERROR);
            }
            
        } catch (Exception e) {
            logger.error("Error resetting configuration", e);
            statusMessage = "Error resetting configuration: " + e.getMessage();
        }
    }
    
    /**
     * Test connection command
     */
    @Command
    @NotifyChange("statusMessage")
    public void testConnection() {
        try {
            Map<String, Object> result = configService.testConnection();
            boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            
            statusMessage = message;
            
            if (success) {
                Messagebox.show("Connection test successful!\n" + message, "Success", 
                               Messagebox.OK, Messagebox.INFORMATION);
            } else {
                Messagebox.show("Connection test failed!\n" + message, "Error", 
                               Messagebox.OK, Messagebox.ERROR);
            }
            
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            statusMessage = "Connection test failed: " + e.getMessage();
        }
    }
    
    /**
     * Validate configuration before saving
     */
    private boolean validateConfiguration() {
        if (injiWebBaseUrl == null || injiWebBaseUrl.trim().isEmpty()) {
            Messagebox.show("Inji Web Base URL is required", "Validation Error", 
                           Messagebox.OK, Messagebox.ERROR);
            return false;
        }
        
        if (injiVerifyBaseUrl == null || injiVerifyBaseUrl.trim().isEmpty()) {
            Messagebox.show("Inji Verify Base URL is required", "Validation Error", 
                           Messagebox.OK, Messagebox.ERROR);
            return false;
        }
        
        if (clientId == null || clientId.trim().isEmpty()) {
            Messagebox.show("Client ID is required", "Validation Error", 
                           Messagebox.OK, Messagebox.ERROR);
            return false;
        }
        
        // Validate JSON strings
        if (!isValidJson(credentialMappings)) {
            Messagebox.show("Credential Mappings must be valid JSON", "Validation Error", 
                           Messagebox.OK, Messagebox.ERROR);
            return false;
        }
        
        if (!isValidJson(presentationDefinition)) {
            Messagebox.show("Presentation Definition must be valid JSON", "Validation Error", 
                           Messagebox.OK, Messagebox.ERROR);
            return false;
        }
        
        return true;
    }
    
    /**
     * Build configuration map from form values
     */
    private Map<String, Object> buildConfigurationMap() {
        Map<String, Object> config = configService.getCurrentConfig();
        
        config.put("injiWebBaseURL", injiWebBaseUrl);
        config.put("injiVerifyBaseURL", injiVerifyBaseUrl);
        config.put("agamaCallBackUrl", agamaCallbackUrl);
        config.put("clientId", clientId);
        config.put("enableRegistration", enableRegistration);
        
        // Parse JSON strings back to objects
        try {
            config.put("credentialMappings", parseJsonString(credentialMappings));
            config.put("presentationDefinition", parseJsonString(presentationDefinition));
        } catch (Exception e) {
            logger.error("Error parsing JSON configuration", e);
        }
        
        return config;
    }
    
    /**
     * Helper methods
     */
    private String convertToJsonString(Object obj) {
        try {
            if (obj == null) return "{}";
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Error converting to JSON string", e);
            return "{}";
        }
    }
    
    private Object parseJsonString(String jsonString) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(jsonString, Object.class);
        } catch (Exception e) {
            logger.error("Error parsing JSON string", e);
            return new java.util.HashMap<>();
        }
    }
    
    private boolean isValidJson(String jsonString) {
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Getters and Setters
    public String getInjiWebBaseUrl() { return injiWebBaseUrl; }
    public void setInjiWebBaseUrl(String injiWebBaseUrl) { this.injiWebBaseUrl = injiWebBaseUrl; }
    
    public String getInjiVerifyBaseUrl() { return injiVerifyBaseUrl; }
    public void setInjiVerifyBaseUrl(String injiVerifyBaseUrl) { this.injiVerifyBaseUrl = injiVerifyBaseUrl; }
    
    public String getAgamaCallbackUrl() { return agamaCallbackUrl; }
    public void setAgamaCallbackUrl(String agamaCallbackUrl) { this.agamaCallbackUrl = agamaCallbackUrl; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public boolean isEnableRegistration() { return enableRegistration; }
    public void setEnableRegistration(boolean enableRegistration) { this.enableRegistration = enableRegistration; }
    
    public String getCredentialMappings() { return credentialMappings; }
    public void setCredentialMappings(String credentialMappings) { this.credentialMappings = credentialMappings; }
    
    public String getPresentationDefinition() { return presentationDefinition; }
    public void setPresentationDefinition(String presentationDefinition) { this.presentationDefinition = presentationDefinition; }
    
    public String getStatusMessage() { return statusMessage; }
    public boolean isConfigurationValid() { return configurationValid; }
    public Map<String, Object> getProjectInfo() { return projectInfo; }
    
    public List<Map<String, Object>> getCredentialMappingsList() { return credentialMappingsList; }
}