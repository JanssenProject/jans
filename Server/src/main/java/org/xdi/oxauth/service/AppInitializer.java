/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.gluu.exception.ConfigurationException;
import org.gluu.model.SimpleProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.ldap.GluuLdapConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.service.JsonService;
import org.gluu.service.PythonService;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.cdi.util.CdiUtil;
import org.gluu.service.custom.lib.CustomLibrariesLoader;
import org.gluu.service.custom.script.CustomScriptManager;
import org.gluu.service.metric.inject.ReportMetric;
import org.gluu.service.timer.QuartzSchedulerManager;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.StringEncrypter;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.slf4j.Logger;
import org.xdi.oxauth.model.auth.AuthenticationMode;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.ConfigurationFactory.PersistenceConfiguration;
import org.xdi.oxauth.model.config.oxIDPAuthConf;
import org.xdi.oxauth.model.event.ApplicationInitializedEvent;
import org.xdi.oxauth.model.util.SecurityProviderUtility;
import org.xdi.oxauth.service.cdi.event.AuthConfigurationEvent;
import org.xdi.oxauth.service.cdi.event.ReloadAuthScript;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.service.logger.LoggerService;
import org.xdi.oxauth.service.status.ldap.LdapStatusTimer;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 24/10/2011
 */
@ApplicationScoped
@Named
public class AppInitializer {

	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

	@Inject
	private Logger log;

	@Inject
	private BeanManager beanManager;

	@Inject
	private Event<String> event;

