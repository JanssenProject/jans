package org.gluu.oxauthconfigapi;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.gluu.exception.OxIntializationException;
import org.gluu.oxauthconfigapi.configuration.ConfigurationFactory;
import org.gluu.oxtrust.service.ApplicationFactory;
import org.gluu.oxtrust.service.EncryptionService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.util.CdiUtil;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.StringEncrypter;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.Properties;

@ApplicationScoped
public class OxauthConfigApiApplication {

	@Inject
	Logger logger;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	Instance<EncryptionService> encryptionServiceInstance;

	@Inject
	ApplicationFactory applicationFactory;

	@Inject
	BeanManager beanManager;

	@Inject
	ConfigurationFactory configurationFactory;

	void onStart(@Observes StartupEvent ev) {
		logger.info("=================================================================");
		logger.info("=============  STARTING API APPLICATION  ========================");
		logger.info("=================================================================");
		System.setProperty(ResteasyContextParameters.RESTEASY_PATCH_FILTER_DISABLED, "true");
		configurationFactory.create();
		persistenceEntryManagerInstance.get();
		logger.info("=================================================================");
		logger.info("==============  APPLICATION IS UP AND RUNNING ===================");
		logger.info("=================================================================");
	}

	void onStop(@Observes ShutdownEvent ev) {
		logger.info("================================================================");
		logger.info("===========  API APPLICATION STOPPED  ==========================");
		logger.info("================================================================");
	}

	@Produces
	@ApplicationScoped
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	public PersistenceEntryManager createPersistenceEntryManager() {
		Properties connectionProperties = preparePersistanceProperties();
		PersistenceEntryManagerFactory persistenceEntryManagerFactory = applicationFactory
				.getPersistenceEntryManagerFactory(configurationFactory.getPersistenceConfiguration());
		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory
				.createEntryManager(connectionProperties);
		logger.debug("Created {}: {} with operation service: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
                persistenceEntryManager, persistenceEntryManager.getOperationService());
		return persistenceEntryManager;
	}

	@Produces
	@ApplicationScoped
	public StringEncrypter getStringEncrypter() throws OxIntializationException {
		String encodeSalt = configurationFactory.getCryptoConfigurationSalt();
		if (StringHelper.isEmpty(encodeSalt)) {
			throw new OxIntializationException("Encode salt isn't defined");
		}
		try {
			StringEncrypter stringEncrypter = StringEncrypter.instance(encodeSalt);
			return stringEncrypter;
		} catch (EncryptionException ex) {
			throw new OxIntializationException("Failed to create StringEncrypter instance");
		}
	}

	protected Properties preparePersistanceProperties() {
		PersistenceConfiguration persistenceConfiguration = this.configurationFactory.getPersistenceConfiguration();
		FileConfiguration persistenceConfig = persistenceConfiguration.getConfiguration();
		Properties connectionProperties = (Properties) persistenceConfig.getProperties();

		EncryptionService securityService = encryptionServiceInstance.get();
		Properties decryptedConnectionProperties = securityService.decryptAllProperties(connectionProperties);
		return decryptedConnectionProperties;
	}

	public void recreatePersistanceEntryManager(@Observes @LdapConfigurationReload String event) {
		recreatePersistanceEntryManagerImpl(persistenceEntryManagerInstance,
				ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
	}

	protected void recreatePersistanceEntryManagerImpl(Instance<PersistenceEntryManager> instance,
			String persistenceEntryManagerName, Annotation... qualifiers) {
		PersistenceEntryManager oldLdapEntryManager = CdiUtil.getContextBean(beanManager, PersistenceEntryManager.class,
				persistenceEntryManagerName, qualifiers);
		closePersistenceEntryManager(oldLdapEntryManager, persistenceEntryManagerName);
		PersistenceEntryManager ldapEntryManager = instance.get();
		instance.destroy(ldapEntryManager);
		logger.debug("Recreated instance {}: {} with operation service: {}", persistenceEntryManagerName,
				ldapEntryManager, ldapEntryManager.getOperationService());
	}

	private void closePersistenceEntryManager(PersistenceEntryManager oldPersistenceEntryManager,
			String persistenceEntryManagerName) {
		if ((oldPersistenceEntryManager != null) && (oldPersistenceEntryManager.getOperationService() != null)) {
			logger.debug("Attempting to destroy {}:{} with operation service: {}", persistenceEntryManagerName,
					oldPersistenceEntryManager, oldPersistenceEntryManager.getOperationService());
			oldPersistenceEntryManager.destroy();
			logger.debug("Destroyed {}:{} with operation service: {}", persistenceEntryManagerName,
					oldPersistenceEntryManager, oldPersistenceEntryManager.getOperationService());
		}
	}

}
