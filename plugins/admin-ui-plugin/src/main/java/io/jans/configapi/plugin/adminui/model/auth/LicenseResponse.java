package io.jans.configapi.plugin.adminui.model.auth;

public class LicenseResponse {
    private boolean isLicenseEnable;
    private String productName;
    private String productCode;
    private String licenseType;
    private int maxActivations;
    private String licenseKey;
    private boolean isLicenseActive;
    private String validityPeriod;
    private String companyName;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;

    public boolean isLicenseEnable() {
        return isLicenseEnable;
    }

    public void setIsLicenseEnable(boolean isLicenseEnable) {
        this.isLicenseEnable = isLicenseEnable;
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

    public boolean isLicenseActive() {
        return isLicenseActive;
    }

    public void setLicenseActive(boolean licenseActive) {
        isLicenseActive = licenseActive;
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
}
