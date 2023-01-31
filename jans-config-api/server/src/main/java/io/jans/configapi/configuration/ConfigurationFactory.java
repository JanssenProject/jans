/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.configuration.Configuration;
import io.jans.configapi.model.configuration.ApiConf;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.CorsConfiguration;
import io.jans.configapi.model.configuration.CorsConfigurationFilter;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.SecurityProviderUtility;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Alternative
@Priority(1)
public class ConfigurationFactory {

    static {
        if (System.getProperty("jans.base") != null) {
            BASE_DIR = System.getProperty("jans.base");
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

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private Event<AppConfiguration> authConfigurationUpdateEvent;

    @Inject
    private Event<ApiAppConfiguration> apiConfigurationUpdateEvent;

    @Inject
    private Event<String> event;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private Instance<Configuration> configurationInstance;

    // timer events
    public static final String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
    public static final String BASE_CONFIGURATION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";

    private static final int DEFAULT_INTERVAL = 30; // 30 seconds
    private AtomicBoolean isActive;
    private long baseConfigurationFileLastModifiedTime;

    // base dir
    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String BASE_PROPERTIES_FILE = DIR + Constants.BASE_PROPERTIES_FILE_NAME;
    private static final String APP_PROPERTIES_FILE = DIR + Constants.LDAP_PROPERTIES_FILE_NAME;
    private static final String SALT_FILE_NAME = Constants.SALT_FILE_NAME;

    // auth-server config
    private AppConfiguration appConfiguration;
    private StaticConfiguration staticConf;
    private WebKeysConfiguration jwks;
    private ErrorResponseFactory errorResponseFactory;
    private PersistenceConfiguration persistenceConfiguration;
    private FileConfiguration baseConfiguration;
    private String cryptoConfigurationSalt;
    private String saltFilePath;
    private boolean authConfigloaded = false;
    private long authLoadedRevision = -1;

    // api config
    public static final String CONFIGAPI_CONFIGURATION_ENTRY = "configApi_ConfigurationEntryDN";
    private ApiAppConfiguration apiAppConfiguration;
    private CorsConfigurationFilter corsConfigurationFilter;
    private boolean apiConfigloaded = false;
    private long apiLoadedRevision = -1;
    private String apiProtectionType;
    private String apiClientId;
    private String apiClientPassword;
    private List<String> apiApprovedIssuer;
    private boolean configOauthEnabled;

    @Produces
    @ApplicationScoped
    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
    }

    @Produces
    @ApplicationScoped
    public ApiAppConfiguration getApiAppConfiguration() {
        return apiAppConfiguration;
    }

    @Produces
    @ApplicationScoped
    public CorsConfigurationFilter getCorsConfigurationFilters() {
        return this.corsConfigurationFilter;
    }

    @Produces
    @ApplicationScoped
    public CorsConfiguration getCorsConfiguration() {
        try {
            if (this.corsConfigurationFilter != null) {
                CorsConfiguration corsConfiguration = new CorsConfiguration();
                corsConfiguration.parseAndStore(this.corsConfigurationFilter.getCorsEnabled().toString(),
                        this.corsConfigurationFilter.getCorsAllowedOrigins(),
                        this.corsConfigurationFilter.getCorsAllowedMethods(),
                        this.corsConfigurationFilter.getCorsAllowedHeaders(),
                        this.corsConfigurationFilter.getCorsExposedHeaders(),
                        this.corsConfigurationFilter.getCorsSupportCredentials().toString(),
                        Long.toString(this.corsConfigurationFilter.getCorsPreflightMaxAge()),
                        this.corsConfigurationFilter.getCorsRequestDecorate().toString());
                log.debug("Initializing CorsConfiguration:{} ", corsConfiguration);
                return corsConfiguration;
            }

        } catch (Exception ex) {
            throw new ConfigurationException("Failed to initialize  CorsConfiguration" + corsConfigurationFilter);
        }
        return null;
    }

    @Produces
    @ApplicationScoped
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }

    @Produces
    @ApplicationScoped
    public StaticConfiguration getStaticConf() {
        return staticConf;
    }

    @Produces
    @ApplicationScoped
    public WebKeysConfiguration getJwks() {
        return jwks;
    }

    @Produces
    @ApplicationScoped
    public ErrorResponseFactory getErrorResponseFactory() {
        return errorResponseFactory;
    }

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() throws OxIntializationException {
        if (StringHelper.isEmpty(cryptoConfigurationSalt)) {
            throw new OxIntializationException("Encode salt isn't defined");
        }
        try {
            return StringEncrypter.instance(cryptoConfigurationSalt);
        } catch (StringEncrypter.EncryptionException ex) {
            throw new OxIntializationException("Failed to create StringEncrypter instance", ex);
        }
    }

    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    public static String getAppPropertiesFile() {
        return APP_PROPERTIES_FILE;
    }

    public String getApiProtectionType() {
        return this.apiProtectionType;
    }

    public String getApiClientId() {
        return this.apiClientId;
    }

    public String getApiClientPassword() {
        return this.apiClientPassword;
    }

