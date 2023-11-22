package io.jans.configapi.plugin.keycloak.idp.broker.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpAppConfiguration implements Configuration {

    private String applicationName;
    private String trustedIdpDn;
    private boolean enabled;

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String username;
    private String password;

    private String idpRootDir;
    private String idpMetadataRootDir;
    private String idpMetadataTempDir;
    private String idpMetadataFilePattern;
    private String idpMetadataFile;

    private String spMetadataUrl;
    private String spMetadataRootDir;
    private String spMetadataTempDir;
    private String spMetadataFilePattern;
    private String spMetadataFile;

    private boolean ignoreValidation;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getTrustedIdpDn() {
        return trustedIdpDn;
    }

    public void setTrustedIdpDn(String trustedIdpDn) {
        this.trustedIdpDn = trustedIdpDn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdpRootDir() {
        return idpRootDir;
    }

    public void setIdpRootDir(String idpRootDir) {
        this.idpRootDir = idpRootDir;
    }

    public String getIdpMetadataRootDir() {
        return idpMetadataRootDir;
    }

    public void setIdpMetadataRootDir(String idpMetadataRootDir) {
        this.idpMetadataRootDir = idpMetadataRootDir;
    }

    public String getIdpMetadataTempDir() {
        return idpMetadataTempDir;
    }

    public void setIdpMetadataTempDir(String idpMetadataTempDir) {
        this.idpMetadataTempDir = idpMetadataTempDir;
    }

    public String getIdpMetadataFilePattern() {
        return idpMetadataFilePattern;
    }

    public void setIdpMetadataFilePattern(String idpMetadataFilePattern) {
        this.idpMetadataFilePattern = idpMetadataFilePattern;
    }

    public String getIdpMetadataFile() {
        return idpMetadataFile;
    }

    public void setIdpMetadataFile(String idpMetadataFile) {
        this.idpMetadataFile = idpMetadataFile;
    }
    
    public String getSpMetadataUrl() {
        return spMetadataUrl;
    }

    public void setSpMetadataUrl(String spMetadataUrl) {
        this.spMetadataUrl = spMetadataUrl;
    }

    public String getSpMetadataRootDir() {
        return spMetadataRootDir;
    }

    public void setSpMetadataRootDir(String spMetadataRootDir) {
        this.spMetadataRootDir = spMetadataRootDir;
    }

    public String getSpMetadataTempDir() {
        return spMetadataTempDir;
    }

    public void setSpMetadataTempDir(String spMetadataTempDir) {
        this.spMetadataTempDir = spMetadataTempDir;
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

    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    @Override
    public String toString() {
        return "IdpAppConfiguration [applicationName=" + applicationName + ", trustedIdpDn=" + trustedIdpDn
                + ", enabled=" + enabled + ", serverUrl=" + serverUrl + ", realm=" + realm + ", clientId=" + clientId
                + ", clientSecret=" + clientSecret + ", grantType=" + grantType + ", username=" + username
                + ", password=" + password + ", idpRootDir=" + idpRootDir + ", idpMetadataRootDir=" + idpMetadataRootDir
                + ", idpMetadataTempDir=" + idpMetadataTempDir + ", idpMetadataFilePattern=" + idpMetadataFilePattern
                + ", idpMetadataFile=" + idpMetadataFile + ", spMetadataUrl=" + spMetadataUrl + ", spMetadataRootDir="
                + spMetadataRootDir + ", spMetadataTempDir=" + spMetadataTempDir + ", spMetadataFilePattern="
                + spMetadataFilePattern + ", spMetadataFile=" + spMetadataFile + ", ignoreValidation="
                + ignoreValidation + "]";
    }

}
