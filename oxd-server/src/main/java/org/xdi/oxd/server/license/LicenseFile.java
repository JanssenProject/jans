/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.Jackson;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseFile implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseFile.class);

    public LicenseFile() {
    }

    public LicenseFile(String encodedLicense) {
        this.encodedLicense = encodedLicense;
    }

    @JsonProperty(value = "encoded_license")
    private String encodedLicense;

    public String getEncodedLicense() {
        return encodedLicense;
    }

    public LicenseFile setEncodedLicense(String encodedLicense) {
        this.encodedLicense = encodedLicense;
        return this;
    }

    public static LicenseFile create(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, LicenseFile.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public String asJson() {
        return Jackson.asJsonSilently(this);
    }
}
