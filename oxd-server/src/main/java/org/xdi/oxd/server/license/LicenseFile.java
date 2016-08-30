/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.Jackson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseFile implements Serializable {

    public static final String LICENSE_FILE_NAME = ".oxd-license";

    public static final String LICENSE_FILE_PATH = LICENSE_FILE_NAME;

    private static final Logger LOG = LoggerFactory.getLogger(LicenseFile.class);

    @JsonProperty(value = "encoded_license")
    private String encodedLicense;

    @JsonIgnore
    private long lastModified;

    public LicenseFile() {
    }

    public LicenseFile(String encodedLicense) {
        this.encodedLicense = encodedLicense;
    }

    public String getEncodedLicense() {
        return encodedLicense;
    }

    public void setEncodedLicense(String encodedLicense) {
        this.encodedLicense = encodedLicense;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    private static LicenseFile create(InputStream p_stream) {
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

    public static Optional<LicenseFile> load() {
        InputStream inputStream = null;
        try {
            File file = getLicenseFile();
            inputStream = new FileInputStream(file);
            LicenseFile licenseFile = LicenseFile.create(inputStream);
            licenseFile.setLastModified(file.lastModified());
            return Optional.of(licenseFile);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return Optional.absent();
    }

    public static File getLicenseFile() throws IOException {
        File file = new File(LICENSE_FILE_PATH);
        if (!file.exists()) {
            final boolean fileCreated = file.createNewFile();
            if (!fileCreated) {
                throw new RuntimeException("Failed to create license file, path:" + file.getAbsolutePath());
            }
        }
        LOG.debug("License file location: " + file.getAbsolutePath());
        return file;
    }

    public String asJson() {
        return Jackson.asJsonSilently(this);
    }
}
