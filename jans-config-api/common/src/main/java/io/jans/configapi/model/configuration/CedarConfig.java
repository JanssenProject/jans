package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CedarConfig {

    @Schema(description = "Application Name.")
    private String applicationName;

    @Schema(description = "List of attributes that are optional.")
    private String policyStoreFilename;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getPolicyStoreFilename() {
        return policyStoreFilename;
    }

    public void setPolicyStoreFilename(String policyStoreFilename) {
        this.policyStoreFilename = policyStoreFilename;
    }

    @Override
    public String toString() {
        return "CedarConfig [applicationName=" + applicationName + ", policyStoreFilename=" + policyStoreFilename + "]";
    }

}
