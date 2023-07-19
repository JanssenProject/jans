/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.security.api.ApiProtectionService;
import io.jans.configapi.security.service.AuthorizationService;
import io.jans.configapi.service.status.StatusCheckerTimer;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.service.PersistanceFactoryService;

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
@Named("samlAppInitializer")
public class SamlAppInitializer {

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    BeanManager beanManager;

    @Inject
    SamlConfigurationFactory samlConfigurationFactory;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private ApiProtectionService apiProtectionService;

    @Inject
    private Instance<AuthorizationService> authorizationServiceInstance;

    @Inject
    StatusCheckerTimer statusCheckerTimer;


    public void onAppStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.error("=============  Initializing SAML  ========================");
        log.error("init:{}", init);

        // configuration
        this.samlConfigurationFactory.create();

        log.error("==============  APPLICATION IS UP AND RUNNING ===================");
    }

    public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
        log.error("================================================================");
        log.error("===========  SAML APPLICATION STOPPED  ==========================");
        log.error("init:{}", init);
        log.error("================================================================");
    }

    @Produces
    @ApplicationScoped
    public SamlConfigurationFactory getSamlConfigurationFactory() {
        return samlConfigurationFactory;
    }

   
}
