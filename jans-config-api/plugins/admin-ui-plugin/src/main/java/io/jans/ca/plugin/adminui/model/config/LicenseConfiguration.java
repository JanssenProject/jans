package io.jans.ca.plugin.adminui.model.config;

import jakarta.inject.Inject;
import org.slf4j.Logger;

public class LicenseConfiguration {

    @Inject
    Logger log;

    private String hardwareId;
    private String licenseKey;
    private String scanApiHostname;
    private String scanAuthServerHostname;
    private String scanApiClientId;
    private String scanApiClientSecret;

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getScanApiHostname() {
        return scanApiHostname;
    }

    public void setScanApiHostname(String scanApiHostname) {
        this.scanApiHostname = scanApiHostname;
    }

    public String getScanApiClientId() {
        return scanApiClientId;
    }

    public void setScanApiClientId(String scanApiClientId) {
        this.scanApiClientId = scanApiClientId;
    }

    public String getScanApiClientSecret() {
        return scanApiClientSecret;
    }

    public void setScanApiClientSecret(String scanApiClientSecret) {
        this.scanApiClientSecret = scanApiClientSecret;
    }

    public String getScanAuthServerHostname() {
        return scanAuthServerHostname;
    }

    public void setScanAuthServerHostname(String scanAuthServerHostname) {
        this.scanAuthServerHostname = scanAuthServerHostname;
    }
}