package io.jans.casa.plugins.strongauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by jgomer on 2018-04-18.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Browser {

    private String name;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
