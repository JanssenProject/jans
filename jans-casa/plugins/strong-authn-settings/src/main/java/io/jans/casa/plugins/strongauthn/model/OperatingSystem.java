package io.jans.casa.plugins.strongauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by jgomer on 2018-04-18.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperatingSystem {

    private String family;
    private String version;

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
