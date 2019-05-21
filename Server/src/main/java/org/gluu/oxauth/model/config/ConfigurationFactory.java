/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.config;

import java.io.File;
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
import javax.servlet.ServletRegistration;

import org.apache.commons.lang.StringUtils;
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
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.BaseConfigurationReload;
import org.gluu.service.cdi.event.ConfigurationEvent;
import org.gluu.service.cdi.event.ConfigurationUpdate;
import org.gluu.service.cdi.event.LdapConfigurationReload;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;

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
	private PersistanceFactoryService persistanceFactoryService;

	@Inject
	private Instance<Configuration> configurationInstance;

	public final static String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
	public final static String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

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

	private static final String BASE_PROPERTIES_FILE = DIR + "gluu.properties";
	private static final String LDAP_PROPERTIES_FILE = DIR + "oxauth-ldap.properties";

	private final String CONFIG_FILE_NAME = "oxauth-config.json";
	private final String ERRORS_FILE_NAME = "oxauth-errors.json";
	private final String STATIC_CONF_FILE_NAME = "oxauth-static-conf.json";
	private final String WEB_KEYS_FILE_NAME = "oxauth-web-keys.json";
	private final String SALT_FILE_NAME = "salt";

	private String confDir, configFilePath, errorsFilePath, staticConfFilePath, webKeysFilePath, saltFilePath;

	private boolean loaded = false;

	private FileConfiguration baseConfiguration;
    
    private PersistenceConfiguration persistenceConfiguration;
	private AppConfiguration conf;
	private StaticConfiguration staticConf;
	private WebKeysConfiguration jwks;
	private ErrorResponseFactory errorResponseFactory;
	private String cryptoConfigurationSalt;

    private String contextPath;
	private String facesMapping;

	private AtomicBoolean isActive;

	private long baseConfigurationFileLastModifiedTime;

	private long loadedRevision = -1;
	private boolean loadedFromLdap = true;

	@PostConstruct
	public void init() {
		this.isActive = new AtomicBoolean(true);
		try {
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(LDAP_PROPERTIES_FILE);
			loadBaseConfiguration();

			this.confDir = confDir();

			this.configFilePath = confDir + CONFIG_FILE_NAME;
			this.errorsFilePath = confDir + ERRORS_FILE_NAME;
			this.staticConfFilePath = confDir + STATIC_CONF_FILE_NAME;

			String certsDir = this.baseConfiguration.getString("certsDir");
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
        ServletRegistration servletRegistration = context.getServletRegistration("Faces Servlet");
        if (servletRegistration == null) {
        	return;
        }

        String[] mappings = servletRegistration.getMappings().toArray(new String[0]);
        if (mappings.length == 0) {
            return;
        }
        
        this.facesMapping = mappings[0].replaceAll("\\*", "");
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
	    PersistenceConfiguration newPersistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(LDAP_PROPERTIES_FILE);

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
		log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
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
		final String dn = this.baseConfiguration.getString("oxauth_ConfigurationEntryDN");
		try {
			final Conf conf = ldapManager.find(dn, Conf.class, returnAttributes);

			return conf;
		} catch (BasePersistenceException ex) {
			ex.printStackTrace();
			log.error(ex.getMessage());
		}

		return null;
	}

	private void init(Conf p_conf) {
		initConfigurationConf(p_conf);

		this.loadedRevision = p_conf.getRevision();
	}

	private void initConfigurationConf(Conf p_conf) {
		if (p_conf.getDynamic() != null) {
			conf = p_conf.getDynamic();
		}
		if (p_conf.getStatics() != null) {
			staticConf = p_conf.getStatics();
		}
		if (p_conf.getWebKeys() != null) {
			jwks = p_conf.getWebKeys();
		} else {
			generateWebKeys();
		}
		if (p_conf.getErrors() != null) {
			errorResponseFactory = new ErrorResponseFactory(p_conf.getErrors());
		}
	}

	private void generateWebKeys() {
		log.error("Failed to load JWKS. Attempting to generate new JWKS...");

		String newWebKeys = null;
		try {
			// Generate new JWKS
			JSONObject jsonObject = AbstractCryptoProvider.generateJwks(
					getAppConfiguration().getKeyRegenerationInterval(), getAppConfiguration().getIdTokenLifetime(),
					getAppConfiguration());
			newWebKeys = jsonObject.toString();

			// Attempt to load new JWKS
			jwks = ServerUtil.createJsonMapper().readValue(newWebKeys, WebKeysConfiguration.class);

			// Store new JWKS in LDAP
			Conf conf = loadConfigurationFromLdap();
			conf.setWebKeys(jwks);

			long nextRevision = conf.getRevision() + 1;
			conf.setRevision(nextRevision);

			final PersistenceEntryManager ldapManager = persistenceEntryManagerInstance.get();
			ldapManager.merge(conf);

			log.info("New JWKS generated successfully");
		} catch (Exception ex2) {
			log.error("Failed to re-generate JWKS keys", ex2);
		}
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

	public String getFacesMapping() {
        return facesMapping;
    }

    public String getContextPath() {
        return contextPath;
    }

}
