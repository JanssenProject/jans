package io.jans.configapi.plugin.shibboleth.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShibbolethPluginConfiguration implements Configuration {

    private String applicationName;
    private boolean enabled;
    private String trustRelationshipDn;

    private String shibbolethMetadataDir;
    private String shibbolethMetadataFile;
    private String shibbolethMetadataFilePattern;
    
    private String stagedFilesBase;
    private int tokenDuration = 10; //default
    
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
    
    public String getShibbolethMetadataDir() {
        return shibbolethMetadataDir;
    }
    
    public void setShibbolethMetadataDir(String shibbolethMetadataDir) {
        this.shibbolethMetadataDir = shibbolethMetadataDir;
    }
    
    public String getShibbolethMetadataFile() {
        return shibbolethMetadataFile;
    }
    
    public void setShibbolethMetadataFile(String shibbolethMetadataFile) {
        this.shibbolethMetadataFile = shibbolethMetadataFile;
    }
    
    public String getShibbolethMetadataFilePattern() {
        return shibbolethMetadataFilePattern;
    }
    
    public void setShibbolethMetadataFilePattern(String shibbolethMetadataFilePattern) {
        this.shibbolethMetadataFilePattern = shibbolethMetadataFilePattern;
    }
    
    public String getStagedFilesBase() {
        return stagedFilesBase;
    }

    public void setStagedFilesBase(String stagedFilesBase) {
        this.stagedFilesBase = stagedFilesBase;
    }

    public int getTokenDuration() {
        return tokenDuration;
    }

    public void setTokenDuration(int tokenDuration) {
        this.tokenDuration = tokenDuration;
    }

    @Override
    public String toString() {
        return "ShibbolethPluginConfiguration [applicationName=" + applicationName + ", enabled=" + enabled
                + ", trustRelationshipDn=" + trustRelationshipDn + ", shibbolethMetadataDir=" + shibbolethMetadataDir
                + ", shibbolethMetadataFile=" + shibbolethMetadataFile + ", shibbolethMetadataFilePattern="
                + shibbolethMetadataFilePattern +" , stagedFilesBase=" + stagedFilesBase
                + ", tokenDuration=" + tokenDuration 
                + "]";
    }
    
    
}
