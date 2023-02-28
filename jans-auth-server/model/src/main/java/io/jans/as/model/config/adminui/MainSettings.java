package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MainSettings {

    private OIDCSettings oidcConfig;

    public OIDCSettings getOidcConfig() {
        return oidcConfig;
    }

    public void setOidcConfig(OIDCSettings oidcConfig) {
        this.oidcConfig = oidcConfig;
    }
}
