package org.xdi.oxd.server;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseUpdateService.class);

    private final Configuration conf;

    public LicenseUpdateService(Configuration conf) {
        this.conf = conf;
    }

    public void start() {
        if (hasLicenseConfig()) {
            startUpdate();
        } else {
            LOG.error("Failed to start LicenseUpdateService. Configuration licenseId or licenseServerEndpoint is empty.");
        }
    }

    private void startUpdate() {
        // todo
    }

    public boolean hasLicenseConfig() {
        return !Strings.isNullOrEmpty(conf.getLicenseId()) && !Strings.isNullOrEmpty(conf.getLicenseServerEndpoint());
    }
}
