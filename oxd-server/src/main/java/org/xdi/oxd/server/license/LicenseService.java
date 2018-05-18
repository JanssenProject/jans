/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.LicenseClient;
import org.xdi.oxd.license.client.js.AppMetadata;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.Product;
import org.xdi.oxd.license.client.js.StatisticUpdateRequest;
import org.xdi.oxd.license.validator.LicenseContent;
import org.xdi.oxd.license.validator.LicenseValidator;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.ServerLauncher;
import org.xdi.oxd.server.ShutdownException;
import org.xdi.oxd.server.Utils;
import org.xdi.oxd.server.service.HttpService;
import org.xdi.oxd.server.service.Rp;
import org.xdi.oxd.server.service.TimeService;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Properties;
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
    private final HttpService httpService;

    private volatile LicenseMetadata metadata = null;
    private volatile boolean licenseValid = false;

    private final Cache<String, Rp> clientUpdateCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Inject
    public LicenseService(Configuration conf, HttpService httpService, TimeService timeService) {
        this.conf = conf;
        this.timeService = timeService;
        this.updateService = new LicenseFileUpdateService(conf, httpService);
        this.httpService = httpService;
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

            Optional<LicenseFile> licenseFile = LicenseFile.load();
            if (!licenseFile.isPresent() || Strings.isNullOrEmpty(licenseFile.get().getEncodedLicense())) {
                LOG.error("Failed to load license file : " + LicenseFile.getLicenseFile().getAbsolutePath());
                return false;
            }

            if (StringUtils.isBlank(licenseFile.get().getLicenseId()) || !licenseFile.get().getLicenseId().equals(conf.getLicenseId())) {
                LOG.info(String.format("Deleting license file ... license id in file (%s) does not match license id from oxd-conf.json (%s)", licenseFile.get().getLicenseId(), conf.getLicenseId()));
                LicenseFile.deleteContent();
                this.updateService.updateLicenseFromServer();
                licenseFile = LicenseFile.load();
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

    public void notifyClientUsed(final Rp rp, final boolean isClientLocal) {
        CoreUtils.createExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (shouldNotifyAboutClientUsage(rp)) {
                    notifyClientUsedImpl(rp, isClientLocal);
                }
            }
        });
    }

    private boolean shouldNotifyAboutClientUsage(Rp rp) {
        boolean hasInCache = clientUpdateCache.getIfPresent(rp.getClientId()) != null;
        if (hasInCache) {
            return false; // skip update, client was updated already
        }
        clientUpdateCache.put(rp.getClientId(), rp);
        return true;
    }

    private void notifyClientUsedImpl(Rp rp, boolean isClientLocal) {
        try {
            String licenseId = conf.getLicenseId();
            String clientId = rp.getClientId();
            String oxdId = rp.getOxdId();
            String clientName = rp.getClientName();
            String macAddress = MacAddressProvider.macAddress();

            StatisticUpdateRequest request = StatisticUpdateRequest.clientUpdate(
                    licenseId, clientId, oxdId, clientName, macAddress, isClientLocal);
            request.setAppMetadata(appMetadata(rp.getOxdRpProgrammingLanguage(), conf.getServerName()));
            LOG.trace("Updating statistic ... , request: " + request);
            Response response = LicenseClient.statisticWs(LicenseFileUpdateService.LICENSE_SERVER_ENDPOINT, httpService.getClientExecutor()).update(request);
            if (response.getStatus() == 200) {
                LOG.trace("Updated statistic. oxdId: " + oxdId + ", response: " + response);
            } else {
                throw new RuntimeException("Failed to update statistic, rp: " + rp);
            }
        } catch (Exception e) {
            LOG.error("Failed to update statistic. Message: " + e.getMessage(), e);
        }
    }

    private static AppMetadata appMetadata(String programmingLanguage, String serverName) {
        AppMetadata appMetadata = new AppMetadata();
        appMetadata.setAppName("oxd");
        appMetadata.setAppVersion("3.2.0");
        appMetadata.setProgrammingLanguage(programmingLanguage);

        Properties buildProperties = ServerLauncher.buildProperties();
        if (buildProperties != null) {
            for (String key : buildProperties.stringPropertyNames()) {
                appMetadata.getData().put(key, buildProperties.getProperty(key));
            }
        }
        appMetadata.getData().put("server_name", serverName);

        return appMetadata;
    }
}
