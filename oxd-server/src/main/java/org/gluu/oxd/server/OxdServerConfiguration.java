package org.gluu.oxd.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxdServerConfiguration extends Configuration {

    //@JsonProperty(value = "register_client_app_type")
    private String registerClientAppType = "web";
    //    @JsonProperty(value = "register_client_response_types")
    private String registerClientResponesType = "code";
    @JsonProperty(value = "use_client_authentication_for_pat")
    private Boolean useClientAuthenticationForPat = true;
    @JsonProperty(value = "trust_all_certs")
    private Boolean trustAllCerts;
    @JsonProperty(value = "trust_store_path")
    private String keyStorePath;
    @JsonProperty(value = "trust_store_password")
    private String keyStorePassword;
    @JsonProperty(value = "enable_jwks_generation")
    private Boolean enableJwksGeneration = true;
    @JsonProperty(value = "crypt_provider_key_store_path")
    private String cryptProviderKeyStorePath;
    @JsonProperty(value = "crypt_provider_key_store_password")
    private String cryptProviderKeyStorePassword;
    @JsonProperty(value = "crypt_provider_dn_name")
    private String cryptProviderDnName;
    @JsonProperty(value = "jwks_expiration_in_hours")
    private int jwksExpirationInHours = 720;
    @JsonProperty(value = "jwks_regeneration_interval_in_hours")
    private int jwksRegenerationIntervalInHours = 720;
    @JsonProperty(value = "support-google-logout")
    private Boolean supportGoogleLogout = true;
    @JsonProperty(value = "state_expiration_in_minutes")
    private int stateExpirationInMinutes = 5;
    @JsonProperty(value = "nonce_expiration_in_minutes")
    private int nonceExpirationInMinutes = 5;
    @JsonProperty(value = "request_object_expiration_in_minutes")
    private int requestObjectExpirationInMinutes = 5;
    @JsonProperty(value = "db_cleanup_interval_in_hours")
    private int dbCleanupIntervalInHours = 1;
    @JsonProperty(value = "rp_cache_expiration_in_minutes")
    private int rpCacheExpirationInMinutes = 60;
    @JsonProperty(value = "public_op_key_cache_expiration_in_minutes")
    private int publicOpKeyCacheExpirationInMinutes = 60;
    @JsonProperty(value = "protect_commands_with_access_token")
    private Boolean protectCommandsWithAccessToken;
    @JsonProperty(value = "accept_id_token_without_signature")
    private Boolean acceptIdTokenWithoutSignature = false;
    @JsonProperty(value = "id_token_validation_c_hash_required")
    private Boolean idTokenValidationCHashRequired = true;
    @JsonProperty(value = "id_token_validation_at_hash_required")
    private Boolean idTokenValidationAtHashRequired = true;
    @JsonProperty(value = "validate_user_info_with_id_token")
    private Boolean validateUserInfoWithIdToken = false;
    @JsonProperty(value = "uma2_auto_register_claims_gathering_endpoint_as_redirect_uri_of_client")
    private Boolean uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient;
    @JsonProperty(value = "add_client_credentials_grant_type_automatically_during_client_registration")
    private Boolean addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration = true;
    @JsonProperty(value = "migration_source_folder_path")
    private String migrationSourceFolderPath;
    @JsonProperty(value = "allowed_op_hosts")
    private List<String> allowedOpHosts = Lists.newArrayList();
    @JsonProperty(value = "storage")
    private String storage;
    @JsonProperty(value = "storage_configuration")
    private JsonNode storageConfiguration;
    @JsonProperty(value = "defaultSiteConfig")
    private JsonNode defaultSiteConfig;
    @JsonProperty(value = "tracer")
    private String tracer;
    @JsonProperty(value = "tracer_host")
    private String tracerHost;
    @JsonProperty(value = "tracer_port")
    private int tracerPort = 5775;
    @JsonProperty(value = "enable_tracing")
    private Boolean enableTracing = false;
    @JsonProperty(value = "proxy_configuration")
    private JsonNode proxyConfiguration;
    @JsonProperty(value = "protect_commands_with_oxd_id")
    private List<String> protectCommandsWithOxdId;
    @JsonProperty(value = "persistence_manager_remove_count")
    private int persistenceManagerRemoveCount = 1000;
    @JsonProperty(value = "bind_ip_addresses")
    private List<String> bindIpAddresses;

    public Boolean getEnableTracing() {
        return enableTracing;
    }

    public void setEnableTracing(Boolean enableTracing) {
        this.enableTracing = enableTracing;
    }

    public String getTracer() {
        return tracer;
    }

    public void setTracer(String tracer) {
        this.tracer = tracer;
    }

    public String getTracerHost() {
        return tracerHost;
    }

    public void setTracerHost(String tracerHost) {
        this.tracerHost = tracerHost;
    }

    public int getTracerPort() {
        return tracerPort;
    }

    public void setTracerPort(int tracerPort) {
        this.tracerPort = tracerPort;
    }

    public JsonNode getDefaultSiteConfig() {
        return defaultSiteConfig;
    }

    public void setDefaultSiteConfig(JsonNode defaultSiteConfig) {
        this.defaultSiteConfig = defaultSiteConfig;
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

    public int getRpCacheExpirationInMinutes() {
        return rpCacheExpirationInMinutes;
    }

    public void setRpCacheExpirationInMinutes(int rpCacheExpirationInMinutes) {
        this.rpCacheExpirationInMinutes = rpCacheExpirationInMinutes;
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

    public Boolean getAddClientCredentialsGrantTypeAutomaticallyDuringClientRegistration() {
        return addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration;
    }

    public void setAddClientCredentialsGrantTypeAutomaticallyDuringClientRegistration(Boolean addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration) {
        this.addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration = addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration;
    }

    public List<String> getAllowedOpHosts() {
        return allowedOpHosts;
    }

    public void setAllowedOpHosts(List<String> allowedOpHosts) {
        this.allowedOpHosts = allowedOpHosts;
    }

    public int getDbCleanupIntervalInHours() {
        return dbCleanupIntervalInHours;
    }

    public void setDbCleanupIntervalInHours(int dbCleanupIntervalInHours) {
        this.dbCleanupIntervalInHours = dbCleanupIntervalInHours;
    }

    public JsonNode getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration(JsonNode proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    public Boolean getAcceptIdTokenWithoutSignature() {
        return acceptIdTokenWithoutSignature;
    }

    public void setAcceptIdTokenWithoutSignature(Boolean acceptIdTokenWithoutSignature) {
        this.acceptIdTokenWithoutSignature = acceptIdTokenWithoutSignature;
    }

    public Boolean getIdTokenValidationCHashRequired() {
        return idTokenValidationCHashRequired;
    }

    public void setIdTokenValidationCHashRequired(Boolean idTokenValidationCHashRequired) {
        this.idTokenValidationCHashRequired = idTokenValidationCHashRequired;
    }

    public Boolean getIdTokenValidationAtHashRequired() {
        return idTokenValidationAtHashRequired;
    }

    public void setIdTokenValidationAtHashRequired(Boolean idTokenValidationAtHashRequired) {
        this.idTokenValidationAtHashRequired = idTokenValidationAtHashRequired;
    }

    public Boolean getValidateUserInfoWithIdToken() {
        return validateUserInfoWithIdToken;
    }

    public void setValidateUserInfoWithIdToken(Boolean validateUserInfoWithIdToken) {
        this.validateUserInfoWithIdToken = validateUserInfoWithIdToken;
    }

    public List<String> getProtectCommandsWithOxdId() {
        return protectCommandsWithOxdId;
    }

    public void setProtectCommandsWithOxdId(List<String> protectCommandsWithOxdId) {
        this.protectCommandsWithOxdId = protectCommandsWithOxdId;
    }

    public int getPersistenceManagerRemoveCount() {
        return persistenceManagerRemoveCount;
    }

    public void setPersistenceManagerRemoveCount(int persistenceManagerRemoveCount) {
        this.persistenceManagerRemoveCount = persistenceManagerRemoveCount;
    }

    public List<String> getBindIpAddresses() {
        return bindIpAddresses;
    }

    public void setBindIpAddresses(List<String> bindIpAddresses) {
        this.bindIpAddresses = bindIpAddresses;
    }

    public Boolean getEnableJwksGeneration() {
        return enableJwksGeneration;
    }

    public void setEnableJwksGeneration(Boolean enableJwksGeneration) {
        this.enableJwksGeneration = enableJwksGeneration;
    }

    public int getJwksExpirationInHours() {
        return jwksExpirationInHours;
    }

    public void setJwksExpirationInHours(int jwksExpirationInHours) {
        this.jwksExpirationInHours = jwksExpirationInHours;
    }

    public int getRequestObjectExpirationInMinutes() {
        return requestObjectExpirationInMinutes;
    }

    public void setRequestObjectExpirationInMinutes(int requestObjectExpirationInMinutes) {
        this.requestObjectExpirationInMinutes = requestObjectExpirationInMinutes;
    }

    public int getJwksRegenerationIntervalInHours() {
        return jwksRegenerationIntervalInHours;
    }

    public void setJwksRegenerationIntervalInHours(int jwksRegenerationIntervalInHours) {
        this.jwksRegenerationIntervalInHours = jwksRegenerationIntervalInHours;
    }

    @Override
    public String toString() {
        return "OxdServerConfiguration{" +
                ", registerClientAppType='" + registerClientAppType + '\'' +
                ", registerClientResponesType='" + registerClientResponesType + '\'' +
                ", useClientAuthenticationForPat=" + useClientAuthenticationForPat +
                ", trustAllCerts=" + trustAllCerts +
                ", keyStorePath='" + keyStorePath + '\'' +
                ", keyStorePassword='" + keyStorePassword + '\'' +
                ", cryptProviderKeyStorePath='" + cryptProviderKeyStorePath + '\'' +
                ", cryptProviderKeyStorePassword='" + cryptProviderKeyStorePassword + '\'' +
                ", cryptProviderDnName='" + cryptProviderDnName + '\'' +
                ", supportGoogleLogout=" + supportGoogleLogout +
                ", stateExpirationInMinutes=" + stateExpirationInMinutes +
                ", nonceExpirationInMinutes=" + nonceExpirationInMinutes +
                ", dbCleanupIntervalInHours=" + dbCleanupIntervalInHours +
                ", rpCacheExpirationInMinutes=" + rpCacheExpirationInMinutes +
                ", publicOpKeyCacheExpirationInMinutes=" + publicOpKeyCacheExpirationInMinutes +
                ", protectCommandsWithAccessToken=" + protectCommandsWithAccessToken +
                ", acceptIdTokenWithoutSignature=" + acceptIdTokenWithoutSignature +
                ", uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient=" + uma2AuthRegisterClaimsGatheringEndpointAsRedirectUriOfClient +
                ", migrationSourceFolderPath='" + migrationSourceFolderPath + '\'' +
                ", storage='" + storage + '\'' +
                ", storageConfiguration=" + storageConfiguration + '\'' +
                ", defaultSiteConfig=" + defaultSiteConfig + '\'' +
                ", allowedOpHosts=" + allowedOpHosts + '\'' +
                ", addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration=" + addClientCredentialsGrantTypeAutomaticallyDuringClientRegistration +
                ", proxyConfiguration=" + proxyConfiguration + '\'' +
                ", idTokenValidationCHashRequired=" + idTokenValidationCHashRequired + '\'' +
                ", idTokenValidationAtHashRequired=" + idTokenValidationAtHashRequired + '\'' +
                ", validateUserInfoWithIdToken=" + validateUserInfoWithIdToken + '\'' +
                ", protectCommandsWithOxdId=" + protectCommandsWithOxdId + '\'' +
                ", persistenceManagerRemoveCount=" + persistenceManagerRemoveCount + '\'' +
                ", bindIpAddresses=" + bindIpAddresses + '\'' +
                ", enableJwksGeneration=" + enableJwksGeneration + '\'' +
                ", jwksExpirationInHours=" + jwksExpirationInHours + '\'' +
                ", requestObjectExpirationInMinutes=" + requestObjectExpirationInMinutes + '\'' +
                ", jwksRegenerationIntervalInHours=" + jwksRegenerationIntervalInHours + '\'' +
                '}';
    }
}
