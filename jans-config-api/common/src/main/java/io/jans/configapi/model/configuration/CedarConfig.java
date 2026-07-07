package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.core.cedarling.model.CedarlingConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CedarConfig extends CedarlingConfiguration {

    @Schema(description = "Application Name.")
    private String applicationName;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String toString() {
        return "CedarConfig [applicationName=" + applicationName + "]";
    }

}
