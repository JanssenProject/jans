/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.exception.ConfigurationException;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.Configuration;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.error.ErrorMessages;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.service.ApplicationFactory;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.ConfigurationEvent;
import org.gluu.service.cdi.event.ConfigurationUpdate;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.slf4j.Logger;
import org.gluu.oxauth.model.config.BaseDnConfiguration;
import org.gluu.oxauth.model.config.StaticConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 15, 2016
 */
@ApplicationScoped
@Named
public class ConfigurationFactory {

	@Inject
	private Logger log;

	@Inject
	private Event<TimerEvent> timerEvent;

	@Inject
	private Event<AppConfiguration> configurationUpdateEvent;

	@Inject
	private Event<String> event;

	@Inject @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private Instance<PersistenceEntryManagerFactory> persistenceEntryManagerFactoryInstance;

	@Inject
	private Instance<Configuration> configurationInstance;

	public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";

	private final static int DEFAULT_INTERVAL = 30; // 30 seconds

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

	private static final String BASE_DIR;
	private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String GLUU_FILE_PATH = DIR + "gluu.properties";
	private static final String LDAP_FILE_PATH = DIR + "oxauth-ldap.properties";
	public static final String LDAP_DEFAULT_FILE_PATH = DIR + "gluu-ldap.properties";

	private final String CONFIG_FILE_NAME = "oxauth-config.json";
	private final String ERRORS_FILE_NAME = "oxauth-errors.json";
	private final String STATIC_CONF_FILE_NAME = "oxauth-static-conf.json";
	private final String WEB_KEYS_FILE_NAME = "oxauth-web-keys.json";
	private final String SALT_FILE_NAME = "salt";

	private String confDir, configFilePath, errorsFilePath, staticConfFilePath, webKeysFilePath, saltFilePath;

	private boolean loaded = false;
    
    private PersistenceConfiguration persistenceConfiguration;
	private AppConfiguration conf;
	private StaticConfiguration staticConf;
	private WebKeysConfiguration jwks;
	private ErrorResponseFactory errorResponseFactory;
	private String cryptoConfigurationSalt;

    private String contextPath;
	private String facesMapping;

	private AtomicBoolean isActive;

	private long loadedRevision = -1;
	private boolean loadedFromLdap = true;

	@PostConstruct
	public void init() {
		this.isActive = new AtomicBoolean(true);
		try {
            this.persistenceConfiguration = loadPersistenceConfiguration();
			this.confDir = confDir();

			this.configFilePath = confDir + CONFIG_FILE_NAME;
			this.errorsFilePath = confDir + ERRORS_FILE_NAME;
			this.staticConfFilePath = confDir + STATIC_CONF_FILE_NAME;

			String certsDir = this.persistenceConfiguration.getConfiguration().getString("certsDir");
			if (StringHelper.isEmpty(certsDir)) {
				certsDir = confDir;
			}
			this.webKeysFilePath = certsDir + File.separator + WEB_KEYS_FILE_NAME;
			this.saltFilePath = confDir + SALT_FILE_NAME;

			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
	}

	public void onServletContextActivation(@Observes ServletContext context ) {
        this.contextPath = context.getContextPath();

        this.facesMapping = "";
        String[] mappings = context.getServletRegistration("Faces Servlet").getMappings().toArray(new String[0]);
        if (mappings.length == 0) {
            return;
        }
        
        this.facesMapping = mappings[0].replaceAll("\\*", "");
     }

    protected PersistenceConfiguration loadPersistenceConfiguration() {
        PersistenceConfiguration currentPersistenceConfiguration = null;

        String gluuFileName = determineGluuConfigurationFileName();
        if (gluuFileName != null) {
            currentPersistenceConfiguration = loadPersistenceConfiguration(gluuFileName);
        }

        // Fall back to old LDAP persistence layer
        if (currentPersistenceConfiguration == null) {
            log.warn("Failed to load persistence configuration. Attempting to use LDAP layer");
            String ldapFileName = determineLdapConfigurationFileName();
            currentPersistenceConfiguration = loadLdapConfiguration(ldapFileName);
        }
        
        return currentPersistenceConfiguration;
    }

	public void create() {
		if (!createFromLdap(true)) {
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
	    PersistenceConfiguration newPersistenceConfiguration = loadPersistenceConfiguration();

		if (newPersistenceConfiguration != null) {
			if (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(), newPersistenceConfiguration.getFileName()) || (newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration.getLastModifiedTime())) {
				// Reload configuration only if it was modified
				this.persistenceConfiguration = newPersistenceConfiguration;
				event.select(LdapConfigurationReload.Literal.INSTANCE).fire(PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE);
			}
		}

		if (!loadedFromLdap) {
			return;
		}

		final Conf conf = loadConfigurationFromLdap("oxRevision");
		if (conf == null) {
			return;
		}

		if (conf.getRevision() <= this.loadedRevision) {
			return;
		}

		createFromLdap(false);
	}

	private String confDir() {
		final String confDir = this.persistenceConfiguration.getConfiguration().getString("confDir", null);
		if (StringUtils.isNotBlank(confDir)) {
			return confDir;
		}

		return DIR;
	}

	@Produces
    @ApplicationScoped
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }

