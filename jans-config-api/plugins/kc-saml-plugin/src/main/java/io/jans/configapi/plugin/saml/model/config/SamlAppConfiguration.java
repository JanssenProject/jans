package io.jans.configapi.plugin.saml.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAppConfiguration implements Configuration {

    @Schema(description = "Name of application.")
    private String applicationName;
    
    @Schema(description = "Trust relationship organizational unit.")
    private String samlTrustRelationshipDn;
    
    @Schema(description = "Identity provider organizational unit.")
    private String trustedIdpDn;
    
    @Schema(description = "SAML functionality enabled.")
    private boolean enabled;
    
    @Schema(description = "Selected SAML server.")
    private String selectedIdp;

    @Schema(description = "SAML server URL.")
    private String serverUrl;
    
    @Schema(description = "SAML server realm, default is `jans`.")
    private String realm;
    
    @Schema(description = "Jans auth SAML client ID.")
    private String clientId;
    
    @Schema(description = "Jans auth SAML client password.")
    private String clientSecret;
    
    @Schema(description = "Grant type to get access token.")
    private String grantType;
    
    @Schema(description = "Oauth2 scope to get access token.")
    private String scope;
    
    @Schema(description = "SAML server username.")
    private String username;
    
    @Schema(description = "SAML server user credentails.")
    private String password;
    
    @Schema(description = "Relative SAML server SP Metadata Url.")
    private String spMetadataUrl;
    
    @Schema(description = "Relative SAML server Token Url.")
    private String tokenUrl;
    
    @Schema(description = "Relative SAML server IDP Url.")
    private String idpUrl;
    
    @Schema(description = "Relative SAML server IDP Token Url.")
    private String extIDPTokenUrl;
    
    @Schema(description = "Relative IDP redirect Url.")
    private String extIDPRedirectUrl;
    
    @Schema(description = "Relative SAML server Metadata import Url.")
    private String idpMetadataImportUrl;

    @Schema(description = "Jans Auth server root SAML directory.")
    private String idpRootDir;
    
    @Schema(description = "Jans Auth server relative SAML directory to store IDP Metadata files.")
    private String idpMetadataDir;
    
    @Schema(description = "Jans Auth server relative SAML temp directory to store IDP Metadata files.")
    private String idpMetadataTempDir;
    
    @Schema(description = "IDP Metadata file name format.")
    private String idpMetadataFile;

    @Schema(description = "Jans Auth server relative SAML directory to store SP Metadata files.")
    private String spMetadataDir;
    
    @Schema(description = "Jans Auth server relative SAML temp directory to store SP Metadata files.")
    private String spMetadataTempDir;
    
    @Schema(description = "SP Metadata file name format.")
    private String spMetadataFile;

    @Schema(description = "Boolean value to enable/disable SAML validation.")
    private boolean ignoreValidation;
    
    @Schema(description = "Boolean value `true` to set the default values for an IDP.")
    private boolean setConfigDefaultValue;

    @Schema(description = "List of mandatory IDP Metadata attributes.")
    private List<String> idpMetadataMandatoryAttributes;
    
    @Schema(description = "Keycloak SAML attribute names.")
    private List<String> kcAttributes;
    
    @Schema(description = "Keycloak SAML config attribute names.")
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
