package io.jans.ca.plugin.adminui.model.config;

import org.slf4j.Logger;

import jakarta.inject.Inject;

public class LicenseConfiguration {

    @Inject
    Logger log;

    private String apiKey;
    private String productCode;
    private String sharedKey;
    private String managementKey;
    private String hardwareId;
    private String licenseKey;

    public LicenseConfiguration() {
    }

    public LicenseConfiguration(String apiKey, String productCode, String sharedKey, String managementKey) {
        this.apiKey = apiKey;
        this.productCode = productCode;
        this.sharedKey = sharedKey;
        this.managementKey = managementKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public String getManagementKey() {
        return managementKey;
    }

    public void setManagementKey(String managementKey) {
        this.managementKey = managementKey;
    }

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
}