/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.slf4j.Logger;
import org.xdi.exception.ConfigurationException;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.configuration.Configuration;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.error.ErrorMessages;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.service.AppInitializer;
import org.xdi.oxauth.service.cdi.event.ConfigurationEvent;
import org.xdi.oxauth.service.cdi.event.ConfigurationUpdate;
import org.xdi.oxauth.service.cdi.event.LdapConfigurationReload;
import org.xdi.oxauth.service.cdi.event.Scheduled;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;
import org.xdi.util.StringHelper;
import org.xdi.util.properties.FileConfiguration;

import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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

	@Inject @Named(AppInitializer.LDAP_ENTRY_MANAGER_NAME)
	private Instance<LdapEntryManager> ldapEntryManagerInstance;
	
	@Inject
	private Instance<Configuration> configurationIsntance;

	public final static String LDAP_CONFIGUARION_RELOAD_EVENT_TYPE = "ldapConfigurationReloadEvent";

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

	private static final String BASE_DIR;
	private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String LDAP_FILE_PATH = DIR + "oxauth-ldap.properties";
	public static final String LDAP_DEFAULT_FILE_PATH = DIR + "ox-ldap.properties";

	private final String CONFIG_FILE_NAME = "oxauth-config.json";
	private final String ERRORS_FILE_NAME = "oxauth-errors.json";
	private final String STATIC_CONF_FILE_NAME = "oxauth-static-conf.json";
	private final String WEB_KEYS_FILE_NAME = "oxauth-web-keys.json";
	private final String SALT_FILE_NAME = "salt";

	private String confDir, configFilePath, errorsFilePath, staticConfFilePath, webKeysFilePath, saltFilePath;

	private boolean loaded = false;
	private FileConfiguration ldapConfiguration;
	private AppConfiguration conf;
	private StaticConfiguration staticConf;
	private WebKeysConfiguration jwks;
	private ErrorResponseFactory errorResponseFactory;
	private String cryptoConfigurationSalt;

	private AtomicBoolean isActive;

	private String prevLdapFileName;
	private long ldapFileLastModifiedTime = -1;

	private long loadedRevision = -1;
	private boolean loadedFromLdap = true;

	@PostConstruct
	public void init() {
		this.isActive = new AtomicBoolean(true);
		try {
			String ldapFileName = determineLdapConfigurationFileName();
			this.prevLdapFileName = loadLdapConfiguration(ldapFileName);
			this.confDir = confDir();
			this.configFilePath = confDir + CONFIG_FILE_NAME;
			this.errorsFilePath = confDir + ERRORS_FILE_NAME;
			this.staticConfFilePath = confDir + STATIC_CONF_FILE_NAME;
			String certsDir = getLdapConfiguration().getString("certsDir");
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
		String ldapFileName = determineLdapConfigurationFileName();
		File ldapFile = new File(ldapFileName);
		if (ldapFile.exists()) {
			final long lastModified = ldapFile.lastModified();
			if (!StringHelper.equalsIgnoreCase(this.prevLdapFileName, ldapFileName)
					|| (lastModified > ldapFileLastModifiedTime)) {
				// reload configuration only if it was modified
				this.prevLdapFileName = loadLdapConfiguration(ldapFileName);
				event.select(LdapConfigurationReload.Literal.INSTANCE).fire(LDAP_CONFIGUARION_RELOAD_EVENT_TYPE);
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
		final String confDir = getLdapConfiguration().getString("confDir", null);
		if (StringUtils.isNotBlank(confDir)) {
			return confDir;
		}

		return DIR;
	}

	public FileConfiguration getLdapConfiguration() {
		return ldapConfiguration;
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
		Instance<? extends Configuration> confInstance = configurationIsntance.select(clazz);
		configurationIsntance.destroy(confInstance.get());
	}

	private Conf loadConfigurationFromLdap(String... returnAttributes) {
		final LdapEntryManager ldapManager = ldapEntryManagerInstance.get();
		final String dn = getLdapConfiguration().getString("oxauth_ConfigurationEntryDN");
		try {
			final Conf conf = ldapManager.find(Conf.class, dn, returnAttributes);

			return conf;
		} catch (LdapMappingException ex) {
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

				final LdapEntryManager ldapManager = ldapEntryManagerInstance.get();
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

	private String loadLdapConfiguration(String ldapFileName) {
		try {
			ldapConfiguration = new FileConfiguration(ldapFileName);

			File ldapFile = new File(ldapFileName);
			if (ldapFile.exists()) {
				this.ldapFileLastModifiedTime = ldapFile.lastModified();
			}

			return ldapFileName;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			ldapConfiguration = null;
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
			log.error("Failed to load configuration from {}", ex, saltFilePath);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
		try {
			FileConfiguration fileConfiguration = new FileConfiguration(fileName);

			return fileConfiguration;
		} catch (Exception ex) {
			if (isMandatory) {
				log.error("Failed to load configuration from {}", ex, fileName);
				throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
			}
		}

		return null;
	}

}
