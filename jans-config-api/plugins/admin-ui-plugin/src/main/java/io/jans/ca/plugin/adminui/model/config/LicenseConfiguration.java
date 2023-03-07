package io.jans.ca.plugin.adminui.model.config;

import jakarta.inject.Inject;
import org.slf4j.Logger;

public class LicenseConfiguration {

    @Inject
    Logger log;

    private String apiKey;
    private String productCode;
    private String sharedKey;
    private String hardwareId;
    private String licenseKey;

    public LicenseConfiguration() {
    }

    public LicenseConfiguration(String apiKey, String productCode, String sharedKey) {
        this.apiKey = apiKey;
        this.productCode = productCode;
        this.sharedKey = sharedKey;
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