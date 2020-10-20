package io.jans.ca.server.service;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import io.jans.ca.common.Jackson2;
import io.jans.ca.server.RpServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ConfigurationService implements Provider<RpServerConfiguration> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    private RpServerConfiguration configuration = null;

    public void setConfiguration(RpServerConfiguration configuration) {
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

    public RpServerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public RpServerConfiguration get() {
        return configuration;
    }
}
