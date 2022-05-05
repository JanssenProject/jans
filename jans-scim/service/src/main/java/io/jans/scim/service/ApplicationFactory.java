/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.jans.scim.model.conf.AppConfiguration;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.cache.InMemoryConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import org.slf4j.Logger;

import io.jans.scim.model.GluuConfiguration;

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
    private ConfigurationService configurationService;

	@Inject
	private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private AppConfiguration appConfiguration;

    public static final String PERSISTENCE_ENTRY_MANAGER_FACTORY_NAME = "persistenceEntryManagerFactory";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";
    public static final String PERSISTENCE_METRIC_ENTRY_MANAGER_NAME = "persistenceMetricEntryManager";

    public static final String PERSISTENCE_CENTRAL_ENTRY_MANAGER_NAME = "centralPersistenceEntryManager";
    
    public static final String PERSISTENCE_METRIC_CONFIG_GROUP_NAME = "metric";

    @Produces @ApplicationScoped
   	public CacheConfiguration getCacheConfiguration() {
   		CacheConfiguration cacheConfiguration = configurationService.getConfiguration().getCacheConfiguration();
   		if ((cacheConfiguration == null) || (cacheConfiguration.getCacheProviderType() == null)) {
   			log.error("Failed to read cache configuration from DB. Please check configuration jsCacheConf attribute " +
   					"that must contain cache configuration JSON represented by CacheConfiguration.class. Appliance DN: " + configurationService.getConfiguration().getDn());
   			log.info("Creating fallback IN-MEMORY cache configuration ... ");

   			cacheConfiguration = new CacheConfiguration();
   			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

   			log.info("IN-MEMORY cache configuration is created.");
   		} else if (cacheConfiguration.getNativePersistenceConfiguration() != null) {
			cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(appConfiguration.getBaseDN());
		}
   		log.info("Cache configuration: " + cacheConfiguration);
   		return cacheConfiguration;
   	}

    @Produces @ApplicationScoped
   	public DocumentStoreConfiguration getDocumentStoreConfiguration() {
    	DocumentStoreConfiguration documentStoreConfiguration = configurationService.getConfiguration().getDocumentStoreConfiguration();
   		if ((documentStoreConfiguration == null) || (documentStoreConfiguration.getDocumentStoreType() == null)) {
   			log.error("Failed to read document store configuration from DB. Please check configuration jsDocStoreConf attribute " +
   					"that must contain document store configuration JSON represented by DocumentStoreConfiguration.class. Appliance DN: " + configurationService.getConfiguration().getDn());
   			log.info("Creating fallback LOCAL document store configuration ... ");

   			documentStoreConfiguration = new DocumentStoreConfiguration();
   			documentStoreConfiguration.setLocalConfiguration(new LocalDocumentStoreConfiguration());

   			log.info("LOCAL document store configuration is created.");
		}

   		log.info("Document store configuration: " + documentStoreConfiguration);
   		return documentStoreConfiguration;
   	}

	@Produces @RequestScoped
	public SmtpConfiguration getSmtpConfiguration() {
		GluuConfiguration configuration = configurationService.getConfiguration();
		SmtpConfiguration smtpConfiguration = configuration.getSmtpConfiguration();
		
		if (smtpConfiguration == null) {
			return new SmtpConfiguration();
		}

		configurationService.decryptSmtpPassword(smtpConfiguration);

		return smtpConfiguration;
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