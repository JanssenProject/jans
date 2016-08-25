/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import net.nicholaswilliams.java.licensing.SignedLicense;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.lib.ALicense;
import org.xdi.oxd.license.client.lib.ALicenseManager;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.service.HttpService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private boolean licenseChanged = false;
    private String encodedLicense = null;
    private LicenseMetadata metadata = null;

    private volatile boolean licenseValid = false;

    @Inject
    public LicenseService(Configuration conf, HttpService httpService) {
        this.conf = conf;
        this.updateService = new LicenseUpdateService(conf, httpService);
        this.updateService.start();

        licenseValid = validate();
        if (licenseValid) {
            schedulePeriodicValidation();
        }
    }

    public LicenseMetadata getMetadata() {
        return metadata;
    }

    public boolean isLicenseValid() {
        return licenseValid;
    }

    private boolean validate() {
        LOG.debug("licenseChanged: " + licenseChanged);
        try {
            final LicenseFile licenseFile = loadLicenseFile();
            if (licenseFile == null || Strings.isNullOrEmpty(licenseFile.getEncodedLicense())) {
                reset();
                return false;
            }

            // state
            if (!licenseChanged) {
                licenseChanged = encodedLicense != null && !licenseFile.getEncodedLicense().equals(encodedLicense);
                if (licenseChanged) {
                    LOG.debug("License was changed!");
                }
            }
            encodedLicense = licenseFile.getEncodedLicense();

            // validation
            final SignedLicense signedLicense = LicenseSerializationUtilities.deserialize(licenseFile.getEncodedLicense());
            ALicenseManager manager = new ALicenseManager(conf.getPublicKey(), conf.getPublicPassword(), signedLicense, conf.getLicensePassword());

            ALicense decryptedLicense = manager.decryptAndVerifyLicense(signedLicense);// DECRYPT signed license
            manager.validateLicense(decryptedLicense);
            LOG.trace("License is valid!");

            final String subject = decryptedLicense.getSubject();
            metadata = Jackson.createJsonMapper().readValue(subject, LicenseMetadata.class);

            LOG.trace("License metadata: " + metadata);

            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.debug("Unable to validate license, defaults to FREE license.");
        reset();
        return false;
    }

    public void reset() {
        metadata = null;
        licenseChanged = false;
        licenseValid = false;
    }

    private void schedulePeriodicValidation() {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(CoreUtils.daemonThreadFactory());
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                licenseValid = validate();
            }
        }, 1, 24, TimeUnit.HOURS);
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

    public boolean isLicenseChanged() {
        return licenseChanged;
    }
}
