/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.model.SmtpConfiguration;
import org.gluu.oxauth.crypto.signature.SHA256withECDSASignatureVerification;
import org.gluu.oxauth.model.config.ConfigurationFactory;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.cache.InMemoryConfiguration;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.gluu.oxauth.model.config.StaticConfiguration;

/**
 * Holds factory methods to create services
 *
 * @author Yuriy Movchan Date: 05/22/2015
 */
@ApplicationScoped
@Named
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
    private StaticConfiguration staticConfiguration;

    public static final String PERSISTENCE_AUTH_CONFIG_NAME = "persistenceAuthConfig";

    public static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";
    public static final String PERSISTENCE_METRIC_ENTRY_MANAGER_NAME = "persistenceMetricEntryManager";

    public static final String PERSISTENCE_AUTH_ENTRY_MANAGER_NAME = "persistenceAuthEntryManager";

    public static final String PERSISTENCE_METRIC_CONFIG_GROUP_NAME = "metric";

	@Produces @ApplicationScoped @Named("sha256withECDSASignatureVerification")
    public SHA256withECDSASignatureVerification getBouncyCastleSignatureVerification() {
        return new SHA256withECDSASignatureVerification();
    }

	@Produces @ApplicationScoped
	public CacheConfiguration getCacheConfiguration() {
		CacheConfiguration cacheConfiguration = configurationService.getConfiguration().getCacheConfiguration();
		if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
			log.error("Failed to read cache configuration from LDAP. Please check configuration oxCacheConfiguration attribute " +
					"that must contain cache configuration JSON represented by CacheConfiguration.class. Applieance DN: " + configurationService.getConfiguration().getDn());
			log.info("Creating fallback IN-MEMORY cache configuration ... ");

			cacheConfiguration = new CacheConfiguration();
			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

			log.info("IN-MEMORY cache configuration is created.");
		} else if (cacheConfiguration.getNativePersistenceConfiguration() != null) {
			cacheConfiguration.getNativePersistenceConfiguration().setBaseDn(StringUtils.remove(staticConfiguration.getBaseDn().getUmaBase(), "ou=uma,").trim());
		}
		log.info("Cache configuration: " + cacheConfiguration);
		return cacheConfiguration;
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

}