/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.conf.service;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.gluu.conf.model.AppConfiguration;
import org.gluu.conf.model.AppConfigurationEntry;
import org.gluu.conf.model.SharedConfigurationEntry;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.persist.service.StandalonePersistanceFactoryService;
import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.cache.InMemoryConfiguration;
import org.gluu.util.StringHelper;
import org.gluu.util.exception.ConfigurationException;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;
import org.gluu.util.security.StringEncrypter;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base OpenId configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 11/02/2015
 */
public abstract class ConfigurationFactory<C extends AppConfiguration, L extends AppConfigurationEntry> {

	private final Logger LOG = LoggerFactory.getLogger(ConfigurationFactory.class);

	static {
		if (System.getProperty("gluu.base") != null) {
			BASE_DIR = System.getProperty("gluu.base");
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

    public static final String BASE_DIR;
    public static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String BASE_PROPERTIES_FILE = DIR + "gluu.properties";
    public static final String DEFAULT_PROPERTIES_FILE = DIR + "openid.properties";

	private static final String SALT_FILE_NAME = "salt";

	private static final String SHARED_CONFIGURATION_DN = "ou=configuration,o=gluu";

	private String confDir, saltFilePath;

	private FileConfiguration baseConfiguration;

	private C appConfiguration;

	private PersistanceFactoryService persistanceFactoryService;
	private PersistenceConfiguration persistenceConfiguration;

	private String cryptoConfigurationSalt;

	private PersistenceEntryManager persistenceEntryManager;

	@SuppressWarnings("unused")
	private long baseConfigurationFileLastModifiedTime;

	private boolean loaded = false;
	private long loadedRevision = -1;

	private AtomicBoolean isActive;

	protected ConfigurationFactory() {
		this.isActive = new AtomicBoolean(true);
		try {
			create();
		} finally {
			this.isActive.set(false);
		}
	}

	private void create() {
		this.persistanceFactoryService = new StandalonePersistanceFactoryService();

        this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(getDefaultConfigurationFileName());
        this.baseConfiguration = loadBaseConfiguration();

		this.confDir = confDir();
		this.saltFilePath = confDir + SALT_FILE_NAME;

		this.cryptoConfigurationSalt = loadCryptoConfigurationSalt();

		this.persistenceEntryManager = createPersistenceEntryManager();

		if (!createFromDb()) {
			LOG.error("Failed to load configuration from DB. Please fix it!!!.");
			throw new ConfigurationException("Failed to load configuration from DB.");
		} else {
			this.loaded = true;
			LOG.info("Configuration loaded successfully.");
		}
	}

	public void destroy() {
		if (this.persistenceEntryManager != null) {
			destroyPersistenceEntryManager(this.persistenceEntryManager);
		}
	}

	private FileConfiguration loadBaseConfiguration() {
		FileConfiguration fileConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
		
		return fileConfiguration;
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			FileConfiguration fileConfiguration = new FileConfiguration(fileName);

			return fileConfiguration;
		} catch (Exception ex) {
			if (isMandatory) {
				LOG.error("Failed to load configuration from {}", fileName, ex);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}

	public FileConfiguration loadPersistanceConfiguration(String persistanceConfigurationFileName, boolean mandatory) {
		try {
			if (StringHelper.isEmpty(persistanceConfigurationFileName)) {
				if (mandatory) {
					throw new ConfigurationException("Failed to load Persistance configuration file!");
				} else {
					return null;
				}
			}

			String persistanceConfigurationFilePath = DIR + persistanceConfigurationFileName;

			FileConfiguration persistanceConfiguration = new FileConfiguration(persistanceConfigurationFilePath);
			if (persistanceConfiguration.isLoaded()) {
				File persistanceFile = new File(persistanceConfigurationFilePath);
				if (persistanceFile.exists()) {
					this.baseConfigurationFileLastModifiedTime = persistanceFile.lastModified();
				}
	
				return persistanceConfiguration;
			}
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			throw new ConfigurationException("Failed to load DB configuration from " + persistanceConfigurationFileName, ex);
		}

		if (mandatory) {
			throw new ConfigurationException("Failed to load DB configuration from " + persistanceConfigurationFileName);
		}
		
		return null;
	}

	private String loadCryptoConfigurationSalt() {
		try {
			FileConfiguration cryptoConfiguration = new FileConfiguration(this.saltFilePath);

			return cryptoConfiguration.getString("encodeSalt");
		} catch (Exception ex) {
			LOG.error("Failed to load configuration from {}", saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private String confDir() {
		final String confDir = getPersistenceConfiguration().getString("confDir");
		if (StringUtils.isNotBlank(confDir)) {
			return confDir;
		}

		return DIR;
	}

	private boolean createFromDb() {
		LOG.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
		try {
			final L persistenceConf = loadConfigurationFromDb();
			this.loadedRevision = persistenceConf.getRevision();
			if (persistenceConf != null) {
				this.appConfiguration = (C) persistenceConf.getApplication();
				return true;
			}
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
		}

		return false;
	}

	private L loadConfigurationFromDb(String... returnAttributes) {
		try {
			final String dn = baseConfiguration.getString(getApplicationConfigurationPropertyName());

			final L persistanceConf = this.persistenceEntryManager.find(dn, getAppConfigurationType(), returnAttributes);
			return persistanceConf;
		} catch (BasePersistenceException ex) {
			LOG.error(ex.getMessage());
		}

		return null;
	}

	protected Properties preparePersistanceProperties() {
		FileConfiguration persistenceConfig = persistenceConfiguration.getConfiguration();
		Properties connectionProperties = (Properties) persistenceConfig.getProperties();

		Properties decryptedConnectionProperties;
		try {
			decryptedConnectionProperties = PropertiesDecrypter.decryptAllProperties(StringEncrypter.defaultInstance(), connectionProperties, this.cryptoConfigurationSalt);
        } catch (EncryptionException ex) {
        	throw new ConfigurationException("Failed to decript configuration properties", ex);
        }

		return decryptedConnectionProperties;
	}

	public PersistenceEntryManager createPersistenceEntryManager() {
		Properties connectionProperties = preparePersistanceProperties();

		PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConfiguration);
		PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerFactory.createEntryManager(connectionProperties);
		LOG.info("Created PersistenceEntryManager: {} with operation service: {}",
				new Object[] {persistenceEntryManager,
						persistenceEntryManager.getOperationService() });

		return persistenceEntryManager;
	}

	private void destroyPersistenceEntryManager(final PersistenceEntryManager persistenceEntryManager) {
		boolean result = persistenceEntryManager.destroy();
		if (result) {
			LOG.debug("Destoyed PersistenceEntryManager: {}", persistenceEntryManager);
		} else {
			LOG.error("Failed to destoy PersistenceEntryManager: {}", persistenceEntryManager);
		}
	}

	public FileConfiguration getPersistenceConfiguration() {
		return this.persistenceConfiguration.getConfiguration();
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	protected String getDefaultPersistanceConfigurationFileName() {
		return "gluu-ldap.properties";
	}

	public C getAppConfiguration() {
		return appConfiguration;
	}

	public CacheConfiguration getCacheConfiguration() {
		SharedConfigurationEntry sharedConfigurationEntry = persistenceEntryManager.find(SharedConfigurationEntry.class, SHARED_CONFIGURATION_DN);
		if (sharedConfigurationEntry == null) {
			LOG.error("Failed to load share configuration from DB. Please fix it!!!.");
			throw new ConfigurationException("Failed to load shared configuration from DB.");
		}

		CacheConfiguration cacheConfiguration = sharedConfigurationEntry.getCacheConfiguration();
		if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
			LOG.error("Failed to read cache configuration from DB. Please check configuration oxCacheConfiguration attribute " +
					"that must contain cache configuration JSON represented by CacheConfiguration.class. Shared configuration DN: " + SHARED_CONFIGURATION_DN);
			LOG.info("Creating fallback IN-MEMORY cache configuration ... ");

			cacheConfiguration = new CacheConfiguration();
			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

			LOG.info("IN-MEMORY cache configuration is created.");
		}
		LOG.info("Cache configuration: " + cacheConfiguration);
		return cacheConfiguration;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public long getLoadedRevision() {
		return loadedRevision;
	}

	protected abstract String getDefaultConfigurationFileName();

	protected abstract Class<L> getAppConfigurationType();

	protected abstract String getApplicationConfigurationPropertyName();

}
