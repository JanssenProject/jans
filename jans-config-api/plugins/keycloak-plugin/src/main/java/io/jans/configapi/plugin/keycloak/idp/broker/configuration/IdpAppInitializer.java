/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.keycloak.idp.broker.timer.IdpMetadataValidationTimer;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.timer.QuartzSchedulerManager;

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

import org.slf4j.Logger;

@ApplicationScoped
@Named("idpAppInitializer")
public class IdpAppInitializer {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    BeanManager beanManager;

    @Inject
    IdpConfigurationFactory idpConfigurationFactory;

    @Inject
    QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    IdpMetadataValidationTimer idpMetadataValidationTimer;

    public void onAppStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("=============  Initializing Keycloak Plugin ========================");
        log.debug("init:{}", init);

        // configuration
        this.idpConfigurationFactory.create();
        initSchedulerService();
        idpMetadataValidationTimer.initTimer();

        log.info("==============  Keycloak Plugin IS UP AND RUNNING ===================");
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

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.info("================================================================");
        log.info("===========  Keycloak Plugin STOPPED  ==========================");
        log.info("init:{}", init);
        log.info("================================================================");
    }

    @Produces
    @ApplicationScoped
    public IdpConfigurationFactory getIdpConfigurationFactory() {
        return idpConfigurationFactory;
    }

}
