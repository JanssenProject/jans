/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * oxD configuration.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 27/07/2013
 */
public class Configuration {

    public static final String DOC_URL = "https://www.gluu.org/docs-oxd/";

    @JsonProperty(value = "port")
    private int port;
    @JsonProperty(value = "time_out_in_seconds")
    private int timeOutInSeconds;
    //@JsonProperty(value = "register_client_app_type")
    private String registerClientAppType = "web";
    //    @JsonProperty(value = "register_client_response_types")
    private String registerClientResponesType = "code";
    @JsonProperty(value = "localhost_only")
    private Boolean localhostOnly;
    @JsonProperty(value = "use_client_authentication_for_pat")
    private Boolean useClientAuthenticationForPat = true;
    @JsonProperty(value = "use_client_authentication_for_aat")
    private Boolean useClientAuthenticationForAat = true;
    @JsonProperty(value = "trust_all_certs")
    private Boolean trustAllCerts;
    @JsonProperty(value = "trust_store_path")
    private String keyStorePath;
    @JsonProperty(value = "trust_store_password")
    private String keyStorePassword;
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

    public int getStateExpirationInMinutes() {
        return stateExpirationInMinutes;
    }

    public void setStateExpirationInMinutes(int stateExpirationInMinutes) {
        this.stateExpirationInMinutes = stateExpirationInMinutes;
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

    public Boolean getUseClientAuthenticationForAat() {
        return useClientAuthenticationForAat;
    }

    public void setUseClientAuthenticationForAat(Boolean useClientAuthenticationForAat) {
        this.useClientAuthenticationForAat = useClientAuthenticationForAat;
    }

    public Boolean getLocalhostOnly() {
        return localhostOnly;
    }

    public void setLocalhostOnly(Boolean p_localhostOnly) {
        localhostOnly = p_localhostOnly;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeOutInSeconds() {
        return timeOutInSeconds;
    }

    public void setTimeOutInSeconds(int timeOutInSeconds) {
        this.timeOutInSeconds = timeOutInSeconds;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration");
        sb.append("{port=").append(port);
        sb.append(", timeOutInSeconds=").append(timeOutInSeconds);
        sb.append(", localhostOnly=").append(localhostOnly);
        sb.append(", useClientAuthenticationForPat=").append(useClientAuthenticationForPat);
        sb.append(", useClientAuthenticationForAat=").append(useClientAuthenticationForAat);
        sb.append(", trustAllCerts=").append(trustAllCerts);
        sb.append(", keyStorePath='").append(keyStorePath).append('\'');
        sb.append(", keyStorePassword='").append(keyStorePassword).append('\'');
        sb.append(", licenseId='").append(licenseId).append('\'');
        sb.append(", publicKey='").append(publicKey).append('\'');
        sb.append(", publicPassword='").append(publicPassword).append('\'');
        sb.append(", licensePassword='").append(licensePassword).append('\'');
        sb.append(", supportGoogleLogout='").append(supportGoogleLogout).append('\'');
        sb.append(", stateExpirationInMinutes='").append(stateExpirationInMinutes).append('\'');
        sb.append(", nonceExpirationInMinutes='").append(nonceExpirationInMinutes).append('\'');
        sb.append(", keyExpirationInMinutes='").append(publicOpKeyCacheExpirationInMinutes).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