    public List<String> getApiApprovedIssuer() {
        return this.apiApprovedIssuer;
    }

    public boolean isConfigOauthEnabled() {
        return configOauthEnabled;
    }

    public void setConfigOauthEnabled(boolean configOauthEnabled) {
        this.configOauthEnabled = configOauthEnabled;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigurationFactory ");
        this.isActive = new AtomicBoolean(true);
        try {
            this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
            loadBaseConfiguration();

            this.saltFilePath = confDir() + SALT_FILE_NAME;
            loadCryptoConfigurationSalt();

            installSecurityProvider();
        } finally {
            this.isActive.set(false);
        }
    }

    public void create() {
        log.info("Loading Configuration");

        // load api config from DB
        if (!loadApiConfigFromDb()) {
            log.error("Failed to load api configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load api configuration from persistence.");
        } else {
            log.info("Api Configuration loaded successfully - apiLoadedRevision:{}, ApiAppConfiguration:{}",
                    this.apiLoadedRevision, getApiAppConfiguration());
        }

        // load auth config from DB
        if (!loadAuthConfigFromDb()) {
            log.error("Failed to load auth configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load auth configuration from persistence.");
        } else {
            log.info("Auth Configuration loaded successfully - authLoadedRevision:{}", this.authLoadedRevision);
        }
    }

    public String getAuthConfigurationDn() {
        return this.baseConfiguration.getString(Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY);
    }

    public String getConfigurationDn(String key) {
        return this.baseConfiguration.getString(key);
    }

    private void loadBaseConfiguration() {
        log.info("Loading base configuration - BASE_PROPERTIES_FILE:{}", BASE_PROPERTIES_FILE);

        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE);
        this.baseConfigurationFileLastModifiedTime = new File(BASE_PROPERTIES_FILE).lastModified();

