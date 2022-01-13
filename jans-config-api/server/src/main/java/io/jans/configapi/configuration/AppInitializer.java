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
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.util.StringHelper;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

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
    private QuartzSchedulerManager quartzSchedulerManager;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("========================== Initializing - App =======================================");
        log.info("=============  STARTING API APPLICATION  ========================");
        System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
        this.configurationFactory.create();
        persistenceEntryManagerInstance.get();
        this.createAuthorizationService();

        // Start timer
        initSchedulerService();

        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);

        log.info("==============  APPLICATION IS UP AND RUNNING ===================");
        log.info("========================== App - Initialized =======================================");
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  API APPLICATION STOPPED  ==========================");
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
        log.info(
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
            throw new ConfigurationException("Failed to create AuthorizationService instance", ex);
        }
    }

    public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
        closePersistenceEntryManager();
        PersistenceEntryManager ldapEntryManager = persistenceEntryManagerInstance.get();
        persistenceEntryManagerInstance.destroy(ldapEntryManager);
        log.debug("Recreated instance {} with operation service: {}", ldapEntryManager,
                ldapEntryManager.getOperationService());
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
        quartzSchedulerManager.start();

        String disableScheduler = System.getProperties().getProperty("gluu.disable.scheduler");
        if (Boolean.parseBoolean(disableScheduler)) {
            this.log.warn("Suspending Quartz Scheduler Service...");
            quartzSchedulerManager.standby();
        }
    }
}
