package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseConfig {

    private String ssa;
    private String scanLicenseApiHostname;
    private String licenseKey;
    private String licenseHardwareKey;
    private String licenseValidUpto;
    private String licenseDetailsLastUpdatedOn;
    private String productCode;
    private String productName;
    private String licenseType;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String companyName;
    private Boolean licenseActive;
    private Boolean licenseExpired;
    private Long licenseMAUThreshold;
    private Long intervalForSyncLicenseDetailsInDays;

    private OIDCClientSettings oidcClient;

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

    public String getLicenseValidUpto() {
        return licenseValidUpto;
    }

    public void setLicenseValidUpto(String licenseValidUpto) {
        this.licenseValidUpto = licenseValidUpto;
    }

    public String getLicenseDetailsLastUpdatedOn() {
        return licenseDetailsLastUpdatedOn;
    }

    public void setLicenseDetailsLastUpdatedOn(String licenseDetailsLastUpdatedOn) {
        this.licenseDetailsLastUpdatedOn = licenseDetailsLastUpdatedOn;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Boolean getLicenseActive() {
        return licenseActive;
    }

    public void setLicenseActive(Boolean licenseActive) {
        this.licenseActive = licenseActive;
    }

    public Boolean getLicenseExpired() {
        return licenseExpired;
    }

    public void setLicenseExpired(Boolean licenseExpired) {
        this.licenseExpired = licenseExpired;
    }

    public Long getLicenseMAUThreshold() {
        return licenseMAUThreshold;
    }

    public void setLicenseMAUThreshold(Long licenseMAUThreshold) {
        this.licenseMAUThreshold = licenseMAUThreshold;
    }

    public Long getIntervalForSyncLicenseDetailsInDays() {
        return intervalForSyncLicenseDetailsInDays;
    }

    public void setIntervalForSyncLicenseDetailsInDays(Long intervalForSyncLicenseDetailsInDays) {
        this.intervalForSyncLicenseDetailsInDays = intervalForSyncLicenseDetailsInDays;
    }

    @Override
    public String toString() {
        return "LicenseConfig{" +
                "ssa='" + ssa + '\'' +
                ", scanLicenseApiHostname='" + scanLicenseApiHostname + '\'' +
                ", licenseKey='" + licenseKey + '\'' +
                ", licenseHardwareKey='" + licenseHardwareKey + '\'' +
                ", licenseValidUpto='" + licenseValidUpto + '\'' +
                ", licenseDetailsLastUpdatedOn='" + licenseDetailsLastUpdatedOn + '\'' +
                ", productCode='" + productCode + '\'' +
                ", productName='" + productName + '\'' +
                ", licenseType='" + licenseType + '\'' +
                ", customerFirstName='" + customerFirstName + '\'' +
                ", customerLastName='" + customerLastName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", companyName='" + companyName + '\'' +
                ", licenseActive=" + licenseActive +
                ", licenseExpired=" + licenseExpired +
                ", oidcClient=" + oidcClient +
                '}';
    }
}
