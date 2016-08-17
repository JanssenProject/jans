/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.license;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.license.client.GenerateWS;
import org.xdi.oxd.license.client.LicenseClient;
import org.xdi.oxd.license.client.data.LicenseResponse;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.service.HttpService;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseUpdateService.class);

    private final Configuration conf;
    private final HttpService httpService;

    public LicenseUpdateService(Configuration conf, HttpService httpService) {
        this.conf = conf;
        this.httpService = httpService;
    }

    public void start() {
        if (hasLicenseConfig()) {
            updateLicenseFromServer();
            scheduleUpdatePinger();
        } else {
            LOG.error("Failed to start LicenseUpdateService. Configuration licenseId or licenseServerEndpoint is empty.");
        }
    }

    private void scheduleUpdatePinger() {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(CoreUtils.daemonThreadFactory());
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateLicenseFromServer();
            }
        }, 1, conf.getLicenseCheckPeriodInHours(), TimeUnit.HOURS);
    }

    private void updateLicenseFromServer() {
        try {
            final GenerateWS generateWS = LicenseClient.generateWs(conf.getLicenseServerEndpoint(), httpService.getClientExecutor());

            final List<LicenseResponse> generatedLicenses = generateWS.generatePost(conf.getLicenseId());
            if (generatedLicenses != null && !generatedLicenses.isEmpty() && !Strings.isNullOrEmpty(generatedLicenses.get(0).getEncodedLicense())) {
                final File file = LicenseService.getLicenseFile();
                if (file != null) {
                    final String json = new LicenseFile(generatedLicenses.get(0).getEncodedLicense()).asJson();
                    FileUtils.write(file, json);
                    LOG.trace("License file updated successfully.");
                    return;
                }
            } else {
                LOG.trace("No license update on server:" + conf.getLicenseServerEndpoint() + ", licenseId: " + conf.getLicenseId());
                return;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.trace("Failed to update license file from server:" + conf.getLicenseServerEndpoint() + ", licenseId: " + conf.getLicenseId());
    }

    public boolean hasLicenseConfig() {
        return !Strings.isNullOrEmpty(conf.getLicenseId()) && !Strings.isNullOrEmpty(conf.getLicenseServerEndpoint());
    }
}
