package io.jans.ca.plugin.adminui.model.auth;

public class LicenseResponse {
    private boolean licenseEnabled;
    private String productName;
    private String productCode;
    private String licenseType;
    private int maxActivations;
    private String licenseKey;
    private boolean licenseActive;
    private String validityPeriod;
    private String companyName;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public boolean getLicenseExpired() {
        return licenseExpired;
    }

    public void setLicenseExpired(boolean licenseExpired) {
        this.licenseExpired = licenseExpired;
    }

    @Override
    public String toString() {
        return "LicenseResponse{" +
                "licenseEnabled=" + licenseEnabled +
                ", productName='" + productName + '\'' +
                ", productCode='" + productCode + '\'' +
                ", licenseType='" + licenseType + '\'' +
                ", maxActivations=" + maxActivations +
                ", licenseKey='" + licenseKey + '\'' +
                ", licenseActive=" + licenseActive +
                ", validityPeriod='" + validityPeriod + '\'' +
                ", companyName='" + companyName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerFirstName='" + customerFirstName + '\'' +
                ", customerLastName='" + customerLastName + '\'' +
                ", licenseExpired='" + licenseExpired + '\'' +
                '}';
    }
}