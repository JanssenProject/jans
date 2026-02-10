package io.jans.casa.plugins.injiwallet;

import io.jans.casa.plugins.injiwallet.service.InjiWalletLinkingService;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main plugin class for Inji Wallet integration with Casa
 * Provides both authentication and registration capabilities using verifiable credentials
 */
public class InjiWalletPlugin extends Plugin {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public InjiWalletPlugin(PluginWrapper wrapper) throws Exception {
        super(wrapper);
        // Initialize service
        InjiWalletLinkingService.getInstance(wrapper.getPluginId());
    }

    @Override
    public void start() {
        logger.info("Inji Wallet plugin started");
    }

    @Override
    public void delete() {
        logger.info("Inji Wallet plugin stopped");
    }
    
}