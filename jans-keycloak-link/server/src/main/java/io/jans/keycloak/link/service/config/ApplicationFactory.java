/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.keycloak.link.service.config;

import io.jans.config.GluuConfiguration;
import io.jans.keycloak.link.server.service.ConfigurationService;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.NullMessageConfiguration;
import jakarta.enterprise.inject.Produces;
import org.slf4j.Logger;

import io.jans.keycloak.link.model.config.AppConfiguration;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import jakarta.enterprise.context.ApplicationScoped;
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

    @Inject
    private ConfigurationService сonfigurationService;

    public static final String PERSISTENCE_ENTRY_MANAGER_FACTORY_NAME = "persistenceEntryManagerFactory";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";
    public static final String PERSISTENCE_METRIC_ENTRY_MANAGER_NAME = "persistenceMetricEntryManager";

    public static final String PERSISTENCE_CENTRAL_ENTRY_MANAGER_NAME = "centralPersistenceEntryManager";
    
    public static final String PERSISTENCE_METRIC_CONFIG_GROUP_NAME = "metric";

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

    @Produces
    @ApplicationScoped
    public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        GluuConfiguration jansConf = сonfigurationService.getConfiguration();
        DocumentStoreConfiguration documentStoreConfiguration = jansConf.getDocumentStoreConfiguration();
        if ((documentStoreConfiguration == null) || (documentStoreConfiguration.getDocumentStoreType() == null)) {
            log.error("Failed to read document store configuration from DB. Please check configuration jsDocStoreConf attribute " +
                    "that must contain document store configuration JSON represented by DocumentStoreConfiguration.class. Appliance DN: {0}", jansConf.getDn());
            log.info("Creating fallback LOCAL document store configuration ... ");

            documentStoreConfiguration = new DocumentStoreConfiguration();
            documentStoreConfiguration.setLocalConfiguration(new LocalDocumentStoreConfiguration());

            log.info("LOCAL document store configuration is created.");
        }

        log.info("Document store configuration: {0}" , documentStoreConfiguration);
        return documentStoreConfiguration;
    }

    @Produces
    @ApplicationScoped
    public MessageConfiguration getMessageConfiguration() {
        GluuConfiguration jansConf = сonfigurationService.getConfiguration();
        MessageConfiguration messageConfiguration = jansConf.getMessageConfiguration();
        if (messageConfiguration == null || messageConfiguration.getMessageProviderType() == null) {
            log.error("Failed to read message configuration from DB. Please check configuration jsMessageConf attribute " +
                    "that must contain message configuration JSON represented by MessageConfiguration.class. Appliance DN: {0}" , jansConf.getDn());
            log.info("Creating fallback Null message configuration ... ");

            messageConfiguration = new MessageConfiguration();
            messageConfiguration.setMessageProviderType(MessageProviderType.DISABLED);
            messageConfiguration.setNullConfiguration(new NullMessageConfiguration());

            log.info("NULL message configuration is created.");
        }

        log.info("Message configuration: {0}" , messageConfiguration);
        return messageConfiguration;
    }
}