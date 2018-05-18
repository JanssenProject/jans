package org.xdi.oxd.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.Configuration;
import org.xdi.oxd.server.service.Rp;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxdServerConfiguration extends Configuration {

    //@JsonProperty(value = "register_client_app_type")
    private String registerClientAppType = "web";
    //    @JsonProperty(value = "register_client_response_types")
    private String registerClientResponesType = "code";
    @JsonProperty(value = "server_name")
    private String serverName;
    @JsonProperty(value = "use_client_authentication_for_pat")
    private Boolean useClientAuthenticationForPat = true;
    @JsonProperty(value = "trust_all_certs")
    private Boolean trustAllCerts;
    @JsonProperty(value = "trust_store_path")
    private String keyStorePath;
    @JsonProperty(value = "trust_store_password")
    private String keyStorePassword;
    @JsonProperty(value = "crypt_provider_key_store_path")
    private String cryptProviderKeyStorePath;
    @JsonProperty(value = "crypt_provider_key_store_password")
    private String cryptProviderKeyStorePassword;
    @JsonProperty(value = "crypt_provider_dn_name")
    private String cryptProviderDnName;
    @JsonProperty(value = "license_id")
    private String licenseId;
    @JsonProperty(value = "public_key")
    private String publicKey;
    @JsonProperty(value = "public_password")
    private String publicPassword;
    @JsonProperty(value = "license_password")
    private String licensePassword;
    @JsonProperty(value = "support-google-logout")
    private Boolean supportGoogleLogout = true;
    @JsonProperty(value = "state_expiration_in_minutes")
    private int stateExpirationInMinutes = 5;
    @JsonProperty(value = "nonce_expiration_in_minutes")
    private int nonceExpirationInMinutes = 5;
    @JsonProperty(value = "public_op_key_cache_expiration_in_minutes")
    private int publicOpKeyCacheExpirationInMinutes = 60;
    @JsonProperty(value = "protect_commands_with_access_token")
    private Boolean protectCommandsWithAccessToken;
    @JsonProperty(value = "uma2_auto_register_claims_gathering_endpoint_as_redirect_uri_of_client")
    private Boolean uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient;
    @JsonProperty(value = "migration_source_folder_path")
    private String migrationSourceFolderPath;
    @JsonProperty(value = "storage")
    private String storage;
    @JsonProperty(value = "storage_configuration")
    private JsonNode storageConfiguration;
    @JsonProperty(value = "remove_expired_clients")
    private Boolean removeExpiredClients = false;
    @JsonProperty(value = "defaultSiteConfig")
    private Rp defaultSiteConfig;

    public Rp getDefaultSiteConfig() {
        return defaultSiteConfig;
    }

    public void setDefaultSiteConfig(Rp defaultSiteConfig) {
        this.defaultSiteConfig = defaultSiteConfig;
    }

    public Boolean getRemoveExpiredClients() {
        return removeExpiredClients;
    }

    public void setRemoveExpiredClients(Boolean removeExpiredClients) {
        this.removeExpiredClients = removeExpiredClients;
    }

    public String getCryptProviderKeyStorePath() {
        return cryptProviderKeyStorePath;
    }

    public void setCryptProviderKeyStorePath(String cryptProviderKeyStorePath) {
        this.cryptProviderKeyStorePath = cryptProviderKeyStorePath;
    }

    public String getCryptProviderKeyStorePassword() {
        return cryptProviderKeyStorePassword;
    }

    public void setCryptProviderKeyStorePassword(String cryptProviderKeyStorePassword) {
        this.cryptProviderKeyStorePassword = cryptProviderKeyStorePassword;
    }

    public String getCryptProviderDnName() {
        return cryptProviderDnName;
    }

    public void setCryptProviderDnName(String cryptProviderDnName) {
        this.cryptProviderDnName = cryptProviderDnName;
    }

    public String getMigrationSourceFolderPath() {
        return migrationSourceFolderPath;
    }

    public void setMigrationSourceFolderPath(String migrationSourceFolderPath) {
        this.migrationSourceFolderPath = migrationSourceFolderPath;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Boolean getUma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient() {
        return uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient;
    }

    public void setUma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient(Boolean uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient) {
        this.uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient = uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient;
    }

    public int getStateExpirationInMinutes() {
        return stateExpirationInMinutes;
    }

    public void setStateExpirationInMinutes(int stateExpirationInMinutes) {
        this.stateExpirationInMinutes = stateExpirationInMinutes;
    }

    public Boolean getProtectCommandsWithAccessToken() {
        return protectCommandsWithAccessToken;
    }

    public void setProtectCommandsWithAccessToken(Boolean protectCommandsWithAccessToken) {
        this.protectCommandsWithAccessToken = protectCommandsWithAccessToken;
    }

    public int getPublicOpKeyCacheExpirationInMinutes() {
        return publicOpKeyCacheExpirationInMinutes;
    }

    public void setPublicOpKeyCacheExpirationInMinutes(int publicOpKeyCacheExpirationInMinutes) {
        this.publicOpKeyCacheExpirationInMinutes = publicOpKeyCacheExpirationInMinutes;
    }

    public int getNonceExpirationInMinutes() {
        return nonceExpirationInMinutes;
    }

    public void setNonceExpirationInMinutes(int nonceExpirationInMinutes) {
        this.nonceExpirationInMinutes = nonceExpirationInMinutes;
    }

    public Boolean getSupportGoogleLogout() {
        return supportGoogleLogout;
    }

    public void setSupportGoogleLogout(Boolean supportGoogleLogout) {
        this.supportGoogleLogout = supportGoogleLogout;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public void setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public void setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public Boolean getTrustAllCerts() {
        return trustAllCerts;
    }

    public void setTrustAllCerts(Boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    public Boolean getUseClientAuthenticationForPat() {
        return useClientAuthenticationForPat;
    }

    public void setUseClientAuthenticationForPat(Boolean useClientAuthenticationForPat) {
        this.useClientAuthenticationForPat = useClientAuthenticationForPat;
    }

    public String getRegisterClientResponesType() {
        return registerClientResponesType;
    }

    public void setRegisterClientResponesType(String p_registerClientResponesType) {
        registerClientResponesType = p_registerClientResponesType;
    }

    public String getRegisterClientAppType() {
        return registerClientAppType;
    }

    public void setRegisterClientAppType(String p_registerClientAppType) {
        registerClientAppType = p_registerClientAppType;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public JsonNode getStorageConfiguration() {
        return storageConfiguration;
    }

    public void setStorageConfiguration(JsonNode storageConfiguration) {
        this.storageConfiguration = storageConfiguration;
    }

    @Override
    public String toString() {
        return "OxdServerConfiguration{" +
                ", registerClientAppType='" + registerClientAppType + '\'' +
                ", registerClientResponesType='" + registerClientResponesType + '\'' +
                ", serverName='" + serverName + '\'' +
                ", useClientAuthenticationForPat=" + useClientAuthenticationForPat +
                ", trustAllCerts=" + trustAllCerts +
                ", keyStorePath='" + keyStorePath + '\'' +
                ", keyStorePassword='" + keyStorePassword + '\'' +
                ", cryptProviderKeyStorePath='" + cryptProviderKeyStorePath + '\'' +
                ", cryptProviderKeyStorePassword='" + cryptProviderKeyStorePassword + '\'' +
                ", cryptProviderDnName='" + cryptProviderDnName + '\'' +
                ", licenseId='" + licenseId + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", publicPassword='" + publicPassword + '\'' +
                ", licensePassword='" + licensePassword + '\'' +
                ", supportGoogleLogout=" + supportGoogleLogout +
                ", stateExpirationInMinutes=" + stateExpirationInMinutes +
                ", nonceExpirationInMinutes=" + nonceExpirationInMinutes +
                ", publicOpKeyCacheExpirationInMinutes=" + publicOpKeyCacheExpirationInMinutes +
                ", protectCommandsWithAccessToken=" + protectCommandsWithAccessToken +
                ", uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient=" + uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient +
                ", migrationSourceFolderPath='" + migrationSourceFolderPath + '\'' +
                ", storage='" + storage + '\'' +
                ", storageConfiguration=" + storageConfiguration +
                ", defaultSiteConfig=" + defaultSiteConfig +
                '}';
    }
}
