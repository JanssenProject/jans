package io.jans.configapi.plugin.saml.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAppConfiguration implements Configuration {

    private String applicationName;
    private String samlTrustRelationshipDn;
    private String trustedIdpDn;
    private boolean enabled;
    private String selectedIdp;

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String scope;
    private String username;
    private String password;
    private String spMetadataUrl;
    private String tokenUrl;
    private String idpUrl;
    private String extIDPTokenUrl;
    private String extIDPRedirectUrl;
    private String idpMetadataImportUrl;

    private String idpRootDir;
    private String idpMetadataDir;
    private String idpMetadataTempDir;
    private String idpMetadataFile;

    private String spMetadataDir;
    private String spMetadataTempDir;
    private String spMetadataFile;

    private boolean ignoreValidation;
    private boolean setConfigDefaultValue;

    private List<String> idpMetadataMandatoryAttributes;
    private List<String> kcAttributes;
    private List<String> kcSamlConfig;
    
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
    
    public String getSelectedIdp() {
        return selectedIdp;
    }
    
    public void setSelectedIdp(String selectedIdp) {
        this.selectedIdp = selectedIdp;
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
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
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
    
    public String getSpMetadataUrl() {
        return spMetadataUrl;
    }
    
    public void setSpMetadataUrl(String spMetadataUrl) {
        this.spMetadataUrl = spMetadataUrl;
    }
    
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
    
    public String getIdpUrl() {
        return idpUrl;
    }
    
    public void setIdpUrl(String idpUrl) {
        this.idpUrl = idpUrl;
    }
    
    public String getExtIDPTokenUrl() {
        return extIDPTokenUrl;
    }
    
    public void setExtIDPTokenUrl(String extIDPTokenUrl) {
        this.extIDPTokenUrl = extIDPTokenUrl;
    }
    
    public String getExtIDPRedirectUrl() {
        return extIDPRedirectUrl;
    }
    
    public void setExtIDPRedirectUrl(String extIDPRedirectUrl) {
        this.extIDPRedirectUrl = extIDPRedirectUrl;
    }
    
    public String getIdpMetadataImportUrl() {
        return idpMetadataImportUrl;
    }
    
    public void setIdpMetadataImportUrl(String idpMetadataImportUrl) {
        this.idpMetadataImportUrl = idpMetadataImportUrl;
    }
    
    public String getIdpRootDir() {
        return idpRootDir;
    }
    
    public void setIdpRootDir(String idpRootDir) {
        this.idpRootDir = idpRootDir;
    }
    
    public String getIdpMetadataDir() {
        return idpMetadataDir;
    }
    
    public void setIdpMetadataDir(String idpMetadataDir) {
        this.idpMetadataDir = idpMetadataDir;
    }
    
    public String getIdpMetadataTempDir() {
        return idpMetadataTempDir;
    }
    
    public void setIdpMetadataTempDir(String idpMetadataTempDir) {
        this.idpMetadataTempDir = idpMetadataTempDir;
    }
    
    public String getIdpMetadataFile() {
        return idpMetadataFile;
    }
    
    public void setIdpMetadataFile(String idpMetadataFile) {
        this.idpMetadataFile = idpMetadataFile;
    }
    
    public String getSpMetadataDir() {
        return spMetadataDir;
    }
    
    public void setSpMetadataDir(String spMetadataDir) {
        this.spMetadataDir = spMetadataDir;
    }
    
    public String getSpMetadataTempDir() {
        return spMetadataTempDir;
    }
    
    public void setSpMetadataTempDir(String spMetadataTempDir) {
        this.spMetadataTempDir = spMetadataTempDir;
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
    
    public boolean isSetConfigDefaultValue() {
        return setConfigDefaultValue;
    }

    public void setSetConfigDefaultValue(boolean setConfigDefaultValue) {
        this.setConfigDefaultValue = setConfigDefaultValue;
    }
    
    public List<String> getIdpMetadataMandatoryAttributes() {
        return idpMetadataMandatoryAttributes;
    }
    
    public void setIdpMetadataMandatoryAttributes(List<String> idpMetadataMandatoryAttributes) {
        this.idpMetadataMandatoryAttributes = idpMetadataMandatoryAttributes;
    }
    
    public List<String> getKcAttributes() {
        return kcAttributes;
    }
    
    public void setKcAttributes(List<String> kcAttributes) {
        this.kcAttributes = kcAttributes;
    }
    
    public List<String> getKcSamlConfig() {
        return kcSamlConfig;
    }
    
    public void setKcSamlConfig(List<String> kcSamlConfig) {
        this.kcSamlConfig = kcSamlConfig;
    }

    @Override
    public String toString() {
        return "SamlAppConfiguration [applicationName=" + applicationName + ", samlTrustRelationshipDn="
                + samlTrustRelationshipDn + ", trustedIdpDn=" + trustedIdpDn + ", enabled=" + enabled + ", selectedIdp="
                + selectedIdp + ", serverUrl=" + serverUrl + ", realm=" + realm + ", clientId=" + clientId
                + ", clientSecret=" + clientSecret + ", grantType=" + grantType + ", scope=" + scope + ", username="
                + username + ", spMetadataUrl=" + spMetadataUrl + ", tokenUrl=" + tokenUrl
                + ", idpUrl=" + idpUrl + ", extIDPTokenUrl=" + extIDPTokenUrl + ", extIDPRedirectUrl="
                + extIDPRedirectUrl + ", idpMetadataImportUrl=" + idpMetadataImportUrl + ", idpRootDir=" + idpRootDir
                + ", idpMetadataDir=" + idpMetadataDir + ", idpMetadataTempDir=" + idpMetadataTempDir
                + ", idpMetadataFile=" + idpMetadataFile + ", spMetadataDir=" + spMetadataDir + ", spMetadataTempDir="
                + spMetadataTempDir + ", spMetadataFile=" + spMetadataFile + ", ignoreValidation=" + ignoreValidation
                + ", setConfigDefaultValue=" + setConfigDefaultValue + ", idpMetadataMandatoryAttributes="
                + idpMetadataMandatoryAttributes + ", kcAttributes=" + kcAttributes + ", kcSamlConfig=" + kcSamlConfig
                + "]";
    }
    
}
