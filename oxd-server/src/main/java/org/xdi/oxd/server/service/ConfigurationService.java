package org.xdi.oxd.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.server.Utils;

import java.io.File;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/09/2015
 */

public class ConfigurationService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    /**
     * oxD configuration property name
     */
    public static final String CONF_SYS_PROPERTY_NAME = "oxd.server.config";

    /**
     * Configuration file name.
     */
    public static final String FILE_NAME = Utils.isTestMode() ? "oxd-conf-test.json" : "oxd-conf.json";

    public File getConfDirectoryFile() {
        final String path = getConfDirectoryPath();
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return file;
        } else {
            throw new RuntimeException("Failed to find conf directory, path: " + path);
        }
    }

    public String getConfDirectoryPath() {
        String confFilePath = System.getProperty(CONF_SYS_PROPERTY_NAME);
        try {
            return confFilePath.substring(0, confFilePath.lastIndexOf(File.separator));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("System property " + CONF_SYS_PROPERTY_NAME + " must point to valid directory path and must contain "
                    + FILE_NAME + " file. Current value: " + confFilePath, e);
        }
    }
}
