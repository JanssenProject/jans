package io.jans.configapi.plugin.keycloak.idp.broker.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpAppConfiguration implements Configuration {

    private String applicationName;
    private String trustedIdpDn;
    private boolean idpEnabled;
    private String idpRootDir;
    private String idpTempDir;
    private String spMetadataFilePattern;
    private String spMetadataFile;
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String username;
    private String password;
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

    public boolean isIdpEnabled() {
        return idpEnabled;
    }

    public void setIdpEnabled(boolean idpEnabled) {
        this.idpEnabled = idpEnabled;
    }

    public String getIdpRootDir() {
        return idpRootDir;
    }

    public void setIdpRootDir(String idpRootDir) {
        this.idpRootDir = idpRootDir;
    }

    public String getIdpTempDir() {
        return idpTempDir;
    }

    public void setIdpTempDir(String idpTempDir) {
        this.idpTempDir = idpTempDir;
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

    public boolean isIgnoreValidation() {
        return ignoreValidation;
    }

    public void setIgnoreValidation(boolean ignoreValidation) {
        this.ignoreValidation = ignoreValidation;
    }

    @Override
    public String toString() {
        return "IdpAppConfiguration [applicationName=" + applicationName + ", trustedIdpDn=" + trustedIdpDn
                + ", idpEnabled=" + idpEnabled + ", idpRootDir=" + idpRootDir + ", idpTempDir=" + idpTempDir
                + ", spMetadataFilePattern=" + spMetadataFilePattern + ", spMetadataFile=" + spMetadataFile
                + ", serverUrl=" + serverUrl + ", realm=" + realm + ", clientId=" + clientId + ", clientSecret="
                + clientSecret + ", grantType=" + grantType + ", username=" + username + ", password=" + password
                + ", ignoreValidation=" + ignoreValidation + "]";
    }

}
