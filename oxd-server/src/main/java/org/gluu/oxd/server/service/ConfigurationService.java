package org.gluu.oxd.server.service;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import org.gluu.oxd.common.Jackson2;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ConfigurationService implements Provider<OxdServerConfiguration> {

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
            return Jackson2.createJsonMapper().readValue(configuration.getDefaultSiteConfig().toString(), Rp.class);
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
