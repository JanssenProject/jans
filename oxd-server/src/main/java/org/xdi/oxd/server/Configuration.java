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
    @JsonProperty(value = "jetty_port")
    private int jettyPort;
    @JsonProperty(value = "start_jetty")
    private boolean startJetty;
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
    @JsonProperty(value = "license_server_endpoint")
    private String licenseServerEndpoint;
    @JsonProperty(value = "license_id")
    private String licenseId;
    @JsonProperty(value = "public_key")
    private String publicKey;
    @JsonProperty(value = "public_password")
    private String publicPassword;
    @JsonProperty(value = "license_password")
    private String licensePassword;

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public int getJettyPort() {
        return jettyPort;
    }

    public void setJettyPort(int jettyPort) {
        this.jettyPort = jettyPort;
    }

    public boolean isStartJetty() {
        return startJetty;
    }

    public void setStartJetty(boolean startJetty) {
        this.startJetty = startJetty;
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

    public String getLicenseServerEndpoint() {
        return licenseServerEndpoint;
    }

    public void setLicenseServerEndpoint(String licenseServerEndpoint) {
        this.licenseServerEndpoint = licenseServerEndpoint;
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
        sb.append(", jettyPort=").append(jettyPort);
        sb.append(", startJetty=").append(startJetty);
        sb.append(", localhostOnly=").append(localhostOnly);
        sb.append(", useClientAuthenticationForPat=").append(useClientAuthenticationForPat);
        sb.append(", useClientAuthenticationForAat=").append(useClientAuthenticationForAat);
        sb.append(", trustAllCerts=").append(trustAllCerts);
        sb.append(", keyStorePath='").append(keyStorePath).append('\'');
        sb.append(", keyStorePassword='").append(keyStorePassword).append('\'');
        sb.append(", licenseServerEndpoint='").append(licenseServerEndpoint).append('\'');
        sb.append(", licenseId='").append(licenseId).append('\'');
        sb.append(", publicKey='").append(publicKey).append('\'');
        sb.append(", publicPassword='").append(publicPassword).append('\'');
        sb.append(", licensePassword='").append(licensePassword).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
