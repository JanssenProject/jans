package org.gluu.oxauthconfigapi.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.config.oxtrust.LdapOxTrustConfiguration;
import org.gluu.oxtrust.service.ApplicationFactory;
//import org.gluu.oxtrust.service.custom.LdapCentralConfigurationReload;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.slf4j.Logger;

import io.quarkus.arc.AlternativePriority;

@ApplicationScoped
@AlternativePriority(1)
public class ConfigurationFactory extends org.gluu.service.config.ConfigurationFactory<LdapOxTrustConfiguration> {

	@Inject
	Logger logger;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	private AppConfiguration appConfiguration;

	@Produces
	@ApplicationScoped
	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	@Override
	protected String getApplicationPropertiesFileName() {
		return APP_PROPERTIES_FILE;
	}

	public String getConfigurationDn() {
		return this.baseConfiguration.getString("oxtrust_ConfigurationEntryDN");
	}

	@Override
	protected void destroryLoadedConfiguration() {
		destroy(AppConfiguration.class);
	}

	@Override
	protected void init(LdapOxTrustConfiguration conf) {
		this.appConfiguration = conf.getApplication();

	}

	@Override
	protected boolean isNewRevision(LdapOxTrustConfiguration arg0) {
		return false;
	}

	@Override
	protected LdapOxTrustConfiguration loadConfigurationFromDb(String... returnAttributes) {
		try {
			final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
			final String configurationDn = getConfigurationDn();
			final LdapOxTrustConfiguration conf = persistenceEntryManager.find(configurationDn,
					LdapOxTrustConfiguration.class, returnAttributes);
			return conf;
		} catch (BasePersistenceException ex) {
			logger.error("Failed to load configuration from LDAP", ex);
		}
		return null;
	}

}
