/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.configuration;

import io.quarkus.arc.AlternativePriority;
import org.apache.commons.lang.StringUtils;
import io.jans.exception.ConfigurationException;
import io.jans.exception.OxIntializationException;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.auth.*;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Properties;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@AlternativePriority(1)
public class ConfigurationFactory {

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
    private static final String APP_PROPERTIES_FILE = DIR + "oxauth.properties";
    private static final String SALT_FILE_NAME = "salt";

    @Inject
    Logger log;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

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
    
    @Inject
    @ConfigProperty(name = "resource.name")
    private static String API_RESOURCE_NAME;
    private static String API_RESOURCE_ID;
    
    @Inject
    @ConfigProperty(name = "client.id")
    private static String API_CLIENT_ID;
    
    @Inject
    @ConfigProperty(name = "protection.type")
    private static String API_PROTECTION_TYPE;
    
    @Inject
    private Instance<AuthorizationService> authorizationServiceInstance;
    
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

    public static String getConfigAppPropertiesFile() {
        return API_PROTECTION_TYPE;
    }

    public static String getApiResourceName() {
        return API_RESOURCE_NAME;
    }

    public static String getApiResourceId() {
        return API_RESOURCE_ID;
    }

    public static String getApiClientId() {
        return API_CLIENT_ID;
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

        //Initialize Config Api Resource
        initConfigApiResource();
        
        //Initialize API Protection Mechanism
        initApiProtectionService();
    }

    private boolean createFromDb() {
        log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final Conf c = loadConfigurationFromDb();
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
        return this.baseConfiguration.getString("oxauth_ConfigurationEntryDN");
    }

    private Conf loadConfigurationFromDb() {
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return persistenceEntryManager.find(Conf.class, getConfigurationDn());
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

   
   
    @Produces
    @ApplicationScoped
    @Named("authorizationService")
    private AuthorizationService initApiProtectionService() {
        if (StringHelper.isEmpty(ConfigurationFactory.getConfigAppPropertiesFile())) {
            throw new ConfigurationException("API Protection Type not defined");
        }
        try {
            if (ApiConstants.PROTECTION_TYPE_OAUTH2.equals(ConfigurationFactory.getConfigAppPropertiesFile())) {
                return authorizationServiceInstance.select(OpenIdAuthorizationService.class).get();
            } else
                return authorizationServiceInstance.select(UmaAuthorizationService.class).get();
        } catch (Exception ex) {
            log.error("Failed to create AuthorizationService instance", ex);
            throw new ConfigurationException("Failed to create AuthorizationService instance", ex);
        }
    }

    @Produces
    @ApplicationScoped
    @Named("configApiResource")
    private UmaResource initConfigApiResource() {
        log.error("ConfigurationFactory.getApiResourceName() = " + ConfigurationFactory.getApiResourceName());
        if (StringHelper.isEmpty(ConfigurationFactory.getApiResourceName())) {
            throw new ConfigurationException("Config API Resource not defined");
        }
        try {
            String[] targetArray = new String[] { ConfigurationFactory.getApiResourceName() };
            Filter oxIdFilter = Filter.createSubstringFilter("oxId", null, targetArray, null);
            Filter displayNameFilter = Filter.createSubstringFilter(ApiConstants.DISPLAY_NAME, null, targetArray, null);
            Filter searchFilter = Filter.createORFilter(oxIdFilter, displayNameFilter);
            List<UmaResource> umaResourceList = persistenceEntryManagerInstance.get()
                    .findEntries(getBaseDnForResource(), UmaResource.class, searchFilter);
            log.error(" \n umaResourceList = " + umaResourceList + "\n");
            /*
             * if (umaResourceList == null || umaResourceList.isEmpty()) throw new
             * ConfigurationException("Matching Config API Resource not found!");
             * UmaResource resource = umaResourceList.stream() .filter(x ->
             * ConfigurationFactory.getApiResourceName().equals(x.getName())).findFirst()
             * .orElse(null); if (resource == null) throw new
             * ConfigurationException("Config API Resource not found!"); return resource;
             */
            // To-uncomment-later???
            return null;

        } catch (Exception ex) {
            log.error("Failed to load Config API Resource.", ex);
            throw new ConfigurationException("Failed to load Config API Resource.", ex);
        }
    }

    public String getBaseDnForResource() {
        final String umaBaseDn = staticConf.getBaseDn().getUmaBase(); // "ou=uma,o=gluu"
        return String.format("ou=resources,%s", umaBaseDn);
    }

}
