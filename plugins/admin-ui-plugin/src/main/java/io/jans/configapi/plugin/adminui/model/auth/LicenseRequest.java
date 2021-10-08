package io.jans.configapi.plugin.adminui.model.auth;

public class LicenseRequest {
    private String licenseKey;
    private String validityPeriod;
    private Integer maxActivations;
    private Boolean licenseActive;

    public String getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public Integer getMaxActivations() {
        return maxActivations;
    }

    public void setMaxActivations(Integer maxActivations) {
        this.maxActivations = maxActivations;
    }

    public Boolean getLicenseActive() {
        return licenseActive;
    }

    public void setLicenseActive(Boolean licenseActive) {
        this.licenseActive = licenseActive;
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
                ", validityPeriod='" + validityPeriod + '\'' +
                ", maxActivations=" + maxActivations +
                ", licenseActive=" + licenseActive +
                '}';
    }
}