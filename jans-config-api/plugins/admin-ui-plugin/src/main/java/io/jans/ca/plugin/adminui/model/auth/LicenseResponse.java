package io.jans.ca.plugin.adminui.model.auth;

public class LicenseResponse {
    private boolean licenseEnabled;
    private String productName;
    private String licenseType;
    private int maxActivations;
    private String licenseKey;
    private boolean licenseActive;
    private String validityPeriod;
    private Long licenseMAUThreshold;

    private boolean licenseExpired;

    public boolean isLicenseEnabled() {
        return licenseEnabled;
    }

    public void setLicenseEnabled(boolean licenseEnabled) {
        this.licenseEnabled = licenseEnabled;
    }

    public boolean isLicenseActive() {
        return licenseActive;
    }

    public void setLicenseActive(boolean licenseActive) {
        this.licenseActive = licenseActive;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public int getMaxActivations() {
        return maxActivations;
    }

    public void setMaxActivations(int maxActivations) {
        this.maxActivations = maxActivations;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public boolean getLicenseExpired() {
        return licenseExpired;
    }

    public void setLicenseExpired(boolean licenseExpired) {
        this.licenseExpired = licenseExpired;
    }

    public boolean isLicenseExpired() {
        return licenseExpired;
    }

    public Long getLicenseMAUThreshold() {
        return licenseMAUThreshold;
    }

    public void setLicenseMAUThreshold(Long licenseMAUThreshold) {
        this.licenseMAUThreshold = licenseMAUThreshold;
    }

    @Override
    public String toString() {
        return "LicenseResponse{" +
                "licenseEnabled=" + licenseEnabled +
                ", productName='" + productName + '\'' +
                ", licenseType='" + licenseType + '\'' +
                ", maxActivations=" + maxActivations +
                ", licenseKey='" + licenseKey + '\'' +
                ", licenseActive=" + licenseActive +
                ", validityPeriod='" + validityPeriod + '\'' +
                ", licenseExpired='" + licenseExpired + '\'' +
                '}';
    }
}