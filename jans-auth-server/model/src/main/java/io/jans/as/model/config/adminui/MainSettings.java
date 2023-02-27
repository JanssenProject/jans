package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MainSettings {

    private OIDCSettings oidcConfig;
    private LicenseConfig licenseConfig;

    public OIDCSettings getOidcConfig() {
        return oidcConfig;
    }

    public void setOidcConfig(OIDCSettings oidcConfig) {
        this.oidcConfig = oidcConfig;
    }

    public LicenseConfig getLicenseConfig() {
        return licenseConfig;
    }

    public void setLicenseConfig(LicenseConfig licenseConfig) {
        this.licenseConfig = licenseConfig;
    }
}
