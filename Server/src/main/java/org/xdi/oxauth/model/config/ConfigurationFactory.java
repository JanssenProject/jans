/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.exception.ConfigurationException;
import org.xdi.oxauth.model.error.ErrorMessages;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.jwk.JSONWebKeySet;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.properties.FileConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version 0.9 February 12, 2015
 */
@Scope(ScopeType.APPLICATION)
@Name("configurationFactory")
@AutoCreate
@Startup
public class ConfigurationFactory {

    private static final Log LOG = Logging.getLog(ConfigurationFactory.class);

    public final static String LDAP_CONFIGUARION_RELOAD_EVENT_TYPE = "LDAP_CONFIGUARION_RELOAD";
    private final static String EVENT_TYPE = "ConfigurationFactoryTimerEvent";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds

    static {
        if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
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

    @Logger
    private Log log;

    private final String CONFIG_FILE_NAME = "oxauth-config.json";
    private final String ERRORS_FILE_NAME = "oxauth-errors.json";
    private final String STATIC_CONF_FILE_NAME = "oxauth-static-conf.json";
    private final String WEB_KEYS_FILE_NAME = "oxauth-web-keys.json";
    private final String SALT_FILE_NAME = "salt";

    private String confDir, configFilePath, errorsFilePath, staticConfFilePath, webKeysFilePath, saltFilePath;

    private FileConfiguration ldapConfiguration;
    private Configuration m_conf;
    private StaticConf m_staticConf;
    private JSONWebKeySet m_jwks;
	private String cryptoConfigurationSalt;

    private AtomicBoolean isActive;

    private long ldapFileLastModifiedTime = -1;

    private long loadedRevision = -1;
    private boolean loadedFromLdap = true;

    @Create
    public void init() {
        this.isActive = new AtomicBoolean(true);
    	try {
			loadLdapConfiguration();
			this.confDir = confDir();
			this.configFilePath = confDir + CONFIG_FILE_NAME;
			this.errorsFilePath = confDir + ERRORS_FILE_NAME;
			this.staticConfFilePath = confDir + STATIC_CONF_FILE_NAME;
			this.webKeysFilePath = getLdapConfiguration().getString("certsDir") + File.separator + WEB_KEYS_FILE_NAME;
			this.saltFilePath = confDir + SALT_FILE_NAME;
			loadCryptoConfigurationSalt();
		} finally {
			this.isActive.set(false);
		}
    }

    public void create() {
        if (!createFromLdap(true)) {
            LOG.error("Failed to load configuration from LDAP. Please fix it!!!.");
            throw new ConfigurationException("Failed to load configuration from LDAP.");
        } else {
            LOG.info("Configuration loaded successfully.");
        }
    }

    @Observer("org.jboss.seam.postInitialization")
    public void initReloadTimer() {
        final long delayBeforeFirstRun = 30 * 1000L;
        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(delayBeforeFirstRun, DEFAULT_INTERVAL * 1000L));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void reloadConfigurationTimerEvent() {
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
        File ldapFile = new File(LDAP_FILE_PATH);
        if (ldapFile.exists()) {
            final long lastModified = ldapFile.lastModified();
            if (lastModified > ldapFileLastModifiedTime) { // reload configuration only if it was modified
                loadLdapConfiguration();
                Events.instance().raiseAsynchronousEvent(LDAP_CONFIGUARION_RELOAD_EVENT_TYPE);
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
        final String confDir = getLdapConfiguration().getString("confDir");
        if (StringUtils.isNotBlank(confDir)) {
            return confDir;
        }

        return DIR;
    }

    public FileConfiguration getLdapConfiguration() {
        return ldapConfiguration;
    }

    public Configuration getConfiguration() {
        return m_conf;
    }

    public StaticConf getStaticConfiguration() {
        return m_staticConf;
    }

    public BaseDnConfiguration getBaseDn() {
        return getStaticConfiguration().getBaseDn();
    }

    public JSONWebKeySet getWebKeys() {
        return m_jwks;
    }

    public ErrorMessages getErrorResponses() {
        final ErrorResponseFactory f = ServerUtil.instance(ErrorResponseFactory.class);
        return f.getMessages();
    }

    public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	private boolean createFromFile() {
    	boolean result = reloadConfFromFile() &&  reloadErrorsFromFile() && reloadStaticConfFromFile() && reloadWebkeyFromFile();
    	
    	return result;
    }

    private boolean reloadWebkeyFromFile() {
        final JSONWebKeySet webKeysFromFile = loadWebKeysFromFile();
        if (webKeysFromFile != null) {
            LOG.info("Reloaded web keys from file: " + webKeysFilePath);
            m_jwks = webKeysFromFile;
            return true;
        } else {
            LOG.error("Failed to load web keys configuration from file: " + webKeysFilePath);
        }

        return false;
    }

    private boolean reloadStaticConfFromFile() {
        final StaticConf staticConfFromFile = loadStaticConfFromFile();
        if (staticConfFromFile != null) {
            LOG.info("Reloaded static conf from file: " + staticConfFilePath);
            m_staticConf = staticConfFromFile;
            return true;
        } else {
            LOG.error("Failed to load static configuration from file: " + staticConfFilePath);
        }

        return false;
    }

    private boolean reloadErrorsFromFile() {
        final ErrorMessages errorsFromFile = loadErrorsFromFile();
        if (errorsFromFile != null) {
            LOG.info("Reloaded errors from file: " + errorsFilePath);
            final ErrorResponseFactory f = ServerUtil.instance(ErrorResponseFactory.class);
            f.setMessages(errorsFromFile);
            return true;
        } else {
            LOG.error("Failed to load errors from file: " + errorsFilePath);
        }

        return false;
    }

    private boolean reloadConfFromFile() {
        final Configuration configFromFile = loadConfFromFile();
        if (configFromFile != null) {
            LOG.info("Reloaded configuration from file: " + configFilePath);
            m_conf = configFromFile;
            return true;
        } else {
            LOG.error("Failed to load configuration from file: " + configFilePath);
        }

        return false;
    }

    private boolean createFromLdap(boolean recoverFromFiles) {
        LOG.info("Loading configuration from LDAP...");
        try {
            final Conf conf = loadConfigurationFromLdap();
            if (conf != null) {
                init(conf);
                return true;
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        if (recoverFromFiles) {
            LOG.info("Unable to find configuration in LDAP, try to load configuration from file system... ");
            if (createFromFile()) {
                this.loadedFromLdap = false;
                return true;
            }
        }

        return false;
    }
    
    private Conf loadConfigurationFromLdap(String ... returnAttributes) {
        final LdapEntryManager ldapManager = ServerUtil.getLdapManager();
        final String dn = getLdapConfiguration().getString("configurationEntryDN");
        try {
            final Conf conf = ldapManager.find(Conf.class, dn, returnAttributes);

            return conf;
        } catch (LdapMappingException ex) {
            LOG.error(ex.getMessage());
        }
        
        return null;
    }

    private void init(Conf p_conf) {
        initConfigurationFromJson(p_conf.getDynamic());
        initStaticConfigurationFromJson(p_conf.getStatics());
        initErrorsFromJson(p_conf.getErrors());
        initWebKeysFromJson(p_conf.getWebKeys());
        this.loadedRevision = p_conf.getRevision();
    }

    private void initWebKeysFromJson(String p_webKeys) {
        try {
            final JSONWebKeySet k = ServerUtil.createJsonMapper().readValue(p_webKeys, JSONWebKeySet.class);
            if (k != null) {
            	m_jwks = k;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initStaticConfigurationFromJson(String p_statics) {
        try {
            final StaticConf c = ServerUtil.createJsonMapper().readValue(p_statics, StaticConf.class);
            if (c != null) {
            	m_staticConf = c;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initConfigurationFromJson(String p_configurationJson) {
        try {
            final Configuration c = ServerUtil.createJsonMapper().readValue(p_configurationJson, Configuration.class);
            if (c != null) {
            	m_conf = c;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void initErrorsFromJson(String p_errosAsJson) {
        try {
            final ErrorMessages errorMessages = ServerUtil.createJsonMapper().readValue(p_errosAsJson, ErrorMessages.class);
            if (errorMessages != null) {
                final ErrorResponseFactory f = ServerUtil.instance(ErrorResponseFactory.class);
                f.setMessages(errorMessages);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

	private void loadLdapConfiguration() {
        try {
    		ldapConfiguration = new FileConfiguration(LDAP_FILE_PATH);

    		File ldapFile = new File(LDAP_FILE_PATH);
    		if (ldapFile.exists()) {
    			this.ldapFileLastModifiedTime = ldapFile.lastModified();
    		}
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            ldapConfiguration = null;
        }
	}

	private Configuration loadConfFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(configFilePath), Configuration.class);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    private ErrorMessages loadErrorsFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(errorsFilePath), ErrorMessages.class);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    private StaticConf loadStaticConfFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(staticConfFilePath), StaticConf.class);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    private JSONWebKeySet loadWebKeysFromFile() {
        try {
            return ServerUtil.createJsonMapper().readValue(new File(webKeysFilePath), JSONWebKeySet.class);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
        return null;
    }

    public void loadCryptoConfigurationSalt() {
        try {
            FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath, true);

			this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
        } catch (Exception ex) {
            LOG.error("Failed to load configuration from {0}", ex, saltFilePath);
            throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
        }
    }

    private FileConfiguration createFileConfiguration(String fileName, boolean isMandatory) {
        try {
            FileConfiguration fileConfiguration = new FileConfiguration(fileName);

            return fileConfiguration;
        } catch (Exception ex) {
            if (isMandatory) {
                LOG.error("Failed to load configuration from {0}", ex, fileName);
                throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
            }
        }

        return null;
    }

    /**
	 * Get ConfigurationFactory instance
	 * 
	 * @return ConfigurationFactory instance
	 */
	public static ConfigurationFactory instance() {
        boolean createContexts = !Contexts.isEventContextActive() && !Contexts.isApplicationContextActive();
        if (createContexts) {
            Lifecycle.beginCall();
        }

        return (ConfigurationFactory) Component.getInstance(ConfigurationFactory.class);
	}

}
