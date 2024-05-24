/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.lock.service.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.config.GluuConfiguration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.lock.server.service.ConfigurationService;
import io.jans.model.SmtpConfiguration;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.cache.InMemoryConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.MessageProviderType;
import io.jans.service.message.model.config.NullMessageConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Holds factory methods to create services
 *
 * @author Yuriy Movchan Date: 12/12/2023
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
    private StaticConfiguration staticConfiguration;

	@Inject
	private PersistanceFactoryService persistanceFactoryService;

    public static final String PERSISTENCE_ENTRY_MANAGER_FACTORY_NAME = "persistenceEntryManagerFactory";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";

    @Produces
    @ApplicationScoped
    public CacheConfiguration getCacheConfiguration() {
        CacheConfiguration cacheConfiguration = configurationService.getConfiguration().getCacheConfiguration();
        if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
            log.error("Failed to read cache configuration from DB. Please check configuration jsCacheConf attribute " +
                    "that must contain cache configuration JSON represented by CacheConfiguration.class. Appliance DN: " + configurationService.getConfiguration().getDn());
            log.info("Creating fallback IN-MEMORY cache configuration ... ");

            cacheConfiguration = new CacheConfiguration();
            cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

            log.info("IN-MEMORY cache configuration is created.");
        }
        if (cacheConfiguration.getNativePersistenceConfiguration() != null) {
            if (!StringUtils.isEmpty(staticConfiguration.getBaseDn().getSessions())) {
                cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(StringUtils.remove(staticConfiguration.getBaseDn().getSessions(), "ou=sessions,").trim());
            }
        }
        log.info("Cache configuration: " + cacheConfiguration);
        return cacheConfiguration;
    }

    @Produces
    @ApplicationScoped
    public MessageConfiguration getMessageConfiguration() {
    	MessageConfiguration messageConfiguration = configurationService.getConfiguration().getMessageConfiguration();
        if (messageConfiguration == null || messageConfiguration.getMessageProviderType() == null) {
            log.error("Failed to read message configuration from DB. Please check configuration jsMessageConf attribute " +
                    "that must contain message configuration JSON represented by MessageConfiguration.class. Appliance DN: " + configurationService.getConfiguration().getDn());
            log.info("Creating fallback Null message configuration ... ");

            messageConfiguration = new MessageConfiguration();
            messageConfiguration.setMessageProviderType(MessageProviderType.DISABLED);
            messageConfiguration.setNullConfiguration(new NullMessageConfiguration());

            log.info("NULL message configuration is created.");
        }

        log.info("Message configuration: " + messageConfiguration);
        return messageConfiguration;
    }

    @Produces
    @ApplicationScoped
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

    @Produces
    @RequestScoped
    public SmtpConfiguration getSmtpConfiguration() {
        GluuConfiguration configuration = configurationService.getConfiguration();
        SmtpConfiguration smtpConfiguration = configuration.getSmtpConfiguration();

        if (smtpConfiguration == null) {
            return new SmtpConfiguration();
        }

        configurationService.decryptSmtpPasswords(smtpConfiguration);

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