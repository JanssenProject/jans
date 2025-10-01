package io.jans.kc.spi.custom.impl;



import io.jans.kc.spi.custom.JansThinBridgeProvider;
import io.jans.kc.spi.custom.JansThinBridgeProviderFactory;
import io.jans.kc.spi.ProviderIDs;
import io.jans.kc.spi.custom.JansThinBridgeInitException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.PersistenceEntryManagerFactory;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.service.StandalonePersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.util.security.PropertiesDecrypter;
import io.jans.util.security.StringEncrypter;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import org.jboss.logging.Logger;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;



public class DefaultJansThinBridgeProviderFactory implements JansThinBridgeProviderFactory {


    private static final Logger log = Logger.getLogger(DefaultJansThinBridgeProviderFactory.class);
    private static final String DEFAULT_CONFIG_FILENAME = "jans.properties";
    private static final String SALT_FILENAME = "salt";
    private static final String PROVIDER_ID = ProviderIDs.JANS_DEFAULT_THIN_BRIDGE_PROVIDER;

    private final PersistanceFactoryService persistenceFactoryService;
    private final PersistenceConfiguration  persistenceConfiguration;
    private final PersistenceEntryManager persistenceEntryManager;

    

    public DefaultJansThinBridgeProviderFactory() {

        log.info("Establishing connection with janssen database");
        persistenceFactoryService = new StandalonePersistanceFactoryService();
        persistenceConfiguration  = persistenceFactoryService.loadPersistenceConfiguration(DEFAULT_CONFIG_FILENAME);
        if(persistenceConfiguration == null) {
            throw new JansThinBridgeInitException("Failed to load persistence configuration from file. " +
                "\n+ Jans configuration base directory: " + getJansConfigurationBaseDir() +
                "\n+ Jans configuration default file: " + DEFAULT_CONFIG_FILENAME);
        }

        String confdir = persistenceConfiguration.getConfiguration().getString("confDir");
        if(!StringUtils.isNotBlank(confdir)) {
            confdir = getJansConfigurationBaseDir() + File.separator + "conf" + File.separator;
        }
        final String salt = cryptographicSaltFromFile(confdir + SALT_FILENAME);
        if(!StringUtils.isNotBlank(salt)) {
            throw new JansThinBridgeInitException("Failed to load cryptographic material from configuration");
        }
        persistenceEntryManager = createPersistenceEntryManager(persistenceConfiguration, persistenceFactoryService, salt);
        log.info("Connection established to janssen database");
    }

    @Override
    public JansThinBridgeProvider create(KeycloakSession session) {

        return new DefaultJansThinBridgeProvider(persistenceEntryManager);
    }


    @Override
    public void init(Config.Scope config) {
        //nothing to do during init for now
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //nothing to do during postInit for now
    }

    @Override
    public void close() {
        //nothing to do during cost for now
    }

    @Override
    public String getId() {

        return PROVIDER_ID;
    }

    private final String cryptographicSaltFromFile(final String path) {

        FileConfiguration cryptoconfig = new FileConfiguration(path);
        return cryptoconfig.getString("encodeSalt");
    }

    private final Properties preparePersistenceProperties(final PersistenceConfiguration persistenceConfiguration, final String salt) {

        try {
            FileConfiguration config = persistenceConfiguration.getConfiguration();
            Properties connprops = config.getProperties();
            return PropertiesDecrypter.decryptAllProperties(StringEncrypter.defaultInstance(),connprops,salt);
        }catch(StringEncrypter.EncryptionException e) {
            throw new JansThinBridgeInitException("Failed to decrypt persistence connection parameters",e);
        }
    }

    private final PersistenceEntryManager createPersistenceEntryManager(final PersistenceConfiguration config,
        final PersistanceFactoryService persistenceFactoryService, final String salt) {

        Properties persistconnprops = preparePersistenceProperties(config,salt);
        PersistenceEntryManagerFactory peManagerFactory = persistenceFactoryService.getPersistenceEntryManagerFactory(config);
        return peManagerFactory.createEntryManager(persistconnprops);   
    }

    private final String getJansConfigurationBaseDir() {

        if( System.getProperty("jans.base") !=null ) {
            return System.getProperty("jans.base");
        }else if( (System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null) ) {
            return System.getProperty("catalina.base");
        }else if( System.getProperty("catalina.home") != null ) {
            return System.getProperty("catalina.home");
        }
        return null;
    }
}
