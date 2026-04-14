package io.jans.configapi.plugin.shibboleth.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShibbolethPluginConfiguration implements Configuration {

    private String applicationName;
    private boolean enabled;

    private String trustRelationshipDn;

    private String spMetadataDir;
    private String spMetadataFile;
    private String spMetadataFilePattern;
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getTrustRelationshipDn() {
        return trustRelationshipDn;
    }
    
    public void setTrustRelationshipDn(String trustRelationshipDn) {
        this.trustRelationshipDn = trustRelationshipDn;
    }
    
    public String getSpMetadataDir() {
        return spMetadataDir;
    }
    
    public void setSpMetadataDir(String spMetadataDir) {
        this.spMetadataDir = spMetadataDir;
    }
    
    public String getSpMetadataFile() {
        return spMetadataFile;
    }
    
    public void setSpMetadataFile(String spMetadataFile) {
        this.spMetadataFile = spMetadataFile;
    }
        
    public String getSpMetadataFilePattern() {
        return spMetadataFilePattern;
    }

    public void setSpMetadataFilePattern(String spMetadataFilePattern) {
        this.spMetadataFilePattern = spMetadataFilePattern;
    }

    @Override
    public String toString() {
        return "ShibbolethPluginConfiguration [applicationName=" + applicationName + ", enabled=" + enabled
                + ", trustRelationshipDn=" + trustRelationshipDn + ", spMetadataDir=" + spMetadataDir
                + ", spMetadataFile=" + spMetadataFile + ", spMetadataFilePattern=" + spMetadataFilePattern + "]";
    }
    
}
