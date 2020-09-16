package org.gluu.configapi.configuration;

import io.quarkus.arc.AlternativePriority;
import org.apache.commons.lang.StringUtils;
import org.gluu.exception.ConfigurationException;
import org.gluu.exception.OxIntializationException;
import org.gluu.oxauth.model.config.Conf;
import org.gluu.oxauth.model.config.StaticConfiguration;
import org.gluu.oxauth.model.config.WebKeysConfiguration;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.service.common.ApplicationFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.BasePersistenceException;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.util.StringHelper;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.StringEncrypter;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

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
    private final String SALT_FILE_NAME = "salt";

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
    private String confDir;
    private String saltFilePath;

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

    public void create() {
        loadBaseConfiguration();
        this.confDir = confDir();
        this.saltFilePath = confDir + SALT_FILE_NAME;

        this.persistenceConfiguration = persistanceFactoryService.loadPersistenceConfiguration(APP_PROPERTIES_FILE);
        loadCryptoConfigurationSalt();

        if (!createFromDb()) {
            log.error("Failed to load configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load configuration from persistence.");
        } else {
            log.info("Configuration loaded successfully.");
        }

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

    public String getOxauthConfigurationDn() {
        return this.baseConfiguration.getString("oxauth_ConfigurationEntryDN");
    }

    private Conf loadConfigurationFromDb() {
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            final Conf conf = persistenceEntryManager.find(Conf.class, getOxauthConfigurationDn());

            return conf;
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
        }

        return null;
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
            FileConfiguration fileConfiguration = new FileConfiguration(fileName);
            return fileConfiguration;
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

    public void loadCryptoConfigurationSalt() {
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
