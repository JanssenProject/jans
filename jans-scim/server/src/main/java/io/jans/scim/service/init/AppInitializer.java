/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.init;

import java.util.Arrays;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.exception.ConfigurationException;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.scim.service.ApplicationFactory;
import io.jans.scim.service.ConfigurationFactory;
import io.jans.scim.service.logger.LoggerService;
import io.jans.service.PythonService;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

@ApplicationScoped
public class AppInitializer {

    private static final int RETRIES = 15;
    private static final int RETRY_INTERVAL = 15;
    private static final String DEFAULT_CONF_BASE = "/etc/jans/conf";

    @Inject
    private Logger logger;

    @Inject
    private StringEncrypter stringEncrypter;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    private ConfigurationFactory configurationFactory;

	@Inject
	private PythonService pythonService;

    @Inject
    private LoggerService loggerService;

	@Inject
	private CustomScriptManager customScriptManager;
	
	@Inject
	PersistenceConfiguration persistenceConfiguration;
    
    //@Inject
    //private ExternalScimService externalScimService;

    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {

        logger.info("SCIM service initializing...");
        SecurityProviderUtility.installBCProvider();

        configurationFactory.create();
		pythonService.initPythonInterpreter(configurationFactory.getBaseConfiguration().getString("pythonModulesDir", null));
        quartzSchedulerManager.start();
		
        configurationFactory.initTimer();
        loggerService.initTimer();
        //externalScimService.init();
        customScriptManager.initTimer(Arrays.asList(
            CustomScriptType.SCIM, CustomScriptType.PERSISTENCE_EXTENSION, CustomScriptType.ID_GENERATOR));
        logger.info("Initialized!");

    }

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() {
        String encodeSalt = configurationFactory.getCryptoConfigurationSalt();

        if (StringHelper.isEmpty(encodeSalt)) {
            throw new ConfigurationException("Encode salt isn't defined");
        }

        try {
            return StringEncrypter.instance(encodeSalt);
        } catch (StringEncrypter.EncryptionException ex) {
            throw new ConfigurationException("Failed to create StringEncrypter instance");
        }

    }

    @Produces
    @ApplicationScoped
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    public PersistenceEntryManager createPersistenceEntryManager() throws Exception {

        logger.debug("Obtaining PersistenceEntryManagerFactory from persistence API");
        FileConfiguration persistenceConfig = persistenceConfiguration.getConfiguration();
        Properties backendProperties = persistenceConfig.getProperties();
        PersistenceEntryManagerFactory factory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConfiguration);

        String type = factory.getPersistenceType();
        logger.info("Underlying database of type '{}' detected", type);
        String file = String.format("%s/%s", DEFAULT_CONF_BASE, persistenceConfiguration.getFileName());
        logger.info("Using config file: {}", file);

        logger.debug("Decrypting backend properties");
        backendProperties = PropertiesDecrypter.decryptAllProperties(stringEncrypter, backendProperties);

        logger.info("Obtaining a Persistence EntryManager");
        int i = 0;
        PersistenceEntryManager entryManager = null;

        do {
            try {
                i++;
                entryManager = factory.createEntryManager(backendProperties);
            } catch (Exception e) {
                logger.warn("Unable to create persistence entry manager, retrying in {} seconds", RETRY_INTERVAL);
                Thread.sleep(RETRY_INTERVAL * 1000);
            }
        } while (entryManager == null && i < RETRIES);

        if (entryManager == null) {
            logger.error("No EntryManager could be obtained");
        }
        return entryManager;

    }

}
