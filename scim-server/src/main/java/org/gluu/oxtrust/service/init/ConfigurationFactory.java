package org.gluu.oxtrust.service.init;

/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.config.oxtrust.AttributeResolverConfiguration;
import org.gluu.config.oxtrust.CacheRefreshConfiguration;
import org.gluu.config.oxtrust.ImportPersonConfig;
import org.gluu.config.oxtrust.LdapOxTrustConfiguration;
import org.gluu.oxtrust.service.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version 0.1, 05/25/2020
 */
@ApplicationScoped
public class ConfigurationFactory extends org.gluu.service.config.ConfigurationFactory<LdapOxTrustConfiguration> {

	@Inject
	private Logger log;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;


	private CacheRefreshConfiguration cacheRefreshConfiguration;
	private ImportPersonConfig importPersonConfig;
	private AppConfiguration appConfiguration;

	private long loadedRevision = -1;

	@Override
	protected void init(LdapOxTrustConfiguration conf) {
		this.appConfiguration = conf.getApplication();
		this.cacheRefreshConfiguration = conf.getCacheRefresh();
		this.importPersonConfig = conf.getImportPersonConfig();
		this.attributeResolverConfiguration = conf.getAttributeResolverConfig();
		this.loadedRevision = conf.getRevision();
	}

	@Override
	protected LdapOxTrustConfiguration loadConfigurationFromDb(String... returnAttributes) {
		final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
		final String configurationDn = getConfigurationDn();
		try {
			final LdapOxTrustConfiguration conf = persistenceEntryManager.find(configurationDn, LdapOxTrustConfiguration.class,
					returnAttributes);

			return conf;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load configuration from LDAP", ex);
		}

		return null;
	}


	@Override
	protected void destroryLoadedConfiguration() {
		destroy(AppConfiguration.class);
		destroy(CacheRefreshConfiguration.class);
		destroy(ImportPersonConfig.class);
		destroy(AttributeResolverConfiguration.class);
	}

	@Override
	protected boolean isNewRevision(LdapOxTrustConfiguration conf) {
		return conf.getRevision() > this.loadedRevision;
	}

	@Override
	public String getConfigurationDn() {
		return this.baseConfiguration.getString("oxtrust_ConfigurationEntryDN");
	}

	public CacheRefreshConfiguration getCacheRefreshConfiguration() {
		return cacheRefreshConfiguration;
	}

	public ImportPersonConfig getImportPersonConfig() {
		return importPersonConfig;
	}

	@Produces
	@ApplicationScoped
	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	@Override
	protected String getApplicationPropertiesFileName() {
		return APP_PROPERTIES_FILE;
	}

}
