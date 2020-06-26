package org.gluu.oxtrust.service.init;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.config.oxtrust.AppConfiguration;
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

	private AppConfiguration appConfiguration;

	private long loadedRevision = -1;

	@Override
	protected void init(LdapOxTrustConfiguration conf) {
		this.appConfiguration = conf.getApplication();
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
	}

	@Override
	protected boolean isNewRevision(LdapOxTrustConfiguration conf) {
		return conf.getRevision() > this.loadedRevision;
	}

	@Override
	public String getConfigurationDn() {
		return this.baseConfiguration.getString("oxtrust_ConfigurationEntryDN");
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
