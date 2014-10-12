package org.xdi.oxd.server.license;

import net.nicholaswilliams.java.licensing.SignedLicense;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.LicenseType;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.ALicenseManager;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;
import org.xdi.oxd.server.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    public static final String LICENSE_FILE_NAME = ".oxd-license";

    public static final String LICENSE_FILE_PATH = /*System.getProperty("user.home") +*/ LICENSE_FILE_NAME;

    private final Configuration conf;
    private final LicenseUpdateService updateService;

    private final LicenseFile licenseFile;
    private int threadsCount = 1;
    private LicenseType licenseType = LicenseType.FREE;
    private boolean isMultiServer = false;

    public LicenseService(Configuration conf) {
        this.conf = conf;
        this.updateService = new LicenseUpdateService(conf);
        this.updateService.start();

        this.licenseFile = loadLicenseFile();
        if (licenseFile != null) {
            validate();
        }
    }

    private void validate() {
        try {
            final SignedLicense signedLicense = LicenseSerializationUtilities.deserialize(licenseFile.getEncodedLicense());
            ALicenseManager manager = new ALicenseManager(conf.getPublicKey(), conf.getPublicPassword(), signedLicense, conf.getLicensePassword());

            ALicense decryptedLicense = manager.decryptAndVerifyLicense(signedLicense);// DECRYPT signed license
            manager.validateLicense(decryptedLicense);
            LOG.trace("License is valid!");

            final String subject = decryptedLicense.getSubject();
            final LicenseMetadata metadata = Jackson.createJsonMapper().readValue(subject, LicenseMetadata.class);

            LOG.trace("License metadata: " + metadata);

            threadsCount = metadata.getThreadsCount();
            licenseType = metadata.getLicenseType();
            isMultiServer = metadata.isMultiServer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private LicenseFile loadLicenseFile() {
        InputStream inputStream = null;
        try {
            File file = getLicenseFile();
            inputStream = new FileInputStream(file);
            return LicenseFile.create(inputStream);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
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


    public int getThreadsCount() {
        return threadsCount;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public boolean isMultiServer() {
        return isMultiServer;
    }
}
