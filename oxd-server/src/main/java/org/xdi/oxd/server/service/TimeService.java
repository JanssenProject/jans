package org.xdi.oxd.server.service;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.GenerateWS;
import org.xdi.oxd.license.client.LicenseClient;
import org.xdi.oxd.server.OxdServerConfiguration;
import org.xdi.oxd.server.license.LicenseFileUpdateService;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */

public class TimeService {

    private static final Logger LOG = LoggerFactory.getLogger(TimeService.class);

    private final OxdServerConfiguration conf;
    private final HttpService httpService;

    @Inject
    public TimeService(OxdServerConfiguration conf, HttpService httpService) {
        this.conf = conf;
        this.httpService = httpService;
    }

    public Date getCurrentLicenseServerTime() {
        Optional<Date> serverTime = currentLicenseServerTime();
        if (serverTime.isPresent()) {
            return serverTime.get();
        }
        return new Date(); // system time
    }

    private Optional<Date> currentLicenseServerTime() {
        try {
            final GenerateWS generateWS = LicenseClient.generateWs(LicenseFileUpdateService.LICENSE_SERVER_ENDPOINT, httpService.getClientExecutor());
            long millis = Long.parseLong(generateWS.currentServerTime());
            if (millis > 0) {
                return Optional.of(new Date(millis));
            }
        } catch (Exception e) {
            LOG.error("Failed to fetch license server time.", e);
        }
        return Optional.absent();
    }

}
