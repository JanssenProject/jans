/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service.config;

import org.slf4j.Logger;

import io.jans.cacherefresh.model.config.AppConfiguration;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cache.CacheConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Holds factory methods to create services
 *
 * @author Yuriy Movchan Date: 02/14/2017
 */
@ApplicationScoped
public class ApplicationFactory {

    @Inject
    private Logger log;

    @Inject
    private ConfigurationFactory configurationFactory;

	@Inject
	private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private AppConfiguration appConfiguration;
    
    private SmtpConfiguration smtpConfiguration = new SmtpConfiguration();

	private CacheConfiguration cacheConfiguration = new CacheConfiguration();

    public static final String PERSISTENCE_ENTRY_MANAGER_FACTORY_NAME = "persistenceEntryManagerFactory";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";
    public static final String PERSISTENCE_METRIC_ENTRY_MANAGER_NAME = "persistenceMetricEntryManager";

    public static final String PERSISTENCE_CENTRAL_ENTRY_MANAGER_NAME = "centralPersistenceEntryManager";
    
    public static final String PERSISTENCE_METRIC_CONFIG_GROUP_NAME = "metric";

    @Produces
    @RequestScoped
    public SmtpConfiguration getSmtpConfiguration() {
        return smtpConfiguration;
    }

    @Produces
    @ApplicationScoped
    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory() {
        PersistenceConfiguration persistenceConfiguration = configurationFactory.getPersistenceConfiguration();
        return persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConfiguration);
    }

	public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(PersistenceConfiguration persistenceConfiguration) {
		return persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConfiguration);
	}

    public PersistenceEntryManagerFactory getPersistenceEntryManagerFactory(Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryClass) {
        return persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceEntryManagerFactoryClass);
    }

}