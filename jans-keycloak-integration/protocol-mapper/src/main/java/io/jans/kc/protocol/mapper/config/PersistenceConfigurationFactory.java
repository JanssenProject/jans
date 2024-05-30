package io.jans.kc.protocol.mapper.config;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.model.PersistenceConfiguration;

import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.service.StandalonePersistanceFactoryService;
import io.jans.util.StringHelper;
import io.jans.util.exception.ConfigurationException;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;


import java.io.File;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import org.jboss.logging.Logger;

public class PersistenceConfigurationFactory {
    

    private static final Logger log = Logger.getLogger(PersistenceConfigurationFactory.class);
    private static final String DEFAULT_CONFIG_FILE_NAME = "jans.properties";
    private static final String SALT_FILE_NAME = "salt";

    private final PersistanceFactoryService persistenceFactoryService;
    private final PersistenceConfiguration persistenceConfig;
    private final PersistenceEntryManager persistenceEntryManager;

    public PersistenceConfigurationFactory(final PersistanceFactoryService persistenceFactoryService, 
        final PersistenceConfiguration persistenceConfig, final PersistenceEntryManager persistenceEntryManager) {

        this.persistenceFactoryService = persistenceFactoryService;
        this.persistenceConfig = persistenceConfig;
        this.persistenceEntryManager = persistenceEntryManager;
    }

    public final PersistenceEntryManager getPersistenceEntryManager() {

        return persistenceEntryManager;
    }

    public static PersistenceConfigurationFactory create() {

        PersistanceFactoryService persistenceFactoryService = new StandalonePersistanceFactoryService();
        PersistenceConfiguration config = persistenceFactoryService.loadPersistenceConfiguration(getDefaultConfigurationFileName());
        if(config == null) {

            throw new PersistenceConfigurationException("Failed to load persistence configuration from file. " +
                "\n+ Jans configuration base directory: " + getJansConfigurationBaseDir() +
                "\n+ Jans configuration default file: " + getDefaultConfigurationFileName());
        }

        FileConfiguration baseConfiguration  = loadBaseConfiguration();
        final String jansConfigBaseDir = getJansConfigurationBaseDir();
        String confdir = config.getConfiguration().getString("confDir");
        if(!StringUtils.isNotBlank(confdir)) {
            confdir = getJansConfigurationBaseDir() + File.separator + "conf" + File.separator;
        }
        final String salt = cryptographicSaltFromFile(confdir+SALT_FILE_NAME);
        if(!StringUtils.isNotBlank(salt)) {
            throw new PersistenceConfigurationException("Failed to load cryptographic material from configuration");
        }
        PersistenceEntryManager persistenceEntryManager = createPersistenceEntryManager(config, persistenceFactoryService, salt);
        return new PersistenceConfigurationFactory(persistenceFactoryService,config,persistenceEntryManager);
    }

    private static final String getDefaultConfigurationFileName() {

        return DEFAULT_CONFIG_FILE_NAME;
    }

    private static final FileConfiguration loadBaseConfiguration() {

        return createFileConfiguration(DEFAULT_CONFIG_FILE_NAME,true);
    }

    private static final FileConfiguration createFileConfiguration(String fileName, boolean mandatory) {

        try {
            return new FileConfiguration(fileName);
        }catch(Exception e) {
            log.errorv(e,"Failed to load configuration from {0}",fileName);
            if(mandatory && fileName != null) {
                throw new PersistenceConfigurationException("Failed to load configuration from "+fileName,e);
            }else if(mandatory && fileName == null) {
                throw new PersistenceConfigurationException("Failed to load configuration because filename was invalid",e);
            }
            return null;
        }
    }

    private static final StringEncrypter createStringEncrypterFromSaltFile(final String path) {

        try {
            final String salt = cryptographicSaltFromFile(path);
            if(StringHelper.isEmpty(salt)) {
                throw new PersistenceConfigurationException("Failed to create string encrypter. No cryptographic salt");
            }
            return StringEncrypter.instance(salt);
        }catch(StringEncrypter.EncryptionException e) {
            throw new PersistenceConfigurationException("Failed to create string encrypter",e);
        }
    }

    private static final String cryptographicSaltFromFile(final String path) {

        try {
            FileConfiguration cryptoconfig = new FileConfiguration(path);
            return cryptoconfig.getString("encodeSalt");
        }catch(Exception e){
            log.errorv(e,"Failed to load cryptographic salt from {}",path);
            throw new PersistenceConfigurationException("Failed to load cryptographic salt from " + path,e);
        }
    }

    private static final Properties preparePersistenceProperties(final PersistenceConfiguration persistenceConfiguration, final String salt) {

        try {
            FileConfiguration config = persistenceConfiguration.getConfiguration();
            Properties connprops = (Properties) config.getProperties();
            return PropertiesDecrypter.decryptAllProperties(StringEncrypter.defaultInstance(),connprops,salt);
        }catch(StringEncrypter.EncryptionException e) {
            throw new PersistenceConfigurationException("Failed to decrypt persistence connection parameters",e);
        }
    }

    private static final PersistenceEntryManager createPersistenceEntryManager(final PersistenceConfiguration config,
         final PersistanceFactoryService persistenceFactoryService, final String salt) {

        try {
          Properties persistenceconnprops = preparePersistenceProperties(config,salt);
          PersistenceEntryManagerFactory persistenceEntryManagerFactory = persistenceFactoryService.getPersistenceEntryManagerFactory(config);
          return persistenceEntryManagerFactory.createEntryManager(persistenceconnprops);
        }catch(Exception e) {
            throw new PersistenceConfigurationException("Failed to create persistence entry manager",e);
        }
    }

    private static final String getJansConfigurationBaseDir() {

        if(System.getProperty("jans.base") != null) {
            return System.getProperty("jans.base");
        }else if((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
            return System.getProperty("catalina.base");
        }else if(System.getProperty("catalina.home") != null) {
            return System.getProperty("catalina.home");
        }else if(System.getProperty("jboss.home.dir") != null) {
            return System.getProperty("jboss.home.dir");
        }

        return null;
    }
}
