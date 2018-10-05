package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ConfigurationService implements Provider<Configuration> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    /**
     * oxD configuration property name
     */
    public static final String CONF_SYS_PROPERTY_NAME = "oxd.server.config";

    public static final String TEST_FILE_NAME = "oxd-conf-test.json";

    public static final String DEFAULT_SITE_CONFIG_JSON = "oxd-default-site-config.json";

    /**
     * Configuration file name.
     */
    private static final String FILE_NAME = Utils.isTestMode() ? TEST_FILE_NAME : "oxd-conf.json";

    private Configuration configuration = null;
    private Rp defaultRp = null;

    public File getConfDirectoryFile() {
        final String path = getConfDirectoryPath();
        LOG.info("Configuration directory: " + path);
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

            final String workingDirectory = System.getProperty("user.dir");
            return workingDirectory + File.separator + ".." + File.separator + "conf";
//            throw new RuntimeException("System property " + CONF_SYS_PROPERTY_NAME + " must point to valid directory path and must contain "
//                    + FILE_NAME + " file. Current value: " + confFilePath, e);
        }
    }

    public void load() {
        loadImpl();
        Preconditions.checkNotNull(configuration, "Failed to load configuration.");
    }

    private void loadImpl() {
        // 1. try system property "oxd.server.config"
        configuration = tryToLoadFromSysProperty(ConfigurationService.CONF_SYS_PROPERTY_NAME);
        if (configuration != null) {
            LOG.trace("Configuration loaded successfully from system property: {}.", ConfigurationService.CONF_SYS_PROPERTY_NAME);
            LOG.trace("Configuration: {}", configuration);
            loadDefaultRp(System.getProperty(ConfigurationService.CONF_SYS_PROPERTY_NAME));
            return;
        }

        // 2. catalina.base
        String property = System.getProperty("catalina.base") + File.separator + "conf" + File.separator + ConfigurationService.FILE_NAME;
        configuration = tryToLoadFromSysProperty(property);
        if (configuration != null) {
            LOG.trace("Configuration loaded successfully from system property: {}.", property);
            LOG.trace("Configuration: {}", configuration);
            loadDefaultRp(property);
            return;
        }

        // 2. catalina.home
        property = System.getProperty("catalina.home") + File.separator + "conf" + File.separator + ConfigurationService.FILE_NAME;
        configuration = tryToLoadFromSysProperty(property);
        if (configuration != null) {
            LOG.trace("Configuration loaded successfully from system property: {}.", property);
            LOG.trace("Configuration: {}", configuration);
            loadDefaultRp(property);
            return;
        }


        final InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(ConfigurationService.FILE_NAME);
        final Configuration configuration = createConfiguration(stream);
        if (configuration != null) {
            LOG.trace("Configuration loaded successfully.");
            LOG.trace("Configuration: {}", configuration);
        }
    }

    private void loadDefaultRp(String propertyName) {
        try {
            String configurationFolder = new File(propertyName).getParent();
            File file = new File(configurationFolder + File.separator + DEFAULT_SITE_CONFIG_JSON);
            LOG.trace("Trying to read " + DEFAULT_SITE_CONFIG_JSON + ", path: " + file.getAbsolutePath());
            if (file.exists()) {
                defaultRp = MigrationService.parseRp(file);
                LOG.info("Default RP configuration loaded successfully: " + defaultRp);
            } else {
                LOG.error("File does not exist at path: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.error("Failed to read " + DEFAULT_SITE_CONFIG_JSON, e);
        }
    }

    public Rp defaultRp() {
        if (defaultRp == null) {
            LOG.error("Failed to load fallback configuration!");
            defaultRp = new Rp();
        }
        return defaultRp;
    }

    private static Configuration tryToLoadFromSysProperty(String propertyName) {
        final String confProperty = System.getProperty(propertyName);
        if (StringUtils.isNotBlank(confProperty)) {
            LOG.trace("Try to load configuration from system property: {}, value: {}", propertyName, confProperty);
            FileInputStream fis = null;
            try {
                final File f = new File(confProperty);
                if (f.exists()) {
                    fis = new FileInputStream(f);
                    return createConfiguration(fis);
                } else {
                    LOG.info("Failed to load configuration from system property because such file does not exist. Value: {}", confProperty);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        return null;
    }

    public static Configuration createConfiguration(InputStream p_stream) {
        try {
            return CoreUtils.createJsonMapper().readValue(p_stream, Configuration.class);
        } catch (Exception e) {
            LOG.error("Failed to parse oxd configuration.", e);
            return null;
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Configuration get() {
        return configuration;
    }
}
