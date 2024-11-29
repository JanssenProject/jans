/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.service.config;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.exception.ConfigurationException;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.BaseDnConfiguration;
import io.jans.lock.model.config.Conf;
import io.jans.lock.model.config.Configuration;
import io.jans.lock.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.ApplicationConfigurationFactory;
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
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan Date: 12/12/2023
 */
@ApplicationScoped
public class ConfigurationFactory extends ApplicationConfigurationFactory {

    public final static String DOCUMENT_STORE_MANAGER_JANS_LOCK_TYPE = "jans-lock"; // Module name
    
	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private Event<AppConfiguration> configurationUpdateEvent;

	@Inject
	private Event<String> event;

	@Inject
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
	private PersistanceFactoryService persistanceFactoryService;

	@Inject
	private Instance<Configuration> configurationInstance;

	public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
	public final static String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

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
	private static final String APP_PROPERTIES_FILE = DIR + "jans-lock.properties";

	private final String SALT_FILE_NAME = "salt";

	private String confDir, saltFilePath;

	private boolean loaded = false;

	private FileConfiguration baseConfiguration;
    
    private PersistenceConfiguration persistenceConfiguration;
	private AppConfiguration dynamicConf;
	private StaticConfiguration staticConf;
	private String cryptoConfigurationSalt;

	private AtomicBoolean isActive;
	private AtomicBoolean isInitialized;

	private long baseConfigurationFileLastModifiedTime;

	private long loadedRevision = -1;
	private boolean loadedFromDB = true;

	@PostConstruct
	public void init() {
		log.info("Initializing ConfigurationFactory ...");
		this.isActive = new AtomicBoolean(true);
		this.isInitialized = new AtomicBoolean(false);
		try {
			log.info("---------PATH to file configuration: {}", APP_PROPERTIES_FILE);
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
			loadBaseConfiguration();

			this.confDir = confDir();

			String certsDir = this.baseConfiguration.getString("certsDir");
			if (StringHelper.isEmpty(certsDir)) {
				certsDir = confDir;
			}
			this.saltFilePath = confDir + SALT_FILE_NAME;

			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
	}

	public void initTimer() {
		if (this.isInitialized.compareAndSet(false, true)) {
			log.debug("Initializing Configuration Timer");
	
			final int delay = 30;
			final int interval = DEFAULT_INTERVAL;
	
			timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new ConfigurationEvent(),
					Scheduled.Literal.INSTANCE));
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
	    PersistenceConfiguration newPersistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);

		if (newPersistenceConfiguration != null) {
			if (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(), newPersistenceConfiguration.getFileName()) || (newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration.getLastModifiedTime())) {
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

		if (!loadedFromDB) {
			return;
		}
		
		reloadConfFromDB();
	}

	private boolean isRevisionIncreased() {
        final Conf conf = loadConfigurationFromDB("jansRevision");
        if (conf == null) {
            return false;
        }

        log.trace("DB revision: " + conf.getRevision() + ", server revision:" + loadedRevision);
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

	public BaseDnConfiguration getBaseDn() {
		return getStaticConfiguration().getBaseDn();
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	public boolean reloadConfFromDB() {
        if (!isRevisionIncreased()) {
            return false;
        }

        return createFromDB(false);
    }

	@Override
	protected boolean createFromDB(boolean recoverFromFiles) {
		log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
		try {
			final Conf c = loadConfigurationFromDB();
			if (c != null) {
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

        if (recoverFromFiles) {
            log.error("Unable to find configuration in DB, load configuration from file system is not implemented... ");
        }

		throw new ConfigurationException("Unable to find configuration in DB... ");
	}

	public void destroy(Class<? extends Configuration> clazz) {
		Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
		configurationInstance.destroy(confInstance.get());
	}

	private Conf loadConfigurationFromDB(String... returnAttributes) {
		final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
		final String dn = this.baseConfiguration.getString("lock_ConfigurationEntryDN");
		try {
			final Conf conf = persistenceEntryManager.find(dn, Conf.class, returnAttributes);

			return conf;
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
		if (conf.getDynamic() != null) {
			dynamicConf = conf.getDynamic();
		}
		if (conf.getStatics() != null) {
			staticConf = conf.getStatics();
		}
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
			log.error("Failed to load configuration from {}", saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			FileConfiguration fileConfiguration = new FileConfiguration(fileName);

			return fileConfiguration;
		} catch (Exception ex) {
			if (isMandatory) {
				log.error("Failed to load configuration from {}", fileName, ex);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}

}
