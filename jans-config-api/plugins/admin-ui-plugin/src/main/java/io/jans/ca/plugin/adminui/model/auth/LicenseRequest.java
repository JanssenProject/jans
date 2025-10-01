package io.jans.ca.plugin.adminui.model.auth;

public class LicenseRequest {
    private String licenseKey;

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    @Override
    public String toString() {
        return "LicenseRequest{" +
                "licenseKey='" + licenseKey + '\'' +
                '}';
    }
}