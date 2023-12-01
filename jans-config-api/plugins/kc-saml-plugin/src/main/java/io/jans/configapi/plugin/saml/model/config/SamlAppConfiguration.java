package io.jans.configapi.plugin.saml.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAppConfiguration implements Configuration {

    private String applicationName;
    private String samlTrustRelationshipDn;
    private boolean samlEnabled;
    private String selectedIdp;
    private String idpRootDir;
    private String idpMetadataFilePattern;
    private String spMetadataFilePattern;
    private String spMetadataFile;
    private boolean configGeneration;
    private boolean ignoreValidation;
    
    private List<IdpConfig> idpConfigs;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSamlTrustRelationshipDn() {
        return samlTrustRelationshipDn;
    }

    public void setSamlTrustRelationshipDn(String samlTrustRelationshipDn) {
        this.samlTrustRelationshipDn = samlTrustRelationshipDn;
    }

    public boolean isSamlEnabled() {
        return samlEnabled;
    }

    public void setSamlEnabled(boolean samlEnabled) {
        this.samlEnabled = samlEnabled;
    }

    public String getSelectedIdp() {
        return selectedIdp;
    }

    public void setSelectedIdp(String selectedIdp) {
        this.selectedIdp = selectedIdp;
    }

    public String getIdpRootDir() {
        return idpRootDir;
    }

    public void setIdpRootDir(String idpRootDir) {
        this.idpRootDir = idpRootDir;
    }

    public String getIdpMetadataFilePattern() {
        return idpMetadataFilePattern;
    }

    public void setIdpMetadataFilePattern(String idpMetadataFilePattern) {
        this.idpMetadataFilePattern = idpMetadataFilePattern;
    }

    public String getSpMetadataFilePattern() {
        return spMetadataFilePattern;
    }

    public void setSpMetadataFilePattern(String spMetadataFilePattern) {
        this.spMetadataFilePattern = spMetadataFilePattern;
    }
    
    public String getSpMetadataFile() {
        return spMetadataFile;
    }

    public void setSpMetadataFile(String spMetadataFile) {
        this.spMetadataFile = spMetadataFile;
    }

    public boolean isConfigGeneration() {
        return configGeneration;
    }

    public void setConfigGeneration(boolean configGeneration) {
        this.configGeneration = configGeneration;
    }

    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    public List<IdpConfig> getIdpConfigs() {
        return idpConfigs;
    }

    public void setIdpConfigs(List<IdpConfig> idpConfigs) {
        this.idpConfigs = idpConfigs;
    }

    @Override
    public String toString() {
        return "SamlAppConfiguration [applicationName=" + applicationName + ", samlTrustRelationshipDn="
                + samlTrustRelationshipDn + ", samlEnabled=" + samlEnabled + ", selectedIdp=" + selectedIdp
                + ", idpRootDir=" + idpRootDir + ", idpMetadataFilePattern=" + idpMetadataFilePattern
                + ", spMetadataFilePattern=" + spMetadataFilePattern + ", configGeneration=" + configGeneration
                + ", ignoreValidation=" + ignoreValidation + ", idpConfigs=" + idpConfigs + "]";
    }   
}
