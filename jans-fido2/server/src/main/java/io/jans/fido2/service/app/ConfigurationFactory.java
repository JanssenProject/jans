/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.app;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jans.fido2.model.error.ErrorResponseFactory;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import io.jans.exception.ConfigurationException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Conf;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.Configuration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
public class ConfigurationFactory {

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private Event<AppConfiguration> configurationUpdateEvent;

	@Inject
	private Event<AppConfiguration> configurationReloadingEvent;

	@Inject
	private Event<String> event;

	@Inject
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
	private PersistanceFactoryService persistanceFactoryService;

	@Inject
	private Instance<Configuration> configurationInstance;

	private ErrorResponseFactory errorResponseFactory;

	public static final String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
	public static final String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

	private static final int DEFAULT_INTERVAL = 30; // 30 seconds

	static {
		if (System.getProperty("jans.base") != null) {
			BASE_DIR = System.getProperty("jans.base");
		} else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	private static final String BASE_DIR;
	private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String BASE_PROPERTIES_FILE = DIR + "jans.properties";
	private static final String APP_PROPERTIES_FILE = DIR + "jans-fido2.properties";

	private static final String SALT_FILE_NAME = "salt";

	private String saltFilePath;

	private boolean loaded = false;

	private FileConfiguration baseConfiguration;
    
    private PersistenceConfiguration persistenceConfiguration;
	private AppConfiguration dynamicConf;
	private StaticConfiguration staticConf;
	private String cryptoConfigurationSalt;

	private AtomicBoolean isActive;

	private long baseConfigurationFileLastModifiedTime;

	private long loadedRevision = -1;
	private boolean loadedFromLdap = true;

	@PostConstruct
	public void init() {
		this.isActive = new AtomicBoolean(true);
		try {
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
			loadBaseConfiguration();

			String confDir = confDir();
			log.debug("confDir: {}", confDir);

			this.saltFilePath = confDir + SALT_FILE_NAME;

			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
	}

	public void create() {
		// createFromDb() either publishes the configuration or throws, so there is no false result to handle.
		createFromDb();
		log.info("Configuration loaded successfully.");
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
		} catch (Exception ex) {
			log.error("Exception happened while reloading application configuration", ex);
		} finally {
			this.isActive.set(false);
		}
	}

	private void reloadConfiguration() {
		// Reload LDAP configuration if needed
	    PersistenceConfiguration newPersistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);

		if (newPersistenceConfiguration != null && (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(), newPersistenceConfiguration.getFileName()) || (newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration.getLastModifiedTime()))) {
			// Reload configuration only if it was modified
			this.persistenceConfiguration = newPersistenceConfiguration;
			event.select(LdapConfigurationReload.Literal.INSTANCE).fire(PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE);
		}

        // Reload Base configuration if needed
		File baseConfigFile = new File(BASE_PROPERTIES_FILE);
		if (baseConfigFile.exists()) {
			final long lastModified = baseConfigFile.lastModified();
			if (lastModified > baseConfigurationFileLastModifiedTime) {
				// Reload configuration only if it was modified
				loadBaseConfiguration();
				event.select(BaseConfigurationReload.Literal.INSTANCE).fire(BASE_CONFIGUARION_RELOAD_EVENT_TYPE);
			}
		}

		if (!loadedFromLdap) {
			return;
		}
		
		reloadConfFromLdap();
	}

	private boolean isRevisionIncreased() {
        final Conf conf = loadConfigurationFromLdap("jansRevision");
        if (conf == null) {
            return false;
        }

        log.trace("LDAP revision: {}, server revision: {}", conf.getRevision(), loadedRevision);
        return conf.getRevision() > this.loadedRevision;
    }

	private String confDir() {
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
        return persistenceConfiguration;
    }

	@Produces
	@ApplicationScoped
	public AppConfiguration getAppConfiguration() {
		return dynamicConf;
	}

	@Produces
	@ApplicationScoped
	public StaticConfiguration getStaticConfiguration() {
		return staticConf;
	}

	@Produces
	@ApplicationScoped
	public ErrorResponseFactory getFido2ErrorResponseFactory() {
		return errorResponseFactory;
	}

	public BaseDnConfiguration getBaseDn() {
		return getStaticConfiguration().getBaseDn();
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	public boolean reloadConfFromLdap() {
        if (!isRevisionIncreased()) {
            return false;
        }

        return createFromDb();
    }

	private boolean createFromDb() {
		log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
		try {
			final Conf c = loadConfigurationFromLdap();
			if (c != null) {
				// Announce the reload before the new configuration is published, so observers that cache
				// state derived from the old configuration can invalidate it first.
				if (this.loaded) {
					configurationReloadingEvent.select(ConfigurationReloading.Literal.INSTANCE).fire(dynamicConf);
				}
				init(c);

				// Destroy old configuration
				if (this.loaded) {
					destroy(AppConfiguration.class);
					destroy(StaticConfiguration.class);
				}

				this.loaded = true;
				configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(dynamicConf);

				return true;
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		throw new ConfigurationException("Unable to find configuration in DB... ");
	}

	public void destroy(Class<? extends Configuration> clazz) {
		Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
		configurationInstance.destroy(confInstance.get());
	}

	private Conf loadConfigurationFromLdap(String... returnAttributes) {
		final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
		final String dn = this.baseConfiguration.getString("fido2_ConfigurationEntryDN");
		try {
			return persistenceEntryManager.find(dn, Conf.class, returnAttributes);
		} catch (BasePersistenceException ex) {
			log.error(ex.getMessage());
		}

		return null;
	}

	private void init(Conf conf) {
		initConfigurationConf(conf);
		this.loadedRevision = conf.getRevision();
	}

	private void initConfigurationConf(Conf conf) {
		if (conf.getDynamicConf() != null) {
			dynamicConf = conf.getDynamicConf();
		}
		if (conf.getStaticConf() != null) {
			staticConf = conf.getStaticConf();
		}
		if (conf.getErrors() != null) {
			errorResponseFactory = new ErrorResponseFactory(conf.getErrors());
		}
	}

	private void loadBaseConfiguration() {
		this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);

		File baseConfigFile = new File(BASE_PROPERTIES_FILE);
		this.baseConfigurationFileLastModifiedTime = baseConfigFile.lastModified();
	}

	public void loadCryptoConfigurationSalt() {
		try {
			FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath, true);

			this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
		} catch (Exception ex) {
			log.error("Failed to load configuration from {}", saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			return new FileConfiguration(fileName);
		} catch (Exception ex) {
			if (isMandatory) {
				log.error("Failed to load configuration from {}", fileName, ex);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}


}
