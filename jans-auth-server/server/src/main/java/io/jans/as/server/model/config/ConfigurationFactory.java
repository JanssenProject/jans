/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.config;

import io.jans.as.common.model.event.CryptoProviderEvent;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.Configuration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.CryptoProviderFactory;
import io.jans.as.model.error.ErrorMessages;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.server.util.ServerUtil;
import io.jans.exception.ConfigurationException;
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
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.jans.as.model.config.Constants.BASE_PROPERTIES_FILE_NAME;
import static io.jans.as.model.config.Constants.CERTS_DIR;
import static io.jans.as.model.config.Constants.LDAP_PROPERTIES_FILE_NAME;
import static io.jans.as.model.config.Constants.SALT_FILE_NAME;
import static io.jans.as.model.config.Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version June 15, 2016
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
    private Event<AbstractCryptoProvider> cryptoProviderEvent;

    @Inject
    private Event<String> event;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    private Instance<AbstractCryptoProvider> abstractCryptoProviderInstance;

    public static final String PERSISTENCE_CONFIGURATION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
    public static final String BASE_CONFIGURATION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

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

    private static final String BASE_PROPERTIES_FILE = DIR + BASE_PROPERTIES_FILE_NAME;
    private static final String APP_PROPERTIES_FILE = DIR + LDAP_PROPERTIES_FILE_NAME;

    private static final String CONFIG_FILE_NAME = "jans-config.json";
    private static final String ERRORS_FILE_NAME = "jans-errors.json";
    private static final String STATIC_CONF_FILE_NAME = "jans-static-conf.json";
    private static final String WEB_KEYS_FILE_NAME = "jans-web-keys.json";

    private String configFilePath;
    private String errorsFilePath;
    private String staticConfFilePath;
    private String webKeysFilePath;
    private String saltFilePath;

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
        log.info("Initializing ConfigurationFactory ...");
        this.isActive = new AtomicBoolean(true);
        try {
            log.info("---------PATH to file configuration: {}", APP_PROPERTIES_FILE);
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
            loadBaseConfiguration();

            String confDir = confDir();

            this.configFilePath = confDir + CONFIG_FILE_NAME;
            this.errorsFilePath = confDir + ERRORS_FILE_NAME;
            this.staticConfFilePath = confDir + STATIC_CONF_FILE_NAME;

            String certsDir = this.baseConfiguration.getString(CERTS_DIR);
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

    public void onServletContextActivation(@Observes ServletContext context) {
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
            log.error("Failed to load configuration from DB. Please fix it!!!.");
            throw new ConfigurationException("Failed to load configuration from DB.");
        } else {
            log.info("Configuration loaded successfully.");
        }
    }

    public void initTimer() {
        log.debug("Initializing Configuration Timer");

        final int delay = 30;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new ConfigurationEvent(),
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

        if (newPersistenceConfiguration != null &&
                (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(), newPersistenceConfiguration.getFileName()) ||
                        (newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration.getLastModifiedTime()))) {
            // Reload configuration only if it was modified
            this.persistenceConfiguration = newPersistenceConfiguration;
            event.select(LdapConfigurationReload.Literal.INSTANCE).fire(PERSISTENCE_CONFIGURATION_RELOAD_EVENT_TYPE);
        }

        // Reload Base configuration if needed
        File baseConf = new File(BASE_PROPERTIES_FILE);
        if (baseConf.exists()) {
            final long lastModified = baseConf.lastModified();
            if (lastModified > baseConfigurationFileLastModifiedTime) {
                // Reload configuration only if it was modified
                loadBaseConfiguration();
                event.select(BaseConfigurationReload.Literal.INSTANCE).fire(BASE_CONFIGURATION_RELOAD_EVENT_TYPE);
            }
        }

        if (!loadedFromLdap) {
            return;
        }

        if (!isRevisionIncreased()) {
            return;
        }

        createFromLdap(false);
    }

    private boolean isRevisionIncreased() {
        final Conf persistenceConf = loadConfigurationFromPersistence("jansRevision");
        if (persistenceConf == null) {
            return false;
        }

        log.trace("LDAP revision: {}, server revision: {}", persistenceConf.getRevision(), loadedRevision);
        return persistenceConf.getRevision() > this.loadedRevision;
    }

    private String confDir() {
        final String confDirectory = this.baseConfiguration.getString("confDir", null);
        if (StringUtils.isNotBlank(confDirectory)) {
            return confDirectory;
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
    public io.jans.as.model.config.StaticConfiguration getStaticConfiguration() {
        return staticConf;
    }

    @Produces
    @ApplicationScoped
    public io.jans.as.model.config.WebKeysConfiguration getWebKeysConfiguration() {
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
        return reloadConfFromFile() && reloadErrorsFromFile() && reloadStaticConfFromFile() && reloadWebkeyFromFile();
    }

    private boolean reloadWebkeyFromFile() {
        final io.jans.as.model.config.WebKeysConfiguration webKeysFromFile = loadWebKeysFromFile();
        if (webKeysFromFile != null) {
            log.info("Reloaded web keys from file: {}", webKeysFilePath);
            jwks = webKeysFromFile;
            return true;
        } else {
            log.error("Failed to load web keys configuration from file: {}", webKeysFilePath);
        }

        return false;
    }

    private boolean reloadStaticConfFromFile() {
        final io.jans.as.model.config.StaticConfiguration staticConfFromFile = loadStaticConfFromFile();
        if (staticConfFromFile != null) {
            log.info("Reloaded static conf from file: {}", staticConfFilePath);
            staticConf = staticConfFromFile;
            return true;
        } else {
            log.error("Failed to load static configuration from file: {}", staticConfFilePath);
        }

        return false;
    }

    private boolean reloadErrorsFromFile() {
        final ErrorMessages errorsFromFile = loadErrorsFromFile();
        if (errorsFromFile != null) {
            log.info("Reloaded errors from file: {}", errorsFilePath);
            errorResponseFactory = new ErrorResponseFactory(errorsFromFile, conf);
            return true;
        } else {
            log.error("Failed to load errors from file: {}", errorsFilePath);
        }

        return false;
    }

    private boolean reloadConfFromFile() {
        final AppConfiguration configFromFile = loadConfFromFile();
        if (configFromFile != null) {
            log.info("Reloaded configuration from file: {}", configFilePath);
            conf = configFromFile;
            return true;
        } else {
            log.error("Failed to load configuration from file: {}", configFilePath);
        }

        return false;
    }

    public boolean reloadConfFromLdap() {
        if (!isRevisionIncreased()) {
            return false;
        }
        return createFromLdap(false);
    }

    private boolean createFromLdap(boolean recoverFromFiles) {
        log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final io.jans.as.model.config.Conf c = loadConfigurationFromPersistence();
            if (c != null) {
                init(c);

                // Destroy old configuration
                if (this.loaded) {
                    destroy(AppConfiguration.class);
                    destroy(io.jans.as.model.config.StaticConfiguration.class);
                    destroy(io.jans.as.model.config.WebKeysConfiguration.class);
                    destroy(ErrorResponseFactory.class);
                }

                this.loaded = true;
                configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(conf);

                destroyCryptoProviderInstance();
                AbstractCryptoProvider newAbstractCryptoProvider = abstractCryptoProviderInstance.get();
                cryptoProviderEvent.select(CryptoProviderEvent.Literal.INSTANCE).fire(newAbstractCryptoProvider);

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

    private void destroyCryptoProviderInstance() {
        log.trace("Destroyed crypto provider instance.");

        AbstractCryptoProvider abstractCryptoProvider = abstractCryptoProviderInstance.get();
        abstractCryptoProviderInstance.destroy(abstractCryptoProvider);
        CryptoProviderFactory.reset();
    }

    private io.jans.as.model.config.Conf loadConfigurationFromPersistence(String... returnAttributes) {
        final PersistenceEntryManager ldapManager = persistenceEntryManagerInstance.get();
        final String dn = this.baseConfiguration.getString(SERVER_KEY_OF_CONFIGURATION_ENTRY);
        try {
            return ldapManager.find(dn, io.jans.as.model.config.Conf.class, returnAttributes);
        } catch (BasePersistenceException ex) {
            if (!dn.contains("_test")) {
                ex.printStackTrace();
            }
            log.error(ex.getMessage(), ex);
        }

        return null;
    }

    private void init(Conf conf) {
        initConfigurationConf(conf);
        this.loadedRevision = conf.getRevision();
    }

    private void initConfigurationConf(Conf conf) {
        if (conf.getDynamic() != null) {
            this.conf = conf.getDynamic();
        }
        if (conf.getStatics() != null) {
            staticConf = conf.getStatics();
        }
        initWebKeys(conf);
        if (conf.getErrors() != null) {
            errorResponseFactory = new ErrorResponseFactory(conf.getErrors(), conf.getDynamic());
        }
    }

    private void initWebKeys(Conf conf) {
        final String jwksUri = conf.getDynamic().getJwksUri();
        if (jwksUri.startsWith(conf.getDynamic().getIssuer())) {
            if (conf.getWebKeys() != null) {
                jwks = conf.getWebKeys();
            } else {
                generateWebKeys();
            }
            return;
        }

        // external jwks
        final JSONObject keys = JwtUtil.getJSONWebKeys(jwksUri);
        log.trace("Downloaded external keys from {}, keys: {}", jwksUri, keys);

        final JSONWebKeySet keySet = JSONWebKeySet.fromJSONObject(keys);

        jwks = new WebKeysConfiguration();
        jwks.setKeys(keySet.getKeys());
    }

    private void generateWebKeys() {
        log.info("Failed to load JWKS. Attempting to generate new JWKS...");

        String newWebKeys = null;
        try {
            final AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(getAppConfiguration());

            // Generate new JWKS
            JSONObject jsonObject = AbstractCryptoProvider.generateJwks(cryptoProvider, getAppConfiguration());
            newWebKeys = jsonObject.toString();

            // Attempt to load new JWKS
            jwks = ServerUtil.createJsonMapper().readValue(newWebKeys, io.jans.as.model.config.WebKeysConfiguration.class);

            // Store new JWKS in LDAP
            Conf configuration = Objects.requireNonNull(loadConfigurationFromPersistence());
            configuration.setWebKeys(jwks);

            long nextRevision = configuration.getRevision() + 1;
            configuration.setRevision(nextRevision);

            final PersistenceEntryManager ldapManager = persistenceEntryManagerInstance.get();
            ldapManager.merge(configuration);

            log.info("Generated new JWKS successfully.");
            if (log.isTraceEnabled()) {
                log.trace("JWKS keys: {}", configuration.getWebKeys().getKeys().stream().map(JSONWebKey::getKid).collect(Collectors.toList()));
                log.trace("KeyStore keys: {}", cryptoProvider.getKeys());
            }
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
        this.baseConfigurationFileLastModifiedTime = new File(BASE_PROPERTIES_FILE).lastModified();
    }

    public void loadCryptoConfigurationSalt() {
        try {
            FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath, true);
            this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
        } catch (Exception ex) {
            if (log.isErrorEnabled())
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

    public String getFacesMapping() {
        return facesMapping;
    }

    public String getContextPath() {
        return contextPath;
    }

}
