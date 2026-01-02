/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import com.google.common.collect.Lists;
import io.jans.service.timer.QuartzSchedulerManager;
import io.jans.exception.ConfigurationException;
import io.jans.fido2.service.shared.LoggerService;
import io.jans.fido2.service.shared.MetricService;
import io.jans.model.custom.script.CustomScriptType;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.service.EncryptionService;
import io.jans.service.PythonService;
import io.jans.service.cdi.event.ApplicationInitialized;
import io.jans.service.cdi.event.ApplicationInitializedEvent;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.service.metric.inject.ReportMetric;
import io.jans.util.StringHelper;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.security.SecurityProviderUtility;
import io.jans.util.security.StringEncrypter;
import io.jans.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Properties;

/**
 * 
 * FIDO2 server initializer
 * @author Yuriy MOvchan
 * @version May 12, 2020
 */
@ApplicationScoped
@Named
public class AppInitializer {

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
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	@ReportMetric
	private Instance<PersistenceEntryManager> persistenceMetricEntryManagerInstance;

	@Inject
	private ApplicationFactory applicationFactory;

	@Inject
	private Instance<EncryptionService> encryptionServiceInstance;

	@Inject
	private PythonService pythonService;

	@Inject
	private MetricService metricService;

	@Inject
	private CustomScriptManager customScriptManager;

	@Inject
	private ConfigurationFactory configurationFactory;

	@Inject
	private CleanerTimer cleanerTimer;

	@Inject
	private QuartzSchedulerManager quartzSchedulerManager;

	@Inject
	private LoggerService loggerService;

	@Inject
	private MDS3UpdateTimer mds3UpdateTimer;

	@Inject
	private Instance<io.jans.fido2.service.metric.Fido2MetricsAggregationScheduler> fido2MetricsAggregationSchedulerInstance;

	@PostConstruct
	public void createApplicationComponents() {
		try {
			SecurityProviderUtility.installBCProvider();
		} catch (ClassCastException ex) {
			log.error("Failed to install BC provider properly");
		}
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		log.info("=== FIDO2 Application Initialization Started ===");
		log.debug("Initializing application services");

		try {
			configurationFactory.create();
			log.info("Configuration factory created successfully");
		} catch (Exception e) {
			log.error("Failed to create configuration factory: {}", e.getMessage(), e);
			return;
		}

		try {
			PersistenceEntryManager localPersistenceEntryManager = persistenceEntryManagerInstance.get();
			log.trace("Attempting to use {}: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
					localPersistenceEntryManager.getOperationService());
			log.info("Persistence entry manager initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize persistence entry manager: {}", e.getMessage(), e);
			return;
		}

		// Initialize python interpreter
		try {
			pythonService
					.initPythonInterpreter(configurationFactory.getBaseConfiguration().getString("pythonModulesDir", null));
			log.info("Python interpreter initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize Python interpreter: {}", e.getMessage(), e);
			// Continue anyway - Python might not be critical
		}

		// Initialize script manager
		List<CustomScriptType> supportedCustomScriptTypes = Lists.newArrayList(CustomScriptType.FIDO2_EXTENSION);

		// Start timer
		try {
			initSchedulerService();
			log.info("Scheduler service initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize scheduler service: {}", e.getMessage(), e);
			return;
		}

		// Schedule timer tasks
		try {
			metricService.initTimer();
			log.info("Metric service timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize metric service timer: {}", e.getMessage(), e);
		}
		
		try {
			configurationFactory.initTimer();
			log.info("Configuration factory timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize configuration factory timer: {}", e.getMessage(), e);
		}
		
		try {
			loggerService.initTimer(true);
			log.info("Logger service timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize logger service timer: {}", e.getMessage(), e);
		}
		
		try {
			cleanerTimer.initTimer();
			log.info("Cleaner timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize cleaner timer: {}", e.getMessage(), e);
		}
		
		try {
			mds3UpdateTimer.initTimer();
			log.info("MDS3 update timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize MDS3 update timer: {}", e.getMessage(), e);
		}
		
		try {
			customScriptManager.initTimer(supportedCustomScriptTypes);
			log.info("Custom script manager timer initialized");
		} catch (Exception e) {
			log.error("Failed to initialize custom script manager timer: {}", e.getMessage(), e);
		}
		
		// Initialize FIDO2 metrics aggregation scheduler (optional - might not be available)
		try {
			if (fido2MetricsAggregationSchedulerInstance != null && !fido2MetricsAggregationSchedulerInstance.isUnsatisfied()) {
				fido2MetricsAggregationSchedulerInstance.get().initTimer();
				log.info("FIDO2 metrics aggregation scheduler initialized");
			} else {
				log.info("FIDO2 metrics aggregation scheduler not available, skipping initialization");
			}
		} catch (Exception e) {
			log.warn("Failed to initialize FIDO2 metrics aggregation scheduler: {}", e.getMessage(), e);
		}

		// Notify plugins about finish application initialization
		try {
			eventApplicationInitialized.select(ApplicationInitialized.Literal.APPLICATION)
					.fire(new ApplicationInitializedEvent());
			log.info("Application initialization event fired successfully");
		} catch (Exception e) {
			log.error("Failed to fire application initialized event: {}", e.getMessage(), e);
		}
		
		log.info("=== FIDO2 Application Initialization Completed Successfully ===");
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

	@Produces
	@ApplicationScoped
	@Named(ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
	@ReportMetric
	public PersistenceEntryManager createMetricPersistenceEntryManager() {
		Properties connectionProperties = prepareCustomPersistanceProperties(
				ApplicationFactory.PERSISTENCE_METRIC_CONFIG_GROUP_NAME);

		PersistenceEntryManager persistenceEntryManager = applicationFactory.getPersistenceEntryManagerFactory()
				.createEntryManager(connectionProperties);
		log.info("Created {}: {} with operation service: {}",
				new Object[] { ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME, persistenceEntryManager,
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

	public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
		log.info("Stopping services and closing DB connections at server shutdown...");
		log.debug("Checking who intiated destroy", new Throwable());

		metricService.close();

		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
		closePersistenceEntryManager(persistenceEntryManager, ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);
	}

}