package org.gluu.oxauthconfigapi.configuration;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.config.oxtrust.Configuration;
import org.gluu.config.oxtrust.LdapOxTrustConfiguration;
import org.gluu.exception.ConfigurationException;
import org.gluu.oxtrust.service.ApplicationFactory;
import org.gluu.oxtrust.service.custom.LdapCentralConfigurationReload;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.JsonService;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.service.cdi.event.ApplicationInitializedEvent;
import org.gluu.service.cdi.event.BaseConfigurationReload;
import org.gluu.service.cdi.event.ConfigurationEvent;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;

import io.quarkus.arc.AlternativePriority;

@ApplicationScoped
@AlternativePriority(1)
public class ConfigurationFactory extends org.gluu.oxtrust.config.ConfigurationFactory {

	@Inject
	Logger log;

	@Inject
	JsonService jsonService;

	@Inject
	Event<TimerEvent> timerEvent;

	@Inject
	Event<AppConfiguration> configurationUpdateEvent;

	@Inject
	Event<String> event;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	PersistanceFactoryService persistanceFactoryService;

	private AppConfiguration appConfiguration;

	@Inject
	Instance<Configuration> configurationInstance;

	public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
	public final static String PERSISTENCE_CENTRAL_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceCentralConfigurationReloadEvent";
	public final static String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

	static {
		if (System.getProperty("gluu.base") != null) {
			BASE_DIR = System.getProperty("gluu.base");
		} else if ((System.getProperty("catalina.base") != null)
				&& (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	public static final String BASE_DIR;
	public static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String BASE_PROPERTIES_FILE = DIR + "gluu.properties";
	public static final String LDAP_PROPERTIES_FILE = DIR + "oxtrust.properties";
	public static final String APPLICATION_CONFIGURATION = "oxtrust-config.json";
	public static final String SALT_FILE_NAME = "salt";
	private String confDir, configFilePath, saltFilePath;
	private boolean loaded = false;
	private FileConfiguration baseConfiguration;
	private PersistenceConfiguration persistenceConfiguration;
	FileConfiguration ldapCentralConfiguration;
	String cryptoConfigurationSalt;
	AtomicBoolean isActive;
	private long baseConfigurationFileLastModifiedTime = -1;
	private long loadedRevision = -1;
	private boolean loadedFromLdap = true;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) ApplicationInitializedEvent init) {
		log.info("=============================Initializaing Config factory");
		init();
	}

	@Override
	protected void initInternal() {
		this.isActive = new AtomicBoolean(true);
		try {
			loadBaseConfiguration();
			this.persistenceConfiguration = persistanceFactoryService
					.loadPersistenceConfiguration(LDAP_PROPERTIES_FILE);
			this.confDir = confDir();
			this.configFilePath = confDir + APPLICATION_CONFIGURATION;
			this.saltFilePath = confDir + SALT_FILE_NAME;
			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
	}

	public void create() {
		init();
		if (!createFromLdap(true)) {
			log.error("Failed to load configuration from LDAP. Please fix it!!!.");
			throw new ConfigurationException("Failed to load configuration from LDAP.");
		} else {
			log.info("Configuration loaded successfully.");
		}
	}

	@Asynchronous
	public void reloadConfigurationTimerEvent(@Observes @Scheduled ConfigurationEvent configurationEvent) {
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
		}
	}

	private void reloadConfiguration() {
		// Reload LDAP configuration if needed
		PersistenceConfiguration newPersistenceConfiguration = persistanceFactoryService
				.loadPersistenceConfiguration(LDAP_PROPERTIES_FILE);

		if (newPersistenceConfiguration != null) {
			if (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(),
					newPersistenceConfiguration.getFileName())
					|| (newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration
							.getLastModifiedTime())) {
				// Reload configuration only if it was modified
				this.persistenceConfiguration = newPersistenceConfiguration;
				event.select(LdapConfigurationReload.Literal.INSTANCE).fire(PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE);
			}
		}

		// Reload Base configuration if needed
		File baseConfiguration = new File(BASE_PROPERTIES_FILE);
		if (baseConfiguration.exists()) {
			final long lastModified = baseConfiguration.lastModified();
			if (lastModified > baseConfigurationFileLastModifiedTime) {
				// Reload configuration only if it was modified
				loadBaseConfiguration();
				event.select(BaseConfigurationReload.Literal.INSTANCE).fire(BASE_CONFIGUARION_RELOAD_EVENT_TYPE);
			}
		}

		if (this.ldapCentralConfiguration != null) {
			// Allow to remove not mandatory configuration file
			this.ldapCentralConfiguration = null;
			event.select(LdapCentralConfigurationReload.Literal.INSTANCE)
					.fire(PERSISTENCE_CENTRAL_CONFIGUARION_RELOAD_EVENT_TYPE);
		}

		if (!loadedFromLdap) {
			return;
		}

		final LdapOxTrustConfiguration conf = loadConfigurationFromLdap("oxRevision");
		if (conf == null) {
			return;
		}

		if (conf.getRevision() <= this.loadedRevision) {
			return;
		}
		createFromLdap(false);
	}

	public String confDir() {
		final String confDir = this.baseConfiguration.getString("confDir", null);
		if (StringUtils.isNotBlank(confDir)) {
			return confDir;
		}
		return DIR;
	}

	public FileConfiguration getBaseConfiguration() {
		return baseConfiguration;
	}

	@Produces
	@ApplicationScoped
	public PersistenceConfiguration getPersistenceConfiguration() {
		log.info("Returning PersistenceConfiguration " + persistenceConfiguration);
		return persistenceConfiguration;
	}

	@Produces
	@ApplicationScoped
	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	public FileConfiguration getLdapCentralConfiguration() {
		return ldapCentralConfiguration;
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	public String getConfigurationDn() {
		return this.baseConfiguration.getString("oxtrust_ConfigurationEntryDN");
	}

	private boolean createFromFile() {
		return reloadAppConfFromFile();
	}

	private boolean reloadAppConfFromFile() {
		final AppConfiguration appConfiguration = loadAppConfFromFile();
		if (appConfiguration != null) {
			log.info("Reloaded application configuration from file: " + configFilePath);
			return true;
		} else {
			log.error("Failed to load application configuration from file: " + configFilePath);
		}
		return false;
	}

	private AppConfiguration loadAppConfFromFile() {
		try {
			String jsonConfig = FileUtils.readFileToString(new File(configFilePath), "UTF-8");
			AppConfiguration appConfiguration = jsonService.jsonToObject(jsonConfig, AppConfiguration.class);
			return appConfiguration;
		} catch (Exception ex) {
			log.error("Failed to load configuration from {}", configFilePath, ex);
		}
		return null;
	}

	private void loadBaseConfiguration() {
		this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
		File baseConfiguration = new File(BASE_PROPERTIES_FILE);
		this.baseConfigurationFileLastModifiedTime = baseConfiguration.lastModified();
	}

	public void loadCryptoConfigurationSalt() {
		try {
			FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath, true);

			this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
		} catch (Exception ex) {
			log.error("Failed to load configuration from {}", this.saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + this.saltFilePath, ex);
		}
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			FileConfiguration fileConfiguration = new FileConfiguration(fileName);
			if (fileConfiguration.isLoaded()) {
				return fileConfiguration;
			}
		} catch (Exception ex) {
			if (isMandatory) {
				log.error("Failed to load configuration from {}", fileName, ex);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}

	private boolean createFromLdap(boolean recoverFromFiles) {
		log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
		try {
			final LdapOxTrustConfiguration conf = loadConfigurationFromLdap();
			if (conf != null) {
				init(conf);
				if (this.loaded) {
					destroy(AppConfiguration.class);
				}
				this.loaded = true;
				return true;
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		if (recoverFromFiles) {
			log.warn("Unable to find configuration in LDAP, try to load configuration from file system... ");
			if (createFromFile()) {
				this.loadedFromLdap = false;
				return true;
			}
		}
		return false;
	}

	public void destroy(Class<? extends Configuration> clazz) {
		Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
		configurationInstance.destroy(confInstance.get());
	}

	public LdapOxTrustConfiguration loadConfigurationFromLdap(String... returnAttributes) {
		try {
			final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
			final String configurationDn = getConfigurationDn();
			final LdapOxTrustConfiguration conf = persistenceEntryManager.find(configurationDn,
					LdapOxTrustConfiguration.class, returnAttributes);
			return conf;
		} catch (BasePersistenceException ex) {
			log.error("Failed to load configuration from LDAP", ex);
		}
		return null;
	}

	private void init(LdapOxTrustConfiguration conf) {
		this.appConfiguration = conf.getApplication();
	}

	public String getIDPTemplatesLocation() {
		String jetyBase = System.getProperty("jetty.base");
		if (StringHelper.isEmpty(jetyBase)) {
			return ConfigurationFactory.DIR;
		}
		return jetyBase + File.separator + "conf" + File.separator;
	}

}
