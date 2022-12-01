/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.security.api.ApiProtectionService;
import io.jans.configapi.security.service.AuthorizationService;
import io.jans.configapi.security.service.OpenIdAuthorizationService;
import io.jans.configapi.service.status.StatusCheckerTimer;
import io.jans.configapi.service.logger.LoggerService;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.PythonService;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.util.StringHelper;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;
import java.util.ArrayList;

import java.util.List;

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

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

@ApplicationScoped
@Named("appInitializer")
public class AppInitializer {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    BeanManager beanManager;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private ApiProtectionService apiProtectionService;

    @Inject
    private Instance<AuthorizationService> authorizationServiceInstance;

    @Inject
    StatusCheckerTimer statusCheckerTimer;

    @Inject
    private LoggerService loggerService;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    private CustomScriptManager customScriptManager;

    @Inject
    private PythonService pythonService;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("=============  STARTING API APPLICATION  ========================");
        log.info("init:{}", init);

        // Resteasy config - Turn off the default patch filter
        System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);

        // configuration
        this.configurationFactory.create();
        persistenceEntryManagerInstance.get();
        this.createAuthorizationService();

        // Initialize python interpreter
        pythonService
                .initPythonInterpreter(configurationFactory.getBaseConfiguration().getString("pythonModulesDir", null));

        // Start timer
        initSchedulerService();

        // Initialize custom Script
        initCustomScripts();

        // Stats timer
        statusCheckerTimer.initTimer();

        // Schedule timer tasks
        loggerService.initTimer();

        // Schedule timer tasks
        configurationFactory.initTimer();

        log.info("==============  APPLICATION IS UP AND RUNNING ===================");
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  API APPLICATION STOPPED  ==========================");
        log.info("init:{}", init);
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public ConfigurationFactory getConfigurationFactory() {
        return configurationFactory;
    }

    @Produces
    @ApplicationScoped
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    public PersistenceEntryManager createPersistenceEntryManager() throws OxIntializationException {
        PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService
                .getPersistenceEntryManagerFactory(configurationFactory.getPersistenceConfiguration());
        PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory
                .createEntryManager(configurationFactory.getDecryptedConnectionProperties());
        log.debug("Created {} with operation service {}", persistenceEntryManager,
                persistenceEntryManager.getOperationService());
        return persistenceEntryManager;
    }

    @Produces
    @ApplicationScoped
    @Named("authorizationService")
    private AuthorizationService createAuthorizationService() {
        log.error(
                "=============  AppInitializer::createAuthorizationService() - configurationFactory.getApiProtectionType():{} ",
                configurationFactory.getApiProtectionType());

        if (StringHelper.isEmpty(configurationFactory.getApiProtectionType())) {
            throw new ConfigurationException("API Protection Type not defined");
        }
        try {
            // Verify resources available
            apiProtectionService.verifyResources(configurationFactory.getApiProtectionType(),
                    configurationFactory.getApiClientId());
            return authorizationServiceInstance.select(OpenIdAuthorizationService.class).get();
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Failed to create AuthorizationService instance - apiProtectionType:{}, exception:{} ",
                        configurationFactory.getApiProtectionType(), ex);
            }
            throw new ConfigurationException("Failed to create AuthorizationService instance  - apiProtectionType = "
                    + configurationFactory.getApiProtectionType(), ex);
        }
    }

    public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
        closePersistenceEntryManager();
        PersistenceEntryManager ldapEntryManager = persistenceEntryManagerInstance.get();
        persistenceEntryManagerInstance.destroy(ldapEntryManager);
        log.debug("Recreated instance {} with operation service: {} - event:{}", ldapEntryManager,
                ldapEntryManager.getOperationService(), event);
    }

    private void closePersistenceEntryManager() {
        PersistenceEntryManager oldInstance = CdiUtil.getContextBean(beanManager, PersistenceEntryManager.class,
                ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
        if (oldInstance == null || oldInstance.getOperationService() == null)
            return;

        log.debug("Attempting to destroy {} with operation service: {}", oldInstance,
                oldInstance.getOperationService());
        oldInstance.destroy();
        log.debug("Destroyed {} with operation service: {}", oldInstance, oldInstance.getOperationService());
    }

    protected void initSchedulerService() {
        log.debug("Initializing Scheduler Service");
        quartzSchedulerManager.start();

        String disableScheduler = System.getProperties().getProperty("gluu.disable.scheduler");
        if (Boolean.parseBoolean(disableScheduler)) {
            this.log.warn("Suspending Quartz Scheduler Service...");
            quartzSchedulerManager.standby();
        }
    }

    private void initCustomScripts() {
        List<CustomScriptType> supportedCustomScriptTypes = new ArrayList<>();
        supportedCustomScriptTypes.add(CustomScriptType.CONFIG_API);
        customScriptManager.initTimer(supportedCustomScriptTypes);
        log.info("Initialized Custom Scripts!");
    }
}
