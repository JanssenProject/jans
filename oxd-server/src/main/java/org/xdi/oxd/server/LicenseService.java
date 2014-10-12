package org.xdi.oxd.server;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2014
 */

public class LicenseService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    public static final String LICENSE_FILE_NAME = ".oxd-license";

    public static final String LICENSE_FILE_PATH = System.getProperty("user.home") + LICENSE_FILE_NAME;

    private final Configuration conf;
    private final LicenseUpdateService updateService;


    public LicenseService(Configuration conf) {
        this.conf = conf;
        this.updateService = new LicenseUpdateService(conf);
        this.updateService.start();

        loadLicense();
    }

    private void loadLicense() {
        try {
            File file = getLicenseFile();

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private File getLicenseFile() throws IOException {
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


    public int numberOfThreads() {
        return 1;
    }

    public boolean hasLicenseConfig() {
        return !Strings.isNullOrEmpty(conf.getLicenseId()) && !Strings.isNullOrEmpty(conf.getLicenseServerEndpoint());
    }
}
