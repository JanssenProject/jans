package org.xdi.oxd.license.client.data;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 07/09/2014
 */

@JsonPropertyOrder({"licenseResponse"})
public class LicenseResponse implements Serializable {

    public static final LicenseResponse EMPTY = new LicenseResponse();

    @JsonProperty(value = "license")
    private String encodedLicense;

    public LicenseResponse() {
    }

    public LicenseResponse(String license) {
        this.encodedLicense = license;
    }

    public String getEncodedLicense() {
        return encodedLicense;
    }

    public void setEncodedLicense(String encodedLicense) {
        this.encodedLicense = encodedLicense;
    }
}
