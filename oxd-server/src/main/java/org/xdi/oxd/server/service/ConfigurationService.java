package org.xdi.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.OxdServerConfiguration;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ConfigurationService implements Provider<OxdServerConfiguration> {

    public static final String DOC_URL = "https://www.gluu.org/docs/oxd";

    public static final String APP_VERSION = "3.2.0";

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    private OxdServerConfiguration configuration = null;

    public void setConfiguration(OxdServerConfiguration configuration) {
        Preconditions.checkNotNull(configuration, "Failed to load configuration.");
        this.configuration = configuration;
    }

    public Rp defaultRp() {
        try {
            return CoreUtils.createJsonMapper().readValue(configuration.getDefaultSiteConfig().toString(), Rp.class);
        } catch (IOException e) {
            LOG.error("Failed to parse default RP.", e);
            return null;
        }
    }

    public OxdServerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public OxdServerConfiguration get() {
        return configuration;
    }
}
