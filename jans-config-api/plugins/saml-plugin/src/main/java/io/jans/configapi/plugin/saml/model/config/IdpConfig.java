package io.jans.configapi.plugin.saml.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;


@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpConfig implements Serializable {
  
    private static final long serialVersionUID = 3681304284933513886L;
    
    private String configId;
    private String rootDir;
    private boolean enabled;
    private String metadataTempDir;
    private String metadataDir;
    private String metadataFilePattern;
    
    public String getConfigId() {
        return configId;
    }
    
    public void setConfigId(String configId) {
        this.configId = configId;
    }
    
    public String getRootDir() {
        return rootDir;
    }
    
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getMetadataTempDir() {
        return metadataTempDir;
    }
    
    public void setMetadataTempDir(String metadataTempDir) {
        this.metadataTempDir = metadataTempDir;
    }
    
    public String getMetadataDir() {
        return metadataDir;
    }
    
    public void setMetadataDir(String metadataDir) {
        this.metadataDir = metadataDir;
    }
    
    public String getMetadataFilePattern() {
        return metadataFilePattern;
    }
    
    public void setMetadataFilePattern(String metadataFilePattern) {
        this.metadataFilePattern = metadataFilePattern;
    }
    
    
    @Override
    public String toString() {
        return "IdpConfig [configId=" + configId + ", rootDir=" + rootDir + ", enabled=" + enabled
                + ", metadataTempDir=" + metadataTempDir + ", metadataDir=" + metadataDir + ", metadataFilePattern="
                + metadataFilePattern + "]";
    }

}
