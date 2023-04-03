package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseConfig {

    private String ssa;
    private String scanLicenseApiHostname;
    private String licenseKey;
    private String licenseHardwareKey;
    private CredentialsEncryptionKeyDetails credentialsEncryptionKey;
    private OIDCClientSettings oidcClient;

    public CredentialsEncryptionKeyDetails getCredentialsEncryptionKey() {
        return credentialsEncryptionKey;
    }

    public void setCredentialsEncryptionKey(CredentialsEncryptionKeyDetails credentialsEncryptionKey) {
        this.credentialsEncryptionKey = credentialsEncryptionKey;
    }

    public OIDCClientSettings getOidcClient() {
        return oidcClient;
    }

    public void setOidcClient(OIDCClientSettings oidcClient) {
        this.oidcClient = oidcClient;
    }

    public String getScanLicenseApiHostname() {
        return scanLicenseApiHostname;
    }

    public void setScanLicenseApiHostname(String scanLicenseApiHostname) {
        this.scanLicenseApiHostname = scanLicenseApiHostname;
    }

    public String getLicenseHardwareKey() {
        return licenseHardwareKey;
    }

    public void setLicenseHardwareKey(String licenseHardwareKey) {
        this.licenseHardwareKey = licenseHardwareKey;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getSsa() {
        return ssa;
    }

    public void setSsa(String ssa) {
        this.ssa = ssa;
    }

    @Override
    public String toString() {
        return "LicenseConfig{" +
                "scanLicenseApiHostname='" + scanLicenseApiHostname + '\'' +
                ", licenseKey='" + licenseKey + '\'' +
                ", ssa='" + ssa + '\'' +
                ", licenseHardwareKey='" + licenseHardwareKey + '\'' +
                ", credentialsEncryptionKey=" + credentialsEncryptionKey.toString() +
                ", oidcClient=" + oidcClient.toString() +
                '}';
    }
}
