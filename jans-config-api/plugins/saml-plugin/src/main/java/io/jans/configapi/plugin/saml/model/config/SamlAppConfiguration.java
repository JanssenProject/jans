package io.jans.configapi.plugin.saml.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAppConfiguration implements Configuration {

    private boolean samlEnabled;
    private String samlIdpRootDir;
    
    
    public boolean isSamlEnabled() {
        return samlEnabled;
    }
    public void setSamlEnabled(boolean samlEnabled) {
        this.samlEnabled = samlEnabled;
    }
    public String getSamlIdpRootDir() {
        return samlIdpRootDir;
    }
    public void setSamlIdpRootDir(String samlIdpRootDir) {
        this.samlIdpRootDir = samlIdpRootDir;
    }
    
    
    @Override
    public String toString() {
        return "SamlAppConfiguration [samlEnabled=" + samlEnabled + ", samlIdpRootDir=" + samlIdpRootDir + "]";
    }
    
    

}