	@Inject
	private Event<ApplicationInitializedEvent> eventApplicationInitialized;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_METRIC_ENTRY_MANAGER_NAME)
	@ReportMetric
	private Instance<PersistenceEntryManager> persistenceMetricEntryManagerInstance;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME)
	private Instance<List<PersistenceEntryManager>> persistenceAuthEntryManagerInstance;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_AUTH_CONFIG_NAME)
	private Instance<List<GluuLdapConfiguration>> persistenceAuthConfigInstance;

	@Inject
	private ApplicationFactory applicationFactory;

	@Inject
	private Instance<AuthenticationMode> authenticationModeInstance;

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
	private KeyGeneratorTimer keyGeneratorTimer;

	@Inject
	private CustomLibrariesLoader customLibrariesLoader;

	@Inject
	private LdapStatusTimer ldapStatusTimer;

	@Inject
	private QuartzSchedulerManager quartzSchedulerManager;

	@Inject
	private LoggerService loggerService;

	@Inject
	private JsonService jsonService;

	private AtomicBoolean isActive;
	private long lastFinishedTime;
	private AuthenticationMode authenticationMode;

	private List<GluuLdapConfiguration> persistenceAuthConfigs;

	@PostConstruct
	public void createApplicationComponents() {
		SecurityProviderUtility.installBCProvider();
	}

	public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		log.debug("Initializing application services");
		customLibrariesLoader.init();

		configurationFactory.create();

		PersistenceEntryManager localPersistenceEntryManager = persistenceEntryManagerInstance.get();
		this.persistenceAuthConfigs = loadPersistenceAuthConfigs(localPersistenceEntryManager);

		setDefaultAuthenticationMethod(localPersistenceEntryManager);

		// Initialize python interpreter
		pythonService.initPythonInterpreter(configurationFactory.getPersistenceConfiguration().getConfiguration()
				.getString("pythonModulesDir", null));

		// Initialize script manager
		List<CustomScriptType> supportedCustomScriptTypes = Arrays.asList(CustomScriptType.PERSON_AUTHENTICATION,
				CustomScriptType.CONSENT_GATHERING, CustomScriptType.CLIENT_REGISTRATION, CustomScriptType.ID_GENERATOR,
				CustomScriptType.UMA_RPT_POLICY, CustomScriptType.UMA_CLAIMS_GATHERING,
				CustomScriptType.APPLICATION_SESSION, CustomScriptType.DYNAMIC_SCOPE, CustomScriptType.INTROSPECTION,
				CustomScriptType.RESOURCE_OWNER_PASSWORD_CREDENTIALS);

		// Start timer
		initSchedulerService();

		// Schedule timer tasks
		metricService.initTimer();
		configurationFactory.initTimer();
		loggerService.initTimer();
		ldapStatusTimer.initTimer();
		cleanerTimer.initTimer();
		customScriptManager.initTimer(supportedCustomScriptTypes);
		keyGeneratorTimer.initTimer();
		initTimer();

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

	public void initTimer() {
		this.isActive = new AtomicBoolean(false);
		this.setLastFinishedTime(System.currentTimeMillis());

		timerEvent.fire(new TimerEvent(new TimerSchedule(60, DEFAULT_INTERVAL), new AuthConfigurationEvent(),
				Scheduled.Literal.INSTANCE));
	}

	@Asynchronous
	public void reloadConfigurationTimerEvent(@Observes @Scheduled AuthConfigurationEvent authConfigurationEvent) {
		if (this.isActive.get()) {
			return;
		}

		if (!this.isActive.compareAndSet(false, true)) {
			return;
		}

		try {
			reloadConfiguration();
		} catch (Throwable ex) {
			log.error("Exception happened while reloading application configuration", ex);
		} finally {
			this.isActive.set(false);
			this.setLastFinishedTime(System.currentTimeMillis());
		}
	}

	private void reloadConfiguration() {
		PersistenceEntryManager localPersistenceEntryManager = persistenceEntryManagerInstance.get();

		log.trace("Attempting to use {}: {}", ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME,
				localPersistenceEntryManager.getOperationService());
		List<GluuLdapConfiguration> newPersistenceAuthConfigs = loadPersistenceAuthConfigs(
				localPersistenceEntryManager);

		if (!this.persistenceAuthConfigs.equals(newPersistenceAuthConfigs)) {
			recreatePersistenceAuthEntryManagers(newPersistenceAuthConfigs);
			this.persistenceAuthConfigs = newPersistenceAuthConfigs;

			event.select(ReloadAuthScript.Literal.INSTANCE)
					.fire(ExternalAuthenticationService.MODIFIED_INTERNAL_TYPES_EVENT_TYPE);
		}

		setDefaultAuthenticationMethod(localPersistenceEntryManager);
	}

	/*
	 * Utility method which can be used in custom scripts
	 */
	public PersistenceEntryManager createPersistenceAuthEntryManager(GluuLdapConfiguration persistenceAuthConfig) {
		Properties persistenceConnectionProperties = prepareAuthConnectionProperties(persistenceAuthConfig);

		PersistenceEntryManager persistenceAuthEntryManager = applicationFactory.getPersistenceEntryManagerFactory()
				.createEntryManager(persistenceConnectionProperties);
		log.debug("Created custom authentication PersistenceEntryManager: {}", persistenceAuthEntryManager);

		return persistenceAuthEntryManager;
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

	@Produces
	@ApplicationScoped
	@Named(ApplicationFactory.PERSISTENCE_AUTH_CONFIG_NAME)
	public List<GluuLdapConfiguration> createPersistenceAuthConfigs() {
		return persistenceAuthConfigs;
	}

	@Produces
	@ApplicationScoped
	@Named(ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME)
	public List<PersistenceEntryManager> createPersistenceAuthEntryManager() {
		List<PersistenceEntryManager> persistenceAuthEntryManagers = new ArrayList<PersistenceEntryManager>();
		if (this.persistenceAuthConfigs.size() == 0) {
			return persistenceAuthEntryManagers;
		}

		List<Properties> persistenceAuthProperties = prepareAuthConnectionProperties(this.persistenceAuthConfigs);

		for (int i = 0; i < persistenceAuthProperties.size(); i++) {
			PersistenceEntryManager persistenceAuthEntryManager = applicationFactory.getPersistenceEntryManagerFactory()
					.createEntryManager(persistenceAuthProperties.get(i));
			log.debug("Created {}#{}: {}", new Object[] { ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME, i,
					persistenceAuthEntryManager });

			persistenceAuthEntryManagers.add(persistenceAuthEntryManager);
		}

		return persistenceAuthEntryManagers;
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

	private void closePersistenceEntryManagers(List<PersistenceEntryManager> oldPersistenceEntryManagers) {
		// Close existing connections
		for (PersistenceEntryManager oldPersistenceEntryManager : oldPersistenceEntryManagers) {
			log.debug("Attempting to destroy {}: {}", ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME,
					oldPersistenceEntryManager);
			oldPersistenceEntryManager.destroy();
			log.debug("Destroyed {}: {}", ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME,
					oldPersistenceEntryManager);
		}
	}

	public void recreatePersistenceAuthEntryManagers(List<GluuLdapConfiguration> newPersistenceAuthConfigs) {
		// Get existing application scoped instance
		List<PersistenceEntryManager> oldPersistenceAuthEntryManagers = CdiUtil.getContextBean(beanManager,
				new ParameterizedTypeImpl(List.class, PersistenceEntryManager.class),
				ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME);

		// Recreate components
		this.persistenceAuthConfigs = newPersistenceAuthConfigs;

		// Close existing connections
		closePersistenceEntryManagers(oldPersistenceAuthEntryManagers);

		// Destroy old Ldap auth entry managers
		for (PersistenceEntryManager oldPersistenceAuthEntryManager : oldPersistenceAuthEntryManagers) {
			log.debug("Attempting to destroy {}: {}", ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME,
					oldPersistenceAuthEntryManager);
			oldPersistenceAuthEntryManager.destroy();
			log.debug("Destroyed {}: {}", ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME,
					oldPersistenceAuthEntryManager);
		}

		// Force to create new Ldap auth entry managers bean
		List<PersistenceEntryManager> persistenceAuthEntryManagers = persistenceAuthEntryManagerInstance.get();
		persistenceAuthEntryManagerInstance.destroy(persistenceAuthEntryManagers);
		log.info("Recreated instance {}: {}", ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME,
				persistenceAuthEntryManagers);

		// Force to create new auth configuration bean
		List<GluuLdapConfiguration> oldPersistenceAuthConfigs = persistenceAuthConfigInstance.get();
		persistenceAuthConfigInstance.destroy(oldPersistenceAuthConfigs);
	}

	private List<Properties> prepareAuthConnectionProperties(List<GluuLdapConfiguration> persistenceAuthConfigs) {
		List<Properties> result = new ArrayList<Properties>();

		// Prepare connection providers per LDAP authentication configuration
		for (GluuLdapConfiguration persistenceAuthConfig : persistenceAuthConfigs) {
			Properties decrypytedConnectionProperties = prepareAuthConnectionProperties(persistenceAuthConfig);

			result.add(decrypytedConnectionProperties);
		}

		return result;
	}

	private Properties prepareAuthConnectionProperties(GluuLdapConfiguration persistenceAuthConfig) {
		FileConfiguration configuration = configurationFactory.getPersistenceConfiguration().getConfiguration();

		Properties properties = (Properties) configuration.getProperties().clone();
		if (persistenceAuthConfig != null) {
			properties.setProperty("servers", buildServersString(persistenceAuthConfig.getServers()));

			String bindDn = persistenceAuthConfig.getBindDN();
			if (StringHelper.isNotEmpty(bindDn)) {
				properties.setProperty("bindDN", bindDn);
				properties.setProperty("bindPassword", persistenceAuthConfig.getBindPassword());
			}
			properties.setProperty("useSSL", Boolean.toString(persistenceAuthConfig.isUseSSL()));
			properties.setProperty("maxconnections", Integer.toString(persistenceAuthConfig.getMaxConnections()));
		}

		EncryptionService securityService = encryptionServiceInstance.get();
		Properties decrypytedProperties = securityService.decryptAllProperties(properties);

		return decrypytedProperties;
	}

	private String buildServersString(List<?> servers) {
		StringBuilder sb = new StringBuilder();

		if (servers == null) {
			return sb.toString();
		}

		boolean first = true;
		for (Object server : servers) {
			if (first) {
				first = false;
			} else {
				sb.append(",");
			}

			if (server instanceof SimpleProperty) {
				sb.append(((SimpleProperty) server).getValue());
			} else {
				sb.append(server);
			}
		}

		return sb.toString();
	}

	private void setDefaultAuthenticationMethod(PersistenceEntryManager localPersistenceEntryManager) {
		String currentAuthMethod = null;
		if (this.authenticationMode != null) {
			currentAuthMethod = this.authenticationMode.getName();
		}

		String actualAuthMethod = getActualDefaultAuthenticationMethod(localPersistenceEntryManager);

		if (!StringHelper.equals(currentAuthMethod, actualAuthMethod)) {
			authenticationMode = null;
			if (actualAuthMethod != null) {
				this.authenticationMode = new AuthenticationMode(actualAuthMethod);
			}

			authenticationModeInstance.destroy(authenticationModeInstance.get());
		}
	}

	private String getActualDefaultAuthenticationMethod(PersistenceEntryManager localPersistenceEntryManager) {
		GluuConfiguration configuration = loadConfiguration(localPersistenceEntryManager, "oxAuthenticationMode");

		if (configuration == null) {
			return null;
		}

		return configuration.getAuthenticationMode();
	}

	@Produces
	@ApplicationScoped
	public AuthenticationMode getDefaultAuthenticationMode() {
		return authenticationMode;
	}

	private GluuConfiguration loadConfiguration(PersistenceEntryManager localPersistenceEntryManager,
			String... persistenceReturnAttributes) {
		String configurationDn = configurationFactory.getBaseDn().getConfiguration();
		if (StringHelper.isEmpty(configurationDn)) {
			return null;
		}

		GluuConfiguration configuration = null;
		try {
			configuration = localPersistenceEntryManager.find(GluuConfiguration.class, configurationDn,
					persistenceReturnAttributes);
		} catch (BasePersistenceException ex) {
			log.error("Failed to load global configuration entry from Ldap", ex);
			return null;
		}

		return configuration;
	}

	private List<GluuLdapConfiguration> loadPersistenceAuthConfigs(
			PersistenceEntryManager localPersistenceEntryManager) {
		List<GluuLdapConfiguration> persistenceAuthConfigs = new ArrayList<GluuLdapConfiguration>();

		List<oxIDPAuthConf> persistenceIdpAuthConfigs = loadLdapIdpAuthConfigs(localPersistenceEntryManager);
		if (persistenceIdpAuthConfigs == null) {
			return persistenceAuthConfigs;
		}

		for (oxIDPAuthConf persistenceIdpAuthConfig : persistenceIdpAuthConfigs) {
			GluuLdapConfiguration persistenceAuthConfig = loadPersistenceAuthConfig(persistenceIdpAuthConfig);
			if ((persistenceAuthConfig != null) && persistenceAuthConfig.isEnabled()) {
				persistenceAuthConfigs.add(persistenceAuthConfig);
			}
		}

		return persistenceAuthConfigs;
	}

	private List<oxIDPAuthConf> loadLdapIdpAuthConfigs(PersistenceEntryManager localPersistenceEntryManager) {
		GluuConfiguration configuration = loadConfiguration(localPersistenceEntryManager, "oxIDPAuthentication");

		if ((configuration == null) || (configuration.getOxIDPAuthentication() == null)) {
			return null;
		}

		List<oxIDPAuthConf> configurations = new ArrayList<oxIDPAuthConf>();
		for (String configurationJson : configuration.getOxIDPAuthentication()) {

			try {
				oxIDPAuthConf authConf = jsonService.jsonToObject(configurationJson, oxIDPAuthConf.class);
				if (authConf.getType().equalsIgnoreCase("ldap")
						|| authConf.getType().equalsIgnoreCase("auth")) {
					configurations.add(authConf);
				}
			} catch (Exception ex) {
				log.error("Failed to create object by json: '{}'", configurationJson, ex);
			}
		}

		return configurations;
	}

	private GluuLdapConfiguration loadPersistenceAuthConfig(oxIDPAuthConf configuration) {
		if (configuration == null) {
			return null;
		}

		try {
			if (configuration.getType().equalsIgnoreCase("auth")) {
				return jsonService.jsonToObject(configuration.getConfig(), GluuLdapConfiguration.class);
			}
		} catch (Exception ex) {
			log.error("Failed to create object by oxIDPAuthConf: '{}'", configuration, ex);
		}

		return null;
	}

	public void destroy(@Observes @BeforeDestroyed(ApplicationScoped.class) ServletContext init) {
		log.info("Closing LDAP connection at server shutdown...");
		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
		closePersistenceEntryManager(persistenceEntryManager, ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME);

		List<PersistenceEntryManager> persistenceAuthEntryManagers = persistenceAuthEntryManagerInstance.get();
		closePersistenceEntryManagers(persistenceAuthEntryManagers);
	}

	public long getLastFinishedTime() {
		return lastFinishedTime;
	}

	public void setLastFinishedTime(long lastFinishedTime) {
		this.lastFinishedTime = lastFinishedTime;
	}

}