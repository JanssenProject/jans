/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.conf.service;

import io.jans.conf.model.AppConfiguration;
import io.jans.conf.model.SharedConfigurationEntry;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.service.StandalonePersistanceFactoryService;
import io.jans.util.StringHelper;
import io.jans.util.exception.ConfigurationException;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang3.StringUtils;
import io.jans.conf.model.AppConfigurationEntry;
import io.jans.service.cache.CacheConfiguration;
import io.jans.service.cache.InMemoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base OpenId configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 11/02/2015
 */
public abstract class ConfigurationFactory<C extends AppConfiguration, L extends AppConfigurationEntry> {

	private final Logger LOG = LoggerFactory.getLogger(ConfigurationFactory.class);

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

    public static final String BASE_DIR;
    public static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String BASE_PROPERTIES_FILE = DIR + "jans.properties";
    public static final String DEFAULT_PROPERTIES_FILE = DIR + "openid.properties";

	private static final String SALT_FILE_NAME = "salt";

	private static final String SHARED_CONFIGURATION_DN = "ou=configuration,o=jans";

	private String confDir, saltFilePath;

	private FileConfiguration baseConfiguration;

	private C appConfiguration;

	private PersistanceFactoryService persistanceFactoryService;
	private PersistenceConfiguration persistenceConfiguration;

	private String cryptoConfigurationSalt;
	private StringEncrypter stringEncrypter;

	private PersistenceEntryManager persistenceEntryManager;

	private CacheConfiguration cacheConfiguration;

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
		this.stringEncrypter = createStringEncrypter();

		this.persistenceEntryManager = createPersistenceEntryManager();

		if (!createFromDb()) {
			LOG.error("Failed to load configuration from DB. Please fix it!!!.");
			throw new ConfigurationException("Failed to load configuration from DB.");
		} else {
			this.loaded = true;
			LOG.info("Configuration loaded successfully.");
		}
		
		this.cacheConfiguration = loadCacheConfiguration();
	}

	public void destroy() {
		if (this.persistenceEntryManager != null) {
			destroyPersistenceEntryManager(this.persistenceEntryManager);
		}
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

	private FileConfiguration loadBaseConfiguration() {
		FileConfiguration fileConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
		
		return fileConfiguration;
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

	private CacheConfiguration loadCacheConfiguration() {
		SharedConfigurationEntry sharedConfigurationEntry = persistenceEntryManager.find(SharedConfigurationEntry.class, SHARED_CONFIGURATION_DN);
		if (sharedConfigurationEntry == null) {
			LOG.error("Failed to load share configuration from DB. Please fix it!!!.");
			throw new ConfigurationException("Failed to load shared configuration from DB.");
		}

		CacheConfiguration cacheConfiguration = sharedConfigurationEntry.getCacheConfiguration();
		if (cacheConfiguration == null || cacheConfiguration.getCacheProviderType() == null) {
			LOG.error("Failed to read cache configuration from DB. Please check configuration jsCacheConf attribute " +
					"that must contain cache configuration JSON represented by CacheConfiguration.class. Shared configuration DN: " + SHARED_CONFIGURATION_DN);
			LOG.info("Creating fallback IN-MEMORY cache configuration ... ");

			cacheConfiguration = new CacheConfiguration();
			cacheConfiguration.setInMemoryConfiguration(new InMemoryConfiguration());

			LOG.info("IN-MEMORY cache configuration is created.");
		}
		LOG.info("Cache configuration: " + cacheConfiguration);

		return cacheConfiguration;
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
        } catch (StringEncrypter.EncryptionException ex) {
        	throw new ConfigurationException("Failed to decript configuration properties", ex);
        }

		return decryptedConnectionProperties;
	}

	private PersistenceEntryManager createPersistenceEntryManager() {
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

	private StringEncrypter createStringEncrypter() {
		String encodeSalt = this.cryptoConfigurationSalt;

		if (StringHelper.isEmpty(encodeSalt)) {
			throw new ConfigurationException("Encode salt isn't defined");
		}

		try {
			StringEncrypter stringEncrypter = StringEncrypter.instance(encodeSalt);

			return stringEncrypter;
		} catch (StringEncrypter.EncryptionException ex) {
			throw new ConfigurationException("Failed to create StringEncrypter instance");
		}
	}

	public StringEncrypter getStringEncrypter() {
		return stringEncrypter;
	}

	public FileConfiguration getPersistenceConfiguration() {
		return this.persistenceConfiguration.getConfiguration();
	}

	public FileConfiguration getBaseConfiguration() {
		return baseConfiguration;
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	public PersistenceEntryManager getPersistenceEntryManager() {
		return persistenceEntryManager;
	}

	protected String getDefaultPersistanceConfigurationFileName() {
		return "jans-ldap.properties";
	}

	public C getAppConfiguration() {
		return appConfiguration;
	}

	public CacheConfiguration getCacheConfiguration() {
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
