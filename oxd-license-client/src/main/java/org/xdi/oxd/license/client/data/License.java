package org.xdi.oxd.license.client.data;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

@JsonPropertyOrder({ "license"})
public class License implements Serializable {

    @JsonProperty(value = "license")
    private String license;

    public License() {
    }

    public License(String license) {
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}
