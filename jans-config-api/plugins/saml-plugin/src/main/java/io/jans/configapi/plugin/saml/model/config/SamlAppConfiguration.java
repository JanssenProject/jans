package io.jans.configapi.plugin.saml.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAppConfiguration implements Configuration {

    private String applicationName;
    private String samlBaseDn;
    private boolean samlEnabled;
    private String selectedIdp;
    private String idpRootDir;
    private String idpTempMetadataFolder;
    private String idpMetadataFolder;
    private String idpMetadataFilePattern;
    private String spMetadataFilePattern;
    
    private List<IdpConfig> idpConfigs;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSamlBaseDn() {
        return samlBaseDn;
    }

    public void setSamlBaseDn(String samlBaseDn) {
        this.samlBaseDn = samlBaseDn;
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

    public String getIdpTempMetadataFolder() {
        return idpTempMetadataFolder;
    }

    public void setIdpTempMetadataFolder(String idpTempMetadataFolder) {
        this.idpTempMetadataFolder = idpTempMetadataFolder;
    }

    public String getIdpMetadataFolder() {
        return idpMetadataFolder;
    }

    public void setIdpMetadataFolder(String idpMetadataFolder) {
        this.idpMetadataFolder = idpMetadataFolder;
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

    public List<IdpConfig> getIdpConfigs() {
        return idpConfigs;
    }

    public void setIdpConfigs(List<IdpConfig> idpConfigs) {
        this.idpConfigs = idpConfigs;
    }

    @Override
    public String toString() {
        return "SamlConfig [applicationName=" + applicationName + ", samlBaseDn=" + samlBaseDn + ", samlEnabled="
                + samlEnabled + ", selectedIdp=" + selectedIdp + ", idpRootDir=" + idpRootDir
                + ", idpTempMetadataFolder=" + idpTempMetadataFolder + ", idpMetadataFolder=" + idpMetadataFolder
                + ", idpMetadataFilePattern=" + idpMetadataFilePattern + ", spMetadataFilePattern="
                + spMetadataFilePattern + ", idpConfigs=" + idpConfigs + "]";
    }

}

