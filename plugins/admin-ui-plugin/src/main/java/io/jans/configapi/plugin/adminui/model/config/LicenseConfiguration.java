package io.jans.configapi.plugin.adminui.model.config;

import com.licensespring.LicenseManager;
import com.licensespring.LicenseSpringConfiguration;
import com.licensespring.model.exceptions.LicenseSpringException;
import org.slf4j.Logger;

import javax.inject.Inject;

public class LicenseConfiguration {

    @Inject
    Logger log;

    private String apiKey;
    private String productCode;
    private String sharedKey;
    private String managementKey;
    private Boolean enabled = Boolean.FALSE;
    LicenseSpringConfiguration licenseSpringConfiguration;
    LicenseManager licenseManager;

    public LicenseConfiguration() {
    }
    public LicenseConfiguration(String apiKey, String productCode, String sharedKey, String managementKey, Boolean enabled) {
        this.apiKey = apiKey;
        this.productCode = productCode;
        this.sharedKey = sharedKey;
        this.enabled = enabled;
        this.managementKey = managementKey;

        if(this.enabled) {
            initializeLicenseManager();
        }
    }

    public void initializeLicenseManager() {
        try {
            this.licenseSpringConfiguration = LicenseSpringConfiguration.builder()
                    .apiKey(apiKey)
                    .productCode(productCode)
                    .sharedKey(sharedKey)
                    .build();

            this.licenseManager = LicenseManager.getInstance();
            if (!licenseManager.isInitialized()) {
                licenseManager.initialize(licenseSpringConfiguration);
            }
        } catch (LicenseSpringException e) {
            log.error("Error in initializing LicenseManager. ", e);
            throw e;
        }
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LicenseSpringConfiguration getLicenseSpringConfiguration() {
        return licenseSpringConfiguration;
    }

    public void setLicenseSpringConfiguration(LicenseSpringConfiguration licenseSpringConfiguration) {
        this.licenseSpringConfiguration = licenseSpringConfiguration;
    }

    public LicenseManager getLicenseManager() {
        return licenseManager;
    }

    public void setLicenseManager(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    public String getManagementKey() {
        return managementKey;
    }

    public void setManagementKey(String managementKey) {
        this.managementKey = managementKey;
    }
}