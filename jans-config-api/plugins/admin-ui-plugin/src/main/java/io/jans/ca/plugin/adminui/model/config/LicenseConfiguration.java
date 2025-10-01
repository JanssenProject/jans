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
}