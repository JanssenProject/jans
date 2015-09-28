package org.xdi.oxd.server.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/09/2015
 */

public class SiteConfigurationService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SiteConfigurationService.class);

    private volatile SiteConfiguration defaultSiteConfiguration;

    public SiteConfiguration defaultSiteConfiguration() {
        if (defaultSiteConfiguration == null) {
            defaultSiteConfiguration = read("oxd-default-site-config");
        }
        return defaultSiteConfiguration;
    }

    public SiteConfiguration read(String id) {
        return tryToLoad(id);
    }

    private static SiteConfiguration tryToLoad(String propertyName) {
        final String confProperty = System.getProperty(propertyName);
        if (StringUtils.isNotBlank(confProperty)) {
            LOG.trace("Try to load site configuration from system property: {}, value: {}", propertyName, confProperty);
            FileInputStream fis = null;
            try {
                final File f = new File(confProperty);
                if (f.exists()) {
                    fis = new FileInputStream(f);
                    return createConfiguration(fis);
                } else {
                    LOG.info("Failed to site load configuration from system property because such file does not exist. Value: {}", confProperty);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }

        return null;
    }

    public static SiteConfiguration createConfiguration(InputStream p_stream) {
        try {
            try {
                return CoreUtils.createJsonMapper().readValue(p_stream, SiteConfiguration.class);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

}
