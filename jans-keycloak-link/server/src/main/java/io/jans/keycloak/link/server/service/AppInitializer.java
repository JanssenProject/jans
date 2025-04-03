/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.keycloak.link.server.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.jans.keycloak.link.timer.JansKeycloakLinkTimer;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

import io.jans.exception.ConfigurationException;
import io.jans.keycloak.link.service.config.ApplicationFactory;
import io.jans.keycloak.link.service.config.ConfigurationFactory;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.EncryptionService;
import io.jans.service.PythonService;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.service.document.store.manager.DocumentStoreManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.util.StringHelper;
import io.jans.util.security.SecurityProviderUtility;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * 
 * Link server initializer
 * @author Yuriy Movchan
 * @version May 12, 2020
 */
@ApplicationScoped
public class AppInitializer {

    private final static String DOCUMENT_STORE_MANAGER_JANS_KEYCLOAK_LINK_TYPE = "jans-keycloak-link"; // Module name

	@Inject
	private Logger log;

	@Inject
	private BeanManager beanManager;

	@Inject
	private Event<ApplicationInitializedEvent> eventApplicationInitialized;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	private ApplicationFactory applicationFactory;

	@Inject
	private Instance<EncryptionService> encryptionServiceInstance;

	@Inject
	private PythonService pythonService;

	@Inject
	private CustomScriptManager customScriptManager;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private QuartzSchedulerManager quartzSchedulerManager;

	@Inject
	private LoggerService loggerService;

	@Inject
	private JansKeycloakLinkTimer jansKeycloakLinkTimer;

    @Inject
    private DocumentStoreManager documentStoreManager;

	@PostConstruct
	public void createApplicationComponents() {
		try {
			SecurityProviderUtility.installBCProvider();
		} catch (ClassCastException ex) {
			log.error("Failed to install BC provider properly");
		}
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
 		log.debug("Initializing application services");

		configurationFactory.create();

		PersistenceEntryManager localPersistenceEntryManager = persistenceEntryManagerInstance.get();
		log.trace("Attempting to use {}: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
				localPersistenceEntryManager.getOperationService());

		// Initialize python interpreter
		pythonService
				.initPythonInterpreter(configurationFactory.getBaseConfiguration().getString("pythonModulesDir", null));
		
		// Initialize script manager
		List<CustomScriptType> supportedCustomScriptTypes = Lists.newArrayList(CustomScriptType.LINK_INTERCEPTION);

		// There is no Fido2 scripts yet
		//supportedCustomScriptTypes.clear();
		
		// Start timer
		initSchedulerService();

		configurationFactory.initTimer();
		loggerService.initTimer();
		customScriptManager.initTimer(supportedCustomScriptTypes);
		jansKeycloakLinkTimer.initTimer();

        // Initialize Document Store Manager
        documentStoreManager.initTimer(Arrays.asList(DOCUMENT_STORE_MANAGER_JANS_KEYCLOAK_LINK_TYPE));

		// Notify plugins about finish application initialization
		eventApplicationInitialized.select(ApplicationInitialized.Literal.APPLICATION)
				.fire(new ApplicationInitializedEvent());
	}

	protected void initSchedulerService() {
		quartzSchedulerManager.start();

		String disableScheduler = System.getProperties().getProperty("gluu.disable.scheduler");
		if ((disableScheduler != null) && Boolean.valueOf(disableScheduler)) {
			this.log.warn("Suspending Quartz Scheduler Service...");
			quartzSchedulerManager.standby();
			return;
		}
	}

	@Produces
	@ApplicationScoped
	public StringEncrypter getStringEncrypter() {
		String encodeSalt = configurationFactory.getCryptoConfigurationSalt();

		if (StringHelper.isEmpty(encodeSalt)) {
			throw new ConfigurationException("Encode salt isn't defined");
		}

		try {
			StringEncrypter stringEncrypter = StringEncrypter.instance(encodeSalt);

			return stringEncrypter;
		} catch (EncryptionException ex) {
			throw new ConfigurationException("Failed to create StringEncrypter instance");
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

	protected Properties prepareCustomPersistanceProperties(String configId) {
		Properties connectionProperties = preparePersistanceProperties();
		if (StringHelper.isNotEmpty(configId)) {
			// Replace properties names 'configId.xyz' to 'configId.xyz' in order to
			// override default values
			connectionProperties = (Properties) connectionProperties.clone();

			String baseGroup = configId + ".";
			for (Object key : connectionProperties.keySet()) {
				String propertyName = (String) key;
				if (propertyName.startsWith(baseGroup)) {
					propertyName = propertyName.substring(baseGroup.length());

					Object value = connectionProperties.get(key);
					connectionProperties.put(propertyName, value);
				}
			}
		}

		return connectionProperties;
	}

	@Produces
	@ApplicationScoped
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	public PersistenceEntryManager createPersistenceEntryManager() {
		Properties connectionProperties = preparePersistanceProperties();

		PersistenceEntryManager persistenceEntryManager = applicationFactory.getPersistenceEntryManagerFactory()
				.createEntryManager(connectionProperties);
		log.info("Created {}: {} with operation service: {}",
				new Object[] { ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME, persistenceEntryManager,
						persistenceEntryManager.getOperationService() });

		return persistenceEntryManager;
	}

	public void recreatePersistenceEntryManager(@Observes @LdapConfigurationReload String event) {
		recreatePersistanceEntryManagerImpl(persistenceEntryManagerInstance,
				ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);

		recreatePersistanceEntryManagerImpl(persistenceEntryManagerInstance,
				ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME, ReportMetric.Literal.INSTANCE);
	}

	protected void recreatePersistanceEntryManagerImpl(Instance<PersistenceEntryManager> instance,
			String persistenceEntryManagerName, Annotation... qualifiers) {
		// Get existing application scoped instance
		PersistenceEntryManager oldPersistenceEntryManager = CdiUtil.getContextBean(beanManager,
				PersistenceEntryManager.class, persistenceEntryManagerName);

		// Close existing connections
		closePersistenceEntryManager(oldPersistenceEntryManager, persistenceEntryManagerName);

		// Force to create new bean
		PersistenceEntryManager persistenceEntryManager = instance.get();
		instance.destroy(persistenceEntryManager);
		log.info("Recreated instance {}: {} with operation service: {}", persistenceEntryManagerName,
				persistenceEntryManager, persistenceEntryManager.getOperationService());
	}

	private void closePersistenceEntryManager(PersistenceEntryManager oldPersistenceEntryManager,
			String persistenceEntryManagerName) {
		// Close existing connections
		if ((oldPersistenceEntryManager != null) && (oldPersistenceEntryManager.getOperationService() != null)) {
			log.debug("Attempting to destroy {}:{} with operation service: {}", persistenceEntryManagerName,
					oldPersistenceEntryManager, oldPersistenceEntryManager.getOperationService());
			oldPersistenceEntryManager.destroy();
			log.debug("Destroyed {}:{} with operation service: {}", persistenceEntryManagerName,
					oldPersistenceEntryManager, oldPersistenceEntryManager.getOperationService());
		}
	}
}
