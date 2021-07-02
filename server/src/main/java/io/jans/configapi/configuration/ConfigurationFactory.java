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
//import org.eclipse.microprofile.config.inject.ConfigProperty;
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
    
    private io.jans.configapi.model.configuration.AppConfiguration apiAppConfiguration;
    //private AppConfiguration apiAppDynamicConf;
    private StaticConfiguration apiAppStaticConf;
    private long loadedRevision = -1;

    /*
     * @Inject
     * 
     * @ConfigProperty(name = "api.protection.type")
     */
    private String apiProtectionType;

//    @Inject
//    @ConfigProperty(name = "api.client.id")
    private String apiClientId;

    /*
     * @Inject
     * 
     * @ConfigProperty(name = "api.client.password")
     */
    private String apiClientPassword;

    /*
     * @Inject
     * 
     * @ConfigProperty(name = "api.approved.issuer")
     */
    private List<String> apiApprovedIssuer;

    @Produces
    @ApplicationScoped
    public AppConfiguration getAppConfiguration() {
        return appConfiguration;
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
    
    private Conf loadAuthConfigurationFromDb() {
        log.info("loading Auth Server Configuration From DB....");
        return this.loadConfigurationFromDb(this.getConfigurationDn());
    }
    
    private void loadApiAppConfigurationFromDb() {
        log.info("loading Api App Configuration From DB....");
        io.jans.configapi.model.configuration.Conf conf = null;
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            conf =  persistenceEntryManager.find(io.jans.configapi.model.configuration.Conf.class, this.baseConfiguration.getString("configApi_ConfigurationEntryDN"));
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
        }
        
        if(conf == null) {
            throw new ConfigurationException("Failed to Api App Configuration From DB " + conf);
        }
        log.info("ApiAppConfigurationFromDb = ....");
        if (conf.getDynamicConf() != null) {
            this.apiAppConfiguration = conf.getDynamicConf();
        }
        if (conf.getStaticConf() != null) {
            this.apiAppStaticConf = conf.getStaticConf();
        }
        this.loadedRevision = conf.getRevision();
        this.setApiConfigurationProperties();
    }
    
    private void setApiConfigurationProperties() {
        log.info("setApiConfigurationProperties ");
        if(this.apiAppConfiguration == null) {
            throw new ConfigurationException("Failed to load Configuration properties " + this.apiAppConfiguration);
        }
        this.apiApprovedIssuer = this.apiAppConfiguration.getApiApprovedIssuer();
        this.apiProtectionType = this.apiAppConfiguration.getApiProtectionType();
        this.apiClientId = this.apiAppConfiguration.getApiClientId();
        this.apiClientPassword = this.apiAppConfiguration.getApiClientPassword();
        
        log.info("Properties set, this.apiApprovedIssuer = "+this.apiApprovedIssuer+" , this.apiProtectionType = "+this.apiProtectionType+" , this.apiClientId = "+this.apiClientId+" , this.apiClientPassword = "+this.apiClientPassword);
    }

    private Conf loadConfigurationFromDb(String returnAttribute) {
        log.info("loadConfigurationFromDb " + returnAttribute);
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return persistenceEntryManager.find(Conf.class, returnAttribute);
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private void loadBaseConfiguration() {
        log.info("Loading base configuration " + BASE_PROPERTIES_FILE);
        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE);
        log.info("Loaded base configuration:" + baseConfiguration.getProperties());
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
            log.error("Failed to load configuration from {}", fileName, ex);
            throw new ConfigurationException("Failed to load configuration from " + fileName, ex);
        }
    }

    private void init(Conf p_conf) {
        initConfigurationConf(p_conf);
    }

    private void initConfigurationConf(Conf p_conf) {
        if (p_conf.getDynamic() != null) {
            appConfiguration = p_conf.getDynamic();
        }
        if (p_conf.getStatics() != null) {
            staticConf = p_conf.getStatics();
        }
        if (p_conf.getWebKeys() != null) {
            jwks = p_conf.getWebKeys();
        }
        if (p_conf.getErrors() != null) {
            errorResponseFactory = new ErrorResponseFactory(p_conf.getErrors(), p_conf.getDynamic());
        }
    }

    private void loadCryptoConfigurationSalt() {
        try {
            FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath);

            this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
        } catch (Exception ex) {
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
