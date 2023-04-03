/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Gluu
 */

package io.jans.cacherefresh.config;

//import javax.inject.Inject;
//import javax.inject.Named;

import io.jans.cacherefresh.model.ApiConf;
import io.jans.config.oxtrust.AppConfiguration;
import io.jans.config.oxtrust.CacheRefreshConfiguration;
import io.jans.config.oxtrust.ImportPersonConfig;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version 0.1, 05/25/2020
 */
@ApplicationScoped
public class ConfigurationFactory extends io.jans.cacherefresh.service.ConfigurationFactory<ApiConf> {

	@Inject
	private Logger log;

	//@Inject
	//@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private PersistenceEntryManager persistenceEntryManagerInstance;


	private CacheRefreshConfiguration cacheRefreshConfiguration;
	//private ImportPersonConfig importPersonConfig;
	//private io.jans.cacherefresh.model.CacheRefreshConfiguration appConfiguration;

	private boolean apiConfigloaded = false;
	private long apiLoadedRevision = -1;


	@Override
	protected void init(ApiConf conf) {
		try {
			this.cacheRefreshConfiguration = conf.getDynamicConf();
		}catch (Exception e){
			log.warn("==============================================");
			log.warn("OxTrust configuration initialization",e);
		}
	}

	public void create() {
		System.out.println("Loading Configuration");


		// load api config from DB
		if (!loadApiConfigFromDb()) {
			System.out.println("Failed to load api configuration from persistence. Please fix it!!!.");
			throw new ConfigurationException("Failed to load api configuration from persistence.");
		} else {
			System.out.println("Api Configuration loaded successfully - apiLoadedRevision:{}, ApiAppConfiguration:{}"+
					this.apiLoadedRevision);
		}

		// load auth config from DB
		/*if (!loadAuthConfigFromDb()) {
			System.out.println("Failed to load auth configuration from persistence. Please fix it!!!.");
			throw new ConfigurationException("Failed to load auth configuration from persistence.");
		} else {
			System.out.println("Auth Configuration loaded successfully - authLoadedRevision:{}", this.authLoadedRevision);
		}*/
	}

	private boolean loadApiConfigFromDb() {
		System.out.println("Loading Api configuration from '{}' DB...");
		try {
			final ApiConf apiConf = loadConfigurationFromDb();
			System.out.println("ApiConf configuration '{}' DB..."+ apiConf);

			if (apiConf != null) {
				init(apiConf);

				// Destroy old configuration
				if (this.apiConfigloaded) {
					//destroy(ApiConf.class);
				}

				this.apiConfigloaded = true;
				//apiConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(apiAppConfiguration);

				return true;
			}
		} catch (Exception ex) {
			System.out.println("Unable to find api configuration in DB..." + ex.getMessage());
			ex.printStackTrace();
		}
		return false;
	}


	@Override
	protected ApiConf loadConfigurationFromDb(String... returnAttributes) {
		final PersistenceEntryManager persistenceEntryManager = getPersistenceEntryManagerInstance();
		//final String configurationDn = getConfigurationDn();
		try {
			final ApiConf conf = persistenceEntryManager.find("ou=jans-cache-refresh,ou=configuration,o=jans", ApiConf.class,
					returnAttributes);

			return conf;
		} catch (BasePersistenceException ex) {
			System.out.println("Failed to load configuration from LDAP"+ ex);
		}

		return null;
	}


	@Override
	protected void destroryLoadedConfiguration() {
		destroy(AppConfiguration.class);
	}

	@Override
	protected boolean isNewRevision(ApiConf conf) {
		return conf.getRevision() > this.apiLoadedRevision;
	}

	@Override
	public String getConfigurationDn() {
		return this.baseConfiguration.getString("oxtrust_ConfigurationEntryDN");
	}

	public CacheRefreshConfiguration getCacheRefreshConfiguration() {
		return cacheRefreshConfiguration;
	}

	/*public ImportPersonConfig getImportPersonConfig() {
		return importPersonConfig;
	}

	@Produces
	@ApplicationScoped
	public io.jans.cacherefresh.model.CacheRefreshConfiguration getAppConfiguration() {
		return appConfiguration;
	}*/

	@Override
	protected String getApplicationPropertiesFileName() {
		return APP_PROPERTIES_FILE;
	}

	public PersistenceEntryManager getPersistenceEntryManagerInstance() {
		return persistenceEntryManagerInstance;
	}

	public void setPersistenceEntryManagerInstance(PersistenceEntryManager persistenceEntryManagerInstance) {
		this.persistenceEntryManagerInstance = persistenceEntryManagerInstance;
	}
}
