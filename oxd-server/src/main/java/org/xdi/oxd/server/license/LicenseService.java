/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.Product;
import org.xdi.oxd.license.validator.LicenseContent;
import org.xdi.oxd.license.validator.LicenseValidator;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.ShutdownException;
import org.xdi.oxd.server.Utils;
import org.xdi.oxd.server.service.HttpService;
import org.xdi.oxd.server.service.TimeService;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    private final Configuration conf;
    private final LicenseFileUpdateService updateService;
    private final TimeService timeService;

    private volatile LicenseMetadata metadata = null;
    private volatile boolean licenseValid = false;

    @Inject
    public LicenseService(Configuration conf, HttpService httpService, TimeService timeService) {
        this.conf = conf;
        this.timeService = timeService;
        this.updateService = new LicenseFileUpdateService(conf, httpService);
    }

    public void start() {
        validateConfiguration();

        Optional<LicenseFile> licenseFile = LicenseFile.load();

        // before license update, check existing license and make sure autoupdate=true, otherwise skip update
        if (validateLicense() && metadata != null && metadata.getAutoupdate() != null &&
                !metadata.getAutoupdate()) {
            licenseValid = true;
            schedulePeriodicValidation(Utils.hoursDiff(new Date(), metadata.getExpirationDate()));
            return; // skip update procedure, autoupdate=false !
        }

        this.updateService.start(licenseFile);

        licenseValid = validateLicense();
        if (licenseValid) {
            schedulePeriodicValidation(1);
        } else {
            throw new ShutdownException("Failed to validate license, shutdown server ... ");
        }
    }

    private void validateConfiguration() {
        if (Strings.isNullOrEmpty(conf.getLicenseId())) {
            throw new ShutdownException("Unable to validate license. license_id is not set in oxd configuration.");
        }
        if (Strings.isNullOrEmpty(conf.getPublicKey())) {
            throw new ShutdownException("Unable to validate license. public_key is not set in oxd configuration.");
        }
        if (Strings.isNullOrEmpty(conf.getPublicPassword())) {
            throw new ShutdownException("Unable to validate license. public_password is not set in oxd configuration.");
        }
    }

    public LicenseMetadata getMetadata() {
        return metadata;
    }

    public boolean isLicenseValid() {
        return licenseValid && !updateService.isRetryLimitExceeded();
    }

    private boolean validateLicense() {
        try {
            LOG.trace("Validating license ...");

            metadata = null;
            licenseValid = false;

            final Optional<LicenseFile> licenseFile = LicenseFile.load();
            if (!licenseFile.isPresent() || Strings.isNullOrEmpty(licenseFile.get().getEncodedLicense())) {
                LOG.error("Failed to load license file : " + LicenseFile.getLicenseFile().getAbsolutePath());
                return false;
            }

            LicenseContent licenseContent = LicenseValidator.validate(
                    conf.getPublicKey(),
                    conf.getPublicPassword(),
                    conf.getLicensePassword(),
                    licenseFile.get().getEncodedLicense(),
                    Product.OXD,
                    timeService.getCurrentLicenseServerTime()
            );

            metadata = licenseContent.getMetadata();
            licenseValid = true;

            LOG.trace("License is validated successfully.");
            LOG.trace("License data: " + metadata);
            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

    private void schedulePeriodicValidation(int initialDelayInHours) {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(CoreUtils.daemonThreadFactory());
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                licenseValid = validateLicense();
            }
        }, initialDelayInHours, 24, TimeUnit.HOURS);
    }
}
