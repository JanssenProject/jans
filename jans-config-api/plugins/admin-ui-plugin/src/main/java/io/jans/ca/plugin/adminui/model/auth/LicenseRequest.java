package io.jans.ca.plugin.adminui.model.auth;

public class LicenseRequest {
    private String licenseKey;
    private Integer maxActivations;

    public Integer getMaxActivations() {
        return maxActivations;
    }

    public void setMaxActivations(Integer maxActivations) {
        this.maxActivations = maxActivations;
    }

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
                ", maxActivations=" + maxActivations +
                '}';
    }
}