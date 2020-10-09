/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service.config;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.gluu.config.oxtrust.AttributeResolverConfiguration;
import org.gluu.config.oxtrust.Configuration;
import org.gluu.exception.ConfigurationException;
import org.gluu.oxtrust.service.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.AppConfigurationReloadEvent;
import org.gluu.service.cdi.event.ApplicationInitialized;
import org.gluu.service.cdi.event.ApplicationInitializedEvent;
import org.gluu.service.cdi.event.BaseConfigurationReload;
import org.gluu.service.cdi.event.ConfigurationEvent;
import org.gluu.service.cdi.event.ConfigurationUpdate;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.gluu.util.StringHelper;
import org.gluu.util.init.Initializable;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version 0.1, 05/15/2013
 */
public abstract class ConfigurationFactory<C> extends Initializable {

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private Event<AppConfigurationReloadEvent> configurationUpdateEvent;

	@Inject
	private Event<String> event;

	@Inject
	@Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	@Inject
	private PersistanceFactoryService persistanceFactoryService;

	@Inject
	private Instance<Configuration> configurationInstance;

	public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
	public final static String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

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
	public static final String APP_PROPERTIES_FILE = DIR + "oxtrust.properties";

	public static final String APPLICATION_CONFIGURATION = "oxtrust-config.json";
	public static final String CACHE_PROPERTIES_FILE = "oxTrustCacheRefresh.properties";
	public static final String LOG_ROTATION_CONFIGURATION = "oxTrustLogRotationConfiguration.xml";
	public static final String SALT_FILE_NAME = "salt";

	private String confDir, configFilePath, cacheRefreshFilePath, logRotationFilePath, saltFilePath;

	private boolean loaded = false;

	protected FileConfiguration baseConfiguration;

	private PersistenceConfiguration persistenceConfiguration;

	protected AttributeResolverConfiguration attributeResolverConfiguration;
	private String cryptoConfigurationSalt;

	private AtomicBoolean isActive;

	private long baseConfigurationFileLastModifiedTime = -1;

	private boolean loadedFromLdap = true;

	public void init(@Observes @ApplicationInitialized(ApplicationScoped.class) ApplicationInitializedEvent init) {
		init();
	}

	@Override
	protected void initInternal() {
		this.isActive = new AtomicBoolean(true);
		try {
			log.info("Creating oxTrustConfiguration");
			loadBaseConfiguration();

			this.persistenceConfiguration = persistanceFactoryService
					.loadPersistenceConfiguration(getApplicationPropertiesFileName());
			this.confDir = confDir();
			this.configFilePath = confDir + APPLICATION_CONFIGURATION;
			this.cacheRefreshFilePath = confDir + CACHE_PROPERTIES_FILE;
			this.logRotationFilePath = confDir + LOG_ROTATION_CONFIGURATION;
			this.saltFilePath = confDir + SALT_FILE_NAME;

			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
	}

	public void create() {
		init();
		if (!createFromDb()) {
			log.error("Failed to load configuration from LDAP. Please fix it!!!.");
			throw new ConfigurationException("Failed to load configuration from LDAP.");
		} else {
			log.info("Configuration loaded successfully.");
		}
	}

	public void initTimer() {
		log.debug("Initializing Configuration Timer");

		final int delay = 30;
		final int interval = DEFAULT_INTERVAL;

		timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new ConfigurationEvent(),
				Scheduled.Literal.INSTANCE));
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
				.loadPersistenceConfiguration(getApplicationPropertiesFileName());

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

		if (!loadedFromLdap) {
			return;
		}

		final C conf = loadConfigurationFromDb("oxRevision");
		if (conf == null) {
			return;
		}

		if (!isNewRevision(conf)) {
			return;
		}

		createFromDb();
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

	public PersistenceConfiguration getPersistenceConfiguration() {
		return persistenceConfiguration;
	}

	public AttributeResolverConfiguration getAttributeResolverConfiguration() {
		return attributeResolverConfiguration;
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
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
				log.debug("########## fileName = " + fileConfiguration.getFileName());
				log.debug("########## oxtrust_ConfigurationEntryDN = "
						+ fileConfiguration.getString("oxtrust_ConfigurationEntryDN"));
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

	private boolean createFromDb() {
		log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
		try {
			final C conf = loadConfigurationFromDb();
			if (conf != null) {
				init(conf);

				// Destroy old configuration
				if (this.loaded) {
					destroryLoadedConfiguration();
				}

				this.loaded = true;
				configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(new AppConfigurationReloadEvent());

				return true;
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		return false;
	}

	protected void destroy(Class<? extends Configuration> clazz) {
		Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
		configurationInstance.destroy(confInstance.get());
	}

	public abstract String getConfigurationDn();

	protected abstract String getApplicationPropertiesFileName();

	protected abstract void init(C conf);
	protected abstract C loadConfigurationFromDb(String... returnAttributes);
	protected abstract void destroryLoadedConfiguration();

	protected abstract boolean isNewRevision(C c);
}
