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
import io.jans.as.model.util.SecurityProviderUtility;
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
import io.jans.util.StringHelper;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Properties;

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

    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

    private static final String BASE_PROPERTIES_FILE = DIR + Constants.BASE_PROPERTIES_FILE_NAME;
    private static final String APP_PROPERTIES_FILE = DIR + Constants.LDAP_PROPERTIES_FILE_NAME;
    private static final String SALT_FILE_NAME = Constants.SALT_FILE_NAME;

    @Inject
    private Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    private AppConfiguration appConfiguration;
    private StaticConfiguration staticConf;
    private WebKeysConfiguration jwks;
    private ErrorResponseFactory errorResponseFactory;
    private PersistenceConfiguration persistenceConfiguration;
    private FileConfiguration baseConfiguration;
    private String cryptoConfigurationSalt;
    private String saltFilePath;

    private ApiAppConfiguration apiAppConfiguration;
    private CorsConfigurationFilter corsConfigurationFilter;
    private long loadedRevision = -1;

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
                log.debug("\n\n Initializing CorsConfiguration:{} ", corsConfiguration);
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

    @SuppressWarnings({ "all" })
    public void create() {
        loadBaseConfiguration();
        this.saltFilePath = confDir() + SALT_FILE_NAME;

        this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
        loadCryptoConfigurationSalt();

        if (!createFromDb()) {
            log.error("Failed to load configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load configuration from persistence.");
        } else {
            log.info("Configuration loaded successfully.");
        }

        loadApiAppConfigurationFromDb();
        log.info("Configuration loadedRevision:{}", this.loadedRevision);

        installSecurityProvider();

    }

    private boolean createFromDb() {
        log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final Conf c = loadAuthConfigurationFromDb();
            if (c != null) {
                init(c);
                return true;
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        throw new ConfigurationException("Unable to find configuration in DB... ");
    }

    public String getConfigurationDn() {
        return this.baseConfiguration.getString(Constants.SERVER_KEY_OF_CONFIGURATION_ENTRY);
    }

    public String getConfigurationDn(String key) {
        return this.baseConfiguration.getString(key);
    }

    private Conf loadAuthConfigurationFromDb() {
        log.info("loading Auth Server Configuration From DB....");
        return this.loadConfigurationFromDb(this.getConfigurationDn());
    }

    private void loadApiAppConfigurationFromDb() {
        log.info("loading Api App Configuration From DB....");
        ApiConf apiConf = null;
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            apiConf = persistenceEntryManager.find(ApiConf.class,
                    this.baseConfiguration.getString("configApi_ConfigurationEntryDN"));
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
        }

        if (apiConf == null) {
            throw new ConfigurationException("Failed to Api App Configuration From DB " + apiConf);
        }
        log.info("ApiAppConfigurationFromDb = ....");
        if (apiConf.getDynamicConf() != null) {
            this.apiAppConfiguration = apiConf.getDynamicConf();
        }

        this.loadedRevision = apiConf.getRevision();

        log.debug(
                "\n\n\n *** ConfigurationFactory::loadApiAppConfigurationFromDb() - apiAppConfiguration:{}, loadedRevision:{} ",
                this.apiAppConfiguration, loadedRevision);
        this.setApiConfigurationProperties();
    }

    private void setApiConfigurationProperties() {
        log.info("setApiConfigurationProperties ");
        if (this.apiAppConfiguration == null) {
            throw new ConfigurationException("Failed to load Configuration properties " + this.apiAppConfiguration);
        }

        log.debug("\n\n\n *** ConfigurationFactory::setApiConfigurationProperties() - this.apiAppConfiguration:{}",
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

    private Conf loadConfigurationFromDb(String returnAttribute) {
        log.info("loadConfigurationFromDb returnAttribute:{}", returnAttribute);
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return persistenceEntryManager.find(Conf.class, returnAttribute);
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private void loadBaseConfiguration() {
        log.info("Loading base configuration - BASE_PROPERTIES_FILE:{}", BASE_PROPERTIES_FILE);
        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE);
        log.info("Loaded base configuration:{}", baseConfiguration.getProperties());
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

    private void init(Conf conf) {
        initConfigurationConf(conf);
    }

    private void initConfigurationConf(Conf conf) {
        if (conf.getDynamic() != null) {
            appConfiguration = conf.getDynamic();
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

}