        log.debug("Loaded base configuration:{}", baseConfiguration.getProperties());
    }

    private String confDir() {
        final String confDir = this.baseConfiguration.getString("confDir", null);
        if (StringUtils.isNotBlank(confDir)) {
            return confDir;
        }

        return DIR;
    }

    private FileConfiguration createFileConfiguration(String fileName) {
        try {
            return new FileConfiguration(fileName);
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("Failed to load configuration from {}", fileName, ex);
            }
            throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
        }
    }

    private boolean loadAuthConfigFromDb() {
        log.debug("Loading Auth configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final Conf c = loadConfigurationFromDb(getConfigurationDn(Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY),
                    new Conf());
            log.trace("Auth configuration '{}' DB...", c);

            if (c != null) {
                initAuthConf(c);

                // Destroy old configuration
                if (this.authConfigloaded) {
                    destroy(AppConfiguration.class);
                }

                this.authConfigloaded = true;
                authConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(appConfiguration);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find auth configuration in DB " + ex.getMessage(), ex);
        }
        return false;
    }

    private boolean loadApiConfigFromDb() {
        log.debug("Loading Api configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final ApiConf apiConf = loadConfigurationFromDb(getConfigurationDn(CONFIGAPI_CONFIGURATION_ENTRY),
                    new ApiConf());
            log.trace("ApiConf configuration '{}' DB...", apiConf);

            if (apiConf != null) {
                initApiAuthConf(apiConf);

                // Destroy old configuration
                if (this.apiConfigloaded) {
                    destroy(ApiAppConfiguration.class);
                }

                this.apiConfigloaded = true;
                apiConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(apiAppConfiguration);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find api configuration in DB..." + ex.getMessage(), ex);
        }
        return false;
    }

    private void initApiAuthConf(ApiConf apiConf) {
        log.debug("Initializing Api App Configuration From DB.... apiConf:{}", apiConf);

        if (apiConf == null) {
            throw new ConfigurationException("Failed to load Api App Configuration From DB " + apiConf);
        }

        log.info("ApiAppConfigurationFromDb = ....");
        if (apiConf.getDynamicConf() != null) {
            this.apiAppConfiguration = apiConf.getDynamicConf();
        }

        this.apiLoadedRevision = apiConf.getRevision();

        log.debug(
                "*** ConfigurationFactory::loadApiAppConfigurationFromDb() - apiAppConfiguration:{}, apiLoadedRevision:{} ",
                this.apiAppConfiguration, apiLoadedRevision);
        this.setApiConfigurationProperties();
    }

    private void setApiConfigurationProperties() {
        log.info("setApiConfigurationProperties");
        if (this.apiAppConfiguration == null) {
            throw new ConfigurationException("Failed to load Configuration properties " + this.apiAppConfiguration);
        }

        log.debug("*** ConfigurationFactory::setApiConfigurationProperties() - this.apiAppConfiguration:{}",
                this.apiAppConfiguration);
        this.apiApprovedIssuer = this.apiAppConfiguration.getApiApprovedIssuer();
        this.apiProtectionType = this.apiAppConfiguration.getApiProtectionType();
        this.apiClientId = this.apiAppConfiguration.getApiClientId();
        this.apiClientPassword = this.apiAppConfiguration.getApiClientPassword();
        this.configOauthEnabled = this.apiAppConfiguration.isConfigOauthEnabled();

        if (this.apiAppConfiguration.getCorsConfigurationFilters() != null
                && !this.apiAppConfiguration.getCorsConfigurationFilters().isEmpty()) {
            this.corsConfigurationFilter = this.apiAppConfiguration.getCorsConfigurationFilters().stream()
                    .filter(x -> "CorsFilter".equals(x.getFilterName())).findAny().orElse(null);
        }

        log.debug(
                "Properties set, this.apiApprovedIssuer:{}, , this.apiProtectionType:{}, this.apiClientId :{}, this.apiClientPassword:{}, this.corsConfigurationFilter:{}, this.configOauthEnabled:{} ",
                this.apiApprovedIssuer, this.apiProtectionType, this.apiClientId, this.apiClientPassword,
                this.corsConfigurationFilter, this.configOauthEnabled);

        // Populate corsConfigurationFilter object
        CorsConfiguration corsConfiguration = this.getCorsConfiguration();
        log.debug("CorsConfiguration Produced :{} ", corsConfiguration);
        getCorsConfigurationFilters();
    }

    private <T> T loadConfigurationFromDb(String dn, T obj, String... returnAttributes) {
        log.debug("loadConfigurationFromDb dn:{}, clazz:{}, returnAttributes:{}", dn, obj, returnAttributes);
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return (T) persistenceEntryManager.find(dn, obj.getClass(), returnAttributes);
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private void initAuthConf(Conf conf) {
        initAuthConfiguration(conf);
        this.authLoadedRevision = conf.getRevision();
    }

    private void initAuthConfiguration(Conf conf) {
        if (conf.getDynamic() != null) {
            appConfiguration = conf.getDynamic();
            log.trace("Auth Config - appConfiguration: {}", appConfiguration);
        }
        if (conf.getStatics() != null) {
            staticConf = conf.getStatics();
        }
        if (conf.getWebKeys() != null) {
            jwks = conf.getWebKeys();
        }
        if (conf.getErrors() != null) {
            errorResponseFactory = new ErrorResponseFactory(conf.getErrors(), conf.getDynamic());
        }
    }

    private boolean isAuthRevisionIncreased() {
        final Conf persistenceConf = loadConfigurationFromDb(
                getConfigurationDn(Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY), new Conf(), "jansRevision");
        if (persistenceConf == null) {
            return false;
        }

        log.debug("Auth Config - DB revision: {}, server revision: {}", persistenceConf.getRevision(),
                authLoadedRevision);
        return persistenceConf.getRevision() > this.authLoadedRevision;
    }

    private boolean isApiRevisionIncreased() {
        final ApiConf apiConf = loadConfigurationFromDb(getConfigurationDn(CONFIGAPI_CONFIGURATION_ENTRY),
                new ApiConf(), "jansRevision");
        if (apiConf == null) {
            return false;
        }

        log.debug("Api Config - DB revision: {}, server revision: {}", apiConf.getRevision(), apiLoadedRevision);
        return apiConf.getRevision() > this.apiLoadedRevision;
    }

    private void loadCryptoConfigurationSalt() {
        try {
            FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath);

            this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
        } catch (Exception ex) {
            if (log.isErrorEnabled())
                log.error("Failed to load configuration from {}", saltFilePath, ex);
            throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
        }
    }

    public String getCryptoConfigurationSalt() {
        return cryptoConfigurationSalt;
    }

    public Properties getDecryptedConnectionProperties() throws OxIntializationException {
        FileConfiguration persistenceConfig = persistenceConfiguration.getConfiguration();
        Properties connectionProperties = persistenceConfig.getProperties();
        if (connectionProperties == null || connectionProperties.isEmpty())
            return connectionProperties;

        return PropertiesDecrypter.decryptAllProperties(getStringEncrypter(), connectionProperties);
    }

    private void installSecurityProvider() {
        try {
            SecurityProviderUtility.installBCProvider();
        } catch (Exception ex) {
            log.error("Failed to install BC provider properly", ex);
        }
    }

    public boolean reloadAuthConfFromLdap() {
        log.debug("Reload auth configuration TimerEvent");
        if (!isAuthRevisionIncreased()) {
            return false;
        }
        return loadAuthConfigFromDb();
    }

    public boolean reloadApiConfFromLdap() {
        log.debug("Reload api configuration TimerEvent");
        if (!isApiRevisionIncreased()) {
            return false;
        }
        return this.loadApiConfigFromDb();
    }

    public void destroy(Class<? extends Configuration> clazz) {
        Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
        configurationInstance.destroy(confInstance.get());
    }

    public void initTimer() {
        log.debug("Initializing Configuration Timer");

        final int delay = 30;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new ConfigurationEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void reloadConfigurationTimerEvent(@Observes @Scheduled ConfigurationEvent configurationEvent) {
        log.debug("Config reload configuration TimerEvent - baseConfigurationFileLastModifiedTime:{}",
                baseConfigurationFileLastModifiedTime);

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

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            reloadAuthConfFromLdap();
            reloadApiConfFromLdap();
        } catch (Exception ex) {
            log.error("Exception happened while reloading application configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

}
