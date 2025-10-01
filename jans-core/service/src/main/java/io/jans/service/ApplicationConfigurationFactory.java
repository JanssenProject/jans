package io.jans.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import io.jans.exception.ConfigurationException;
import io.jans.util.properties.FileConfiguration;
import jakarta.inject.Inject;

/**
 * Base Configuration factory for all applications
 *
 * @author Yuriy Movchan
 * @version 0.1, 12/15/2023
 */
public abstract class ApplicationConfigurationFactory {

    @Inject
    private Logger log;

    private AtomicBoolean isCreated = new AtomicBoolean(false);

    public void create() {
        if (this.isCreated.get()) {
            return;
        }

        if (!this.isCreated.compareAndSet(false, true)) {
            return;
        }
        
        try {
	        if (!createFromDB(true)) {
	            log.error("Failed to load configuration from DB. Please fix it!!!.");
	            throw new ConfigurationException("Failed to load configuration from DB.");
	        } else {
	            log.info("Configuration loaded successfully.");
	        }
        } finally {
        	this.isCreated.set(true);
        }
    }

	protected abstract boolean createFromDB(boolean recoverFromFiles);

	public void initTimer() {}

	public abstract FileConfiguration getBaseConfiguration();
}
