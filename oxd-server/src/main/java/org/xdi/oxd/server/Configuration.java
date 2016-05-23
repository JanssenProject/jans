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
    private Boolean useClientAuthenticationForPat;
    @JsonProperty(value = "trust_all_certs")
    private Boolean trustAllCerts;
    @JsonProperty(value = "trust_store_path")
    private String keyStorePath;
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
    @JsonProperty(value = "license_check_period_in_hours")
    private Integer licenseCheckPeriodInHours = 24;

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

    public Integer getLicenseCheckPeriodInHours() {
        return licenseCheckPeriodInHours;
    }

    public void setLicenseCheckPeriodInHours(Integer licenseCheckPeriodInHours) {
        this.licenseCheckPeriodInHours = licenseCheckPeriodInHours;
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

    public void setUseClientAuthenticationForPat(Boolean p_useClientAuthenticationForPat) {
        useClientAuthenticationForPat = p_useClientAuthenticationForPat;
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

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration");
        sb.append("{port=").append(port);
        sb.append(", timeOutInSeconds=").append(timeOutInSeconds);
        sb.append(", localhostOnly=").append(localhostOnly);
        sb.append(", licenseServerEndpoint=").append(licenseServerEndpoint);
        sb.append(", licenseId=").append(licenseId);
        sb.append('}');
        return sb.toString();
    }
}
