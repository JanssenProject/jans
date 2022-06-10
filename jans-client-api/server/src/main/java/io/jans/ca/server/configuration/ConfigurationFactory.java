/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.configuration;

import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.Configuration;
import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.ApiConf;
import io.jans.ca.server.op.OpClientFactoryImpl;
import io.jans.ca.server.persistence.service.MainPersistenceService;
import io.jans.ca.server.service.*;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.*;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import io.jans.util.security.StringEncrypter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class ConfigurationFactory {

    public static final String CONFIGURATION_ENTRY_DN = "clientApi_ConfigurationEntryDN";
    public static final String JANS_BASE_CONFIG = "jans.base";

    static {
        if (System.getProperty(JANS_BASE_CONFIG) != null) {
            BASE_DIR = System.getProperty(JANS_BASE_CONFIG);
        } else if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
            BASE_DIR = System.getProperty("catalina.base");
        } else if (System.getProperty("catalina.home") != null) {
            BASE_DIR = System.getProperty("catalina.home");
        } else if (System.getProperty("jboss.home.dir") != null) {
            BASE_DIR = System.getProperty("jboss.home.dir");
        } else {
            String jansBase = Utils.readCompileProterty("compile.jans.base");
            BASE_DIR = jansBase;
            System.setProperty(JANS_BASE_CONFIG, jansBase);
        }
    }

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private Event<ApiAppConfiguration> configurationUpdateEvent;

    @Inject
    private Event<String> event;

    @Inject
    private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    ValidationService validationService;

    @Inject
    MainPersistenceService jansConfigurationService;

    @Inject
    RpSyncService rpSyncService;

    @Inject
    KeyGeneratorService keyGeneratorService;

    @Inject
    DiscoveryService discoveryService;

    @Inject
    RpService rpService;

    @Inject
    StateService stateService;

    @Inject
    UmaTokenService umaTokenService;

    @Inject
    PublicOpKeyService publicOpKeyService;

    @Inject
    RequestObjectService requestObjectService;

    @Inject
    OpClientFactoryImpl opClientFactory;

    @Inject
    IntrospectionService introspectionService;

    public static final String PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE = "persistenceConfigurationReloadEvent";
    public static final String BASE_CONFIGUARION_RELOAD_EVENT_TYPE = "baseConfigurationReloadEvent";
    private static final int DEFAULT_INTERVAL = 30; // 30 seconds
    private AtomicBoolean isActive;
    private long baseConfigurationFileLastModifiedTime;


    // base dir
    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

    private static final String BASE_PROPERTIES_FILE = DIR + Constants.BASE_PROPERTIES_FILE_NAME;
    private static final String APP_PROPERTIES_FILE = DIR + Constants.LDAP_PROPERTIES_FILE_NAME;
    private static final String SALT_FILE_NAME = Constants.SALT_FILE_NAME;

    private String saltFilePath;

    private boolean apiConfigloaded = false;

    private FileConfiguration baseConfiguration;

    private PersistenceConfiguration persistenceConfiguration;
    private ApiAppConfiguration dynamicConf;
    private StaticConfiguration staticConf;
    private String cryptoConfigurationSalt;
    private long loadedRevision = -1;
    private boolean loadedFromLdap = true;

    @Produces
    @ApplicationScoped
    private ServiceProvider getServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setRpService(rpService);
        serviceProvider.setConfigurationService(jansConfigurationService);
        serviceProvider.setDiscoveryService(discoveryService);
        serviceProvider.setValidationService(validationService);
        serviceProvider.setHttpService(discoveryService.getHttpService());
        serviceProvider.setRpSyncService(rpSyncService);
        serviceProvider.setStateService(stateService);
        serviceProvider.setUmaTokenService(umaTokenService);
        serviceProvider.setKeyGeneratorService(keyGeneratorService);
        serviceProvider.setPublicOpKeyService(publicOpKeyService);
        serviceProvider.setRequestObjectService(requestObjectService);
        serviceProvider.setOpClientFactory(opClientFactory);
        serviceProvider.setIntrospectionService(introspectionService);
        return serviceProvider;
    }

    @Produces
    @ApplicationScoped
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }

    @Produces
    @ApplicationScoped
    public ApiAppConfiguration getAppConfiguration() {
        return dynamicConf;
    }

    @Produces
    @ApplicationScoped
    public StaticConfiguration getStaticConfiguration() {
        return staticConf;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing ConfigurationFactory ...");
        this.isActive = new AtomicBoolean(true);
        try {
            log.info("---------PATH to file configuration: {}", APP_PROPERTIES_FILE);
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
        log.info("Loading Configuration ...");
        if (!loadApiConfigFromDb()) {
            log.error("Failed to load api configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load api configuration from persistence.");
        } else {
            log.info("Api Configuration loaded successfully - apiLoadedRevision:{}, ApiAppConfiguration:{}", this.loadedRevision, getAppConfiguration());
        }
    }

    public void initTimer() {
        log.debug("Initializing Configuration Timer");

        final int delay = 30;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, DEFAULT_INTERVAL), new ConfigurationEvent(), Scheduled.Literal.INSTANCE));
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

        if (!StringHelper.equalsIgnoreCase(this.persistenceConfiguration.getFileName(), newPersistenceConfiguration.getFileName())
                || newPersistenceConfiguration.getLastModifiedTime() > this.persistenceConfiguration.getLastModifiedTime()) {
            // Reload configuration only if it was modified
            this.persistenceConfiguration = newPersistenceConfiguration;
            event.select(LdapConfigurationReload.Literal.INSTANCE).fire(PERSISTENCE_CONFIGUARION_RELOAD_EVENT_TYPE);
        }

        // Reload Base configuration if needed
        File fileBaseConfiguration = new File(BASE_PROPERTIES_FILE);
        if (fileBaseConfiguration.exists()) {
            final long lastModified = fileBaseConfiguration.lastModified();
            if (lastModified > baseConfigurationFileLastModifiedTime) {
                // Reload configuration only if it was modified
                loadBaseConfiguration();
                event.select(BaseConfigurationReload.Literal.INSTANCE).fire(BASE_CONFIGUARION_RELOAD_EVENT_TYPE);
            }
        }

        if (!loadedFromLdap) {
            return;
        }

        reloadConfFromDb();
    }

    private boolean isApiRevisionIncreased() {
        final ApiConf conf = loadConfigurationFromDb(getConfigurationDn(CONFIGURATION_ENTRY_DN), new ApiConf(), "jansRevision");
        if (conf == null) {
            return false;
        }

        log.trace("LDAP revision: {}, server revision: {}", conf.getRevision(), loadedRevision);
        return conf.getRevision() > this.loadedRevision;
    }

    private String confDir() {
        log.info("PROPERTIES {}", System.getProperties());
        final String confDir = this.baseConfiguration.getString("confDir", null);
        if (StringUtils.isNotBlank(confDir)) {
            return confDir;
        }

        return DIR;
    }

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() {
        String encodeSalt = getCryptoConfigurationSalt();

        if (StringHelper.isEmpty(encodeSalt)) {
            throw new ConfigurationException("Encode salt isn't defined");
        }

        try {
            return StringEncrypter.instance(encodeSalt);
        } catch (StringEncrypter.EncryptionException ex) {
            throw new ConfigurationException("Failed to create StringEncrypter instance");
        }

    }

    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    public BaseDnConfiguration getBaseDn() {
        return getStaticConfiguration().getBaseDn();
    }

    public String getCryptoConfigurationSalt() {
        return cryptoConfigurationSalt;
    }

    public boolean reloadConfFromDb() {
        if (!isApiRevisionIncreased()) {
            return false;
        }

        return loadApiConfigFromDb();
    }

    private boolean loadApiConfigFromDb() {
        log.info("Loading configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            String dn = getConfigurationDn(CONFIGURATION_ENTRY_DN);
            log.info("Dn used = '{}' ", dn);
            final ApiConf apiConf = loadConfigurationFromDb(dn, new ApiConf());
            log.trace("ApiConf configuration '{}' DB...", apiConf);

            if (apiConf != null) {
                initApiAuthConf(apiConf);

                // Destroy old configuration
                if (this.apiConfigloaded) {
                    destroy(ApiAppConfiguration.class);
                }

                this.apiConfigloaded = true;
                configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(dynamicConf);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find api configuration in DB..." + ex.getMessage(), ex);
        }

        return false;
    }

    public void destroy(Class<? extends Configuration> clazz) {
        Instance<? extends Configuration> confInstance = configurationInstance.select(clazz);
        configurationInstance.destroy(confInstance.get());
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

    public String getConfigurationDn(String keyDn) {
        return this.baseConfiguration.getString(keyDn);
    }

    private void initApiAuthConf(ApiConf apiConf) {
        log.debug("Initializing Api App Configuration From DB.... apiConf:{}", apiConf);

        if (apiConf == null) {
            throw new ConfigurationException("Failed to load Api App Configuration From DB " + apiConf);
        }

        log.info("ApiAppConfigurationFromDb = ....");
        if (apiConf.getDynamicConf() != null) {
            dynamicConf = apiConf.getDynamicConf();
        }

        this.loadedRevision = apiConf.getRevision();
        log.debug("*** ConfigurationFactory::loadApiAppConfigurationFromDb() - apiAppConfiguration:{}, apiLoadedRevision:{} ", this.getAppConfiguration(), loadedRevision);
    }

    private void loadBaseConfiguration() {
        log.info("Loading base configuration - BASE_PROPERTIES_FILE:{}", BASE_PROPERTIES_FILE);

        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE, true);
        this.baseConfigurationFileLastModifiedTime = new File(BASE_PROPERTIES_FILE).lastModified();

        log.debug("Loaded base configuration:{}", this.baseConfiguration.getProperties());
    }

    public void loadCryptoConfigurationSalt() {
        try {
            FileConfiguration cryptoConfiguration = createFileConfiguration(saltFilePath, true);

            this.cryptoConfigurationSalt = cryptoConfiguration.getString("encodeSalt");
        } catch (Exception ex) {
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

    private void installSecurityProvider() {
        try {
            SecurityProviderUtility.installBCProvider();
        } catch (Exception ex) {
            log.error("Failed to install BC provider properly", ex);
        }
    }

}
