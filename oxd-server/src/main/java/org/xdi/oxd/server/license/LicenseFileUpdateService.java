/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.GenerateWS;
import org.xdi.oxd.license.client.LicenseClient;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.server.OxdServerConfiguration;
import org.xdi.oxd.server.ServerLauncher;
import org.xdi.oxd.server.service.HttpService;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseFileUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseFileUpdateService.class);

    public static final String LICENSE_SERVER_ENDPOINT = "https://license.gluu.org/oxLicense";

    private static final int ONE_HOUR_AS_MILLIS = 3600000;
    private static final int _24_HOURS_AS_MILLIS = 24 * ONE_HOUR_AS_MILLIS;
    public static final int RETRY_LIMIT = 3;

    private final OxdServerConfiguration conf;
    private final HttpService httpService;
    private AtomicInteger retry = new AtomicInteger();

    LicenseFileUpdateService(OxdServerConfiguration conf, HttpService httpService) {
        this.conf = conf;
        this.httpService = httpService;
    }

    public void start(Optional<LicenseFile> licenseFile) {
        if (!licenseFile.isPresent() || !lastModifiedLessThan12HoursAgo(licenseFile.get().getLastModified())) {
            updateLicenseFromServer();
        }
        scheduleUpdatePinger();
    }

    private boolean lastModifiedLessThan12HoursAgo(long lastModified) {
        long diff = System.currentTimeMillis() - lastModified;
        return diff < _24_HOURS_AS_MILLIS;
    }

    private void scheduleUpdatePinger() {
        newExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateLicenseFromServer();
            }
        }, 24, 24, TimeUnit.HOURS);
    }

    private ScheduledExecutorService newExecutor() {
        return Executors.newSingleThreadScheduledExecutor(CoreUtils.daemonThreadFactory());
    }

    public void updateLicenseFromServer() {
        try {
            final GenerateWS generateWS = LicenseClient.generateWs(LICENSE_SERVER_ENDPOINT, httpService.getClientExecutor());

            final String macAddress = MacAddressProvider.macAddress();
            LOG.trace("Updating license, license_id: " + conf.getLicenseId() + ", retry: " + retry + " ... Mac address: " + macAddress);

            final List<LicenseResponse> generatedLicenses = generateWS.generatePost(conf.getLicenseId(), macAddress);
            if (generatedLicenses != null && !generatedLicenses.isEmpty() && !Strings.isNullOrEmpty(generatedLicenses.get(0).getEncodedLicense())) {
                final File file = LicenseFile.getLicenseFile();
                if (file != null) {
                    final String json = new LicenseFile(generatedLicenses.get(0).getEncodedLicense(), macAddress, conf.getLicenseId()).asJson();
                    FileUtils.write(file, json);

                    retry.set(0);
                    LOG.info("License file updated successfully. Mac address: " + macAddress);
                    return;
                }
            } else {
                retry.set(0);
                LOG.info("No license update, licenseId: " + conf.getLicenseId());
                return;
            }
        } catch (ClientResponseFailure e) {
            LOG.error(e.getMessage() + ", " + e.getResponse().getEntity(String.class), e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.trace("Failed to update license file by licenseId: " + conf.getLicenseId());

        retry.incrementAndGet();

        if (isRetryLimitExceeded()) {
            LicenseFile.deleteSilently();
            LOG.error("Shutdown server after trying to update license. Retry count: " + retry.get());
            ServerLauncher.shutdownDueToInvalidLicense();
        }

        newExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                updateLicenseFromServer();
            }
        }, 3, TimeUnit.HOURS);
    }

    public boolean isRetryLimitExceeded() {
        return retry.get() > RETRY_LIMIT;
    }


}
