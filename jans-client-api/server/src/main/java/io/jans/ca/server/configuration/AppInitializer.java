package io.jans.ca.server.configuration;

/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.ca.server.persistence.service.PersistenceServiceImpl;
import io.jans.ca.server.security.service.AuthorizationService;
import io.jans.ca.server.security.service.ClientApiAuthorizationService;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.logger.LoggerServiceImpl;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.PythonService;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;

import java.util.Properties;

@ApplicationScoped
public class AppInitializer {

    private static final int RETRIES = 15;
    private static final int RETRY_INTERVAL = 15;
    private static final String DEFAULT_CONF_BASE = "/etc/jans/conf";

    @Inject
    Logger logger;
    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;
    @Inject
    private Instance<AuthorizationService> authorizationServiceInstance;
    @Inject
    BeanManager beanManager;

    @Inject
    private StringEncrypter stringEncrypter;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    private PythonService pythonService;

    @Inject
    private LoggerServiceImpl loggerService;

    @Inject
    PersistenceConfiguration persistenceConfiguration;
    @Inject
    PersistenceServiceImpl persistenceService;
    @Inject
    RpService rpService;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {

        if (System.getProperties().containsKey("test.client.api.url")) {
            return;
        }
        logger.info("=============  STARTING CLIENT API APPLICATION  ========================");
        logger.info("init:{}", init);

        // Resteasy config - Turn off the default patch filter
        System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);

        // configuration
        configurationFactory.create();
        persistenceEntryManagerInstance.get();
        this.createAuthorizationService();

        // Initialize python interpreter
        pythonService.initPythonInterpreter(configurationFactory.getBaseConfiguration().getString("pythonModulesDir", null));

        // Start timer
        initSchedulerService();

        // Schedule timer tasks
        loggerService.initTimer();

        // Schedule timer tasks
        configurationFactory.initTimer();

        //Clear RP Test Data with System param
        if (System.getProperties().containsKey("clearTestData")) {
            clearRPTestData();
        }

        logger.info("============== CLIENT API APPLICATION IS UP AND RUNNING ===================");
    }

    @Produces
    @ApplicationScoped
    public ConfigurationFactory getConfigurationFactory() {
        return configurationFactory;
    }

    @Produces
    @ApplicationScoped
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    public PersistenceEntryManager createPersistenceEntryManager() throws InterruptedException {

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
                logger.info("Read backend properties: {}", backendProperties);
                i++;
                entryManager = factory.createEntryManager(backendProperties);
                logger.info("PersistenceEntryManager read: {}", entryManager);
            } catch (Exception e) {
                logger.warn("Unable to create persistence entry manager, retrying in {} seconds", RETRY_INTERVAL);
                Thread.sleep(RETRY_INTERVAL * 1000L);
            }
        } while (entryManager == null && i < RETRIES);

        if (entryManager == null) {
            logger.error("No EntryManager could be obtained");
        }
        return entryManager;

    }

    @Produces
    @ApplicationScoped
    @Named("authorizationService")
    private AuthorizationService createAuthorizationService() {
        logger.info("=============  AppInitializer::createAuthorizationService()  ================  ");
        try {
            return authorizationServiceInstance.select(ClientApiAuthorizationService.class).get();
        } catch (Exception ex) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to create AuthorizationService instance - exception:{} ", ex);
            }
            throw new ConfigurationException("Failed to create AuthorizationService instance , ", ex);
        }
    }

    public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
        closePersistenceEntryManager();
        PersistenceEntryManager ldapEntryManager = persistenceEntryManagerInstance.get();
        persistenceEntryManagerInstance.destroy(ldapEntryManager);
        logger.debug("Recreated instance {} with operation service: {} - event:{}", ldapEntryManager, ldapEntryManager.getOperationService(), event);
    }

    private void closePersistenceEntryManager() {
        PersistenceEntryManager oldInstance = CdiUtil.getContextBean(beanManager, PersistenceEntryManager.class, ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
        if (oldInstance == null || oldInstance.getOperationService() == null) return;

        logger.debug("Attempting to destroy {} with operation service: {}", oldInstance, oldInstance.getOperationService());
        oldInstance.destroy();
        logger.debug("Destroyed {} with operation service: {}", oldInstance, oldInstance.getOperationService());
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext context) {
        logger.info("================================================================");
        logger.info("===========  jans-client-api service STOPPED  ==========================");
        logger.info("servletContext:{}", context);

        logger.info("================================================================");
    }

    protected void initSchedulerService() {
        logger.debug("Initializing Scheduler Service");
        quartzSchedulerManager.start();

        String disableScheduler = System.getProperties().getProperty("gluu.disable.scheduler");
        if (Boolean.parseBoolean(disableScheduler)) {
            this.logger.warn("Suspending Quartz Scheduler Service...");
            quartzSchedulerManager.standby();
        }
    }

    private void clearRPTestData() {
        try {
            String val = System.getProperty("clearTestData");
            if (val != null && !val.isEmpty() && Boolean.parseBoolean(val)) {
                persistenceService.create();
                rpService.removeAllRps();
                rpService.load();
            }
        } catch (Exception e) {
            logger.error("Failed to execute clearTestData action", e);
        }
    }


}