	@Produces
	@ApplicationScoped
	public AppConfiguration getAppConfiguration() {
		return conf;
	}

	@Produces
	@ApplicationScoped
	public StaticConfiguration getStaticConfiguration() {
		return staticConf;
	}

	@Produces
	@ApplicationScoped
	public WebKeysConfiguration getWebKeysConfiguration() {
		return jwks;
	}

	@Produces
	@ApplicationScoped
	public ErrorResponseFactory getErrorResponseFactory() {
		return errorResponseFactory;
	}

	public BaseDnConfiguration getBaseDn() {
		return getStaticConfiguration().getBaseDn();
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	private boolean createFromFile() {
		boolean result = reloadConfFromFile() && reloadErrorsFromFile() && reloadStaticConfFromFile()
				&& reloadWebkeyFromFile();

		return result;
	}

	private boolean reloadWebkeyFromFile() {
		final WebKeysConfiguration webKeysFromFile = loadWebKeysFromFile();
		if (webKeysFromFile != null) {
			log.info("Reloaded web keys from file: " + webKeysFilePath);
			jwks = webKeysFromFile;
			return true;
		} else {
			log.error("Failed to load web keys configuration from file: " + webKeysFilePath);
		}

		return false;
	}

	private boolean reloadStaticConfFromFile() {
		final StaticConfiguration staticConfFromFile = loadStaticConfFromFile();
		if (staticConfFromFile != null) {
			log.info("Reloaded static conf from file: " + staticConfFilePath);
			staticConf = staticConfFromFile;
			return true;
		} else {
			log.error("Failed to load static configuration from file: " + staticConfFilePath);
		}

		return false;
	}

	private boolean reloadErrorsFromFile() {
		final ErrorMessages errorsFromFile = loadErrorsFromFile();
		if (errorsFromFile != null) {
			log.info("Reloaded errors from file: " + errorsFilePath);
			errorResponseFactory = new ErrorResponseFactory(errorsFromFile);
			return true;
		} else {
			log.error("Failed to load errors from file: " + errorsFilePath);
		}

		return false;
	}

	private boolean reloadConfFromFile() {
		final AppConfiguration configFromFile = loadConfFromFile();
		if (configFromFile != null) {
			log.info("Reloaded configuration from file: " + configFilePath);
			conf = configFromFile;
			return true;
		} else {
			log.error("Failed to load configuration from file: " + configFilePath);
		}

		return false;
	}

	private boolean createFromLdap(boolean recoverFromFiles) {
		log.info("Loading configuration from LDAP...");
		try {
			final Conf c = loadConfigurationFromLdap();
			if (c != null) {
				init(c);

				// Destroy old configuration
				if (this.loaded) {
					destroy(AppConfiguration.class);
					destroy(StaticConfiguration.class);
					destroy(WebKeysConfiguration.class);
					destroy(ErrorResponseFactory.class);
				}

				this.loaded = true;
				configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(conf);

				return true;
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		if (recoverFromFiles) {
			log.info("Unable to find configuration in LDAP, try to load configuration from file system... ");
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

	private Conf loadConfigurationFromLdap(String... returnAttributes) {
		final PersistenceEntryManager ldapManager = persistenceEntryManagerInstance.get();
		final String dn = this.persistenceConfiguration.getConfiguration().getString("oxauth_ConfigurationEntryDN");
		try {
			final Conf conf = ldapManager.find(Conf.class, dn, returnAttributes);

			return conf;
		} catch (BasePersistenceException ex) {
			log.error(ex.getMessage());
		}

		return null;
	}

	private void init(Conf p_conf) {
		initConfigurationFromJson(p_conf.getDynamic());
		initStaticConfigurationFromJson(p_conf.getStatics());
		initWebKeysFromJson(p_conf.getWebKeys());
		initErrorsFromJson(p_conf.getErrors());
		this.loadedRevision = p_conf.getRevision();
	}

	private void initWebKeysFromJson(String p_webKeys) {
		try {
			initJwksFromString(p_webKeys);
		} catch (Exception ex) {
			log.error("Failed to load JWKS. Attempting to generate new JWKS...", ex);

			String newWebKeys = null;
			try {
				// Generate new JWKS
				JSONObject jsonObject = AbstractCryptoProvider.generateJwks(
						getAppConfiguration().getKeyRegenerationInterval(), getAppConfiguration().getIdTokenLifetime(),
						getAppConfiguration());
				newWebKeys = jsonObject.toString();

				// Attempt to load new JWKS
				initJwksFromString(newWebKeys);

				// Store new JWKS in LDAP
				Conf conf = loadConfigurationFromLdap();
				conf.setWebKeys(newWebKeys);

				long nextRevision = conf.getRevision() + 1;
				conf.setRevision(nextRevision);

				final PersistenceEntryManager ldapManager = persistenceEntryManagerInstance.get();
				ldapManager.merge(conf);

				log.info("New JWKS generated successfully");
			} catch (Exception ex2) {
				log.error("Failed to re-generate JWKS keys", ex2);
			}
		}
	}

	public void initJwksFromString(String p_webKeys) throws IOException, JsonParseException, JsonMappingException {
		final WebKeysConfiguration k = ServerUtil.createJsonMapper().readValue(p_webKeys, WebKeysConfiguration.class);
		if (k != null) {
			jwks = k;
		}
	}

	private void initStaticConfigurationFromJson(String p_statics) {
		try {
			final StaticConfiguration c = ServerUtil.createJsonMapper().readValue(p_statics, StaticConfiguration.class);
			if (c != null) {
				staticConf = c;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void initConfigurationFromJson(String p_configurationJson) {
		try {
			final AppConfiguration c = ServerUtil.createJsonMapper().readValue(p_configurationJson,
					AppConfiguration.class);
			if (c != null) {
				conf = c;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void initErrorsFromJson(String p_errosAsJson) {
		try {
			final ErrorMessages errorMessages = ServerUtil.createJsonMapper().readValue(p_errosAsJson,
					ErrorMessages.class);
			if (errorMessages != null) {
				errorResponseFactory = new ErrorResponseFactory(errorMessages);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

    private PersistenceConfiguration loadLdapConfiguration(String ldapFileName) {
        try {
            FileConfiguration ldapConfiguration = new FileConfiguration(ldapFileName);

            // Allow to override value via environment variables
            replaceWithSystemValues(ldapConfiguration);

            long ldapFileLastModifiedTime = -1;
            File ldapFile = new File(ldapFileName);
            if (ldapFile.exists()) {
                ldapFileLastModifiedTime = ldapFile.lastModified();
            }

            PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(ldapFileName, ldapConfiguration, org.gluu.persist.ldap.impl.LdapEntryManagerFactory.class, ldapFileLastModifiedTime);

            return persistenceConfiguration;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    private PersistenceConfiguration loadPersistenceConfiguration(String gluuFileName) {
        try {
            // Determine persistence type
            FileConfiguration gluuFileConf = new FileConfiguration(gluuFileName);
            String persistenceType = gluuFileConf.getString("persistence.type");
            
            // Determine configuration file name and factory class type
            String persistenceFileName = null; 
            Class<? extends PersistenceEntryManagerFactory> persistenceEntryManagerFactoryType = null;
            
            for (PersistenceEntryManagerFactory persistenceEntryManagerFactory : persistenceEntryManagerFactoryInstance) {
                log.debug("Found Persistence Entry Manager Factory with type '{}'", persistenceEntryManagerFactory);
                if (StringHelper.equalsIgnoreCase(persistenceEntryManagerFactory.getPersistenceType(), persistenceType)) {
                    persistenceFileName = persistenceEntryManagerFactory.getDefaultConfigurationFileName();
                    persistenceEntryManagerFactoryType = (Class<? extends PersistenceEntryManagerFactory>) persistenceEntryManagerFactory.getClass().getSuperclass();
                    break;
                }
            }
            
            if (persistenceFileName == null) {
                log.error("Unable to get Persistence Entry Manager Factory by type '{}'", persistenceType);
                return null;
            }

            String persistenceFileNamePath = DIR + persistenceFileName;

            FileConfiguration persistenceFileConf = new FileConfiguration(persistenceFileNamePath);
            if (!persistenceFileConf.isLoaded()) {
                log.error("Unable to load configuration file '{}'", persistenceFileNamePath);
                return null;
            }

            // Allow to override value via environment variables
            replaceWithSystemValues(persistenceFileConf);

            long persistenceFileLastModifiedTime = -1;
            File persistenceFile = new File(persistenceFileNamePath);
            if (persistenceFile.exists()) {
                persistenceFileLastModifiedTime = persistenceFile.lastModified();
            }

            PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration(persistenceFileName, persistenceFileConf, persistenceEntryManagerFactoryType, persistenceFileLastModifiedTime);

            return persistenceConfiguration;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

	private void replaceWithSystemValues(FileConfiguration fileConfiguration) {
		Set<Map.Entry<Object, Object>> ldapProperties = fileConfiguration.getProperties().entrySet();
		for (Map.Entry<Object, Object> ldapPropertyEntry : ldapProperties) {
			String ldapPropertyKey = (String) ldapPropertyEntry.getKey();
			if (System.getenv(ldapPropertyKey) != null) {
				ldapPropertyEntry.setValue(System.getenv(ldapPropertyKey));
			}
		}
	}

	private String determineGluuConfigurationFileName() {
		File ldapFile = new File(GLUU_FILE_PATH);
		if (ldapFile.exists()) {
			return GLUU_FILE_PATH;
		}

		return null;
	}

	private String determineLdapConfigurationFileName() {
		File ldapFile = new File(LDAP_FILE_PATH);
		if (ldapFile.exists()) {
			return LDAP_FILE_PATH;
		}

		return LDAP_DEFAULT_FILE_PATH;
	}

	private AppConfiguration loadConfFromFile() {
		try {
			return ServerUtil.createJsonMapper().readValue(new File(configFilePath), AppConfiguration.class);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return null;
	}

	private ErrorMessages loadErrorsFromFile() {
		try {
			return ServerUtil.createJsonMapper().readValue(new File(errorsFilePath), ErrorMessages.class);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return null;
	}

	private StaticConfiguration loadStaticConfFromFile() {
		try {
			return ServerUtil.createJsonMapper().readValue(new File(staticConfFilePath), StaticConfiguration.class);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return null;
	}

	private WebKeysConfiguration loadWebKeysFromFile() {
		try {
			return ServerUtil.createJsonMapper().readValue(new File(webKeysFilePath), WebKeysConfiguration.class);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return null;
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

	public String getFacesMapping() {
        return facesMapping;
    }

    public String getContextPath() {
        return contextPath;
    }

    public class PersistenceConfiguration {
	    private final String fileName;
	    private final FileConfiguration configuration;
        private final Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType;
	    private final long lastModifiedTime;

	    public PersistenceConfiguration(String fileName, FileConfiguration configuration, Class<? extends PersistenceEntryManagerFactory> entryManagerFactoryType, long lastModifiedTime) {
            this.fileName = fileName;
            this.configuration = configuration;
            this.entryManagerFactoryType = entryManagerFactoryType;
            this.lastModifiedTime = lastModifiedTime;
        }

        public final String getFileName() {
            return fileName;
        }

        public final FileConfiguration getConfiguration() {
            return configuration;
        }

        public final Class<? extends PersistenceEntryManagerFactory> getEntryManagerFactoryType() {
            return entryManagerFactoryType;
        }

        public final long getLastModifiedTime() {
            return lastModifiedTime;
        }

	}

}
