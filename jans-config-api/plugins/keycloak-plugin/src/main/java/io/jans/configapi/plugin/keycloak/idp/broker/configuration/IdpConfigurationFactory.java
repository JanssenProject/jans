/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpConf;
import io.jans.configapi.plugin.keycloak.idp.broker.model.config.IdpAppConfiguration;
import io.jans.as.model.configuration.Configuration;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.service.PersistanceFactoryService;
import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import java.io.File;
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
public class IdpConfigurationFactory {

    public static final String IDP_CONFIGURATION_ENTRY_DN = "idp_ConfigurationEntryDN";

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
    private Event<IdpAppConfiguration> idpConfigurationUpdateEvent;

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
    public static final String IDP_BASE_CONFIGURATION_RELOAD_EVENT_TYPE = "idp_baseConfigurationReloadEvent";

    private static final int DEFAULT_INTERVAL = 30; // 30 seconds
    private AtomicBoolean isActive;
    private long baseConfigurationFileLastModifiedTime;

    // base dir
    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String BASE_PROPERTIES_FILE = DIR + Constants.BASE_PROPERTIES_FILE_NAME;

    // idp config
    private IdpAppConfiguration idpAppConfiguration;
    private boolean idpConfigLoaded = false;
    private long idpLoadedRevision = -1;
    private FileConfiguration baseConfiguration;
    
    public String getIdpConfigurationDn() {
        return this.baseConfiguration.getString(IDP_CONFIGURATION_ENTRY_DN);
    }
    
    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing IdpConfigurationFactory ");
        this.isActive = new AtomicBoolean(true);
        try {
            
            loadBaseConfiguration();

        } finally {
            this.isActive.set(false);
        }
    }
    
    @Produces
    @ApplicationScoped
    public IdpAppConfiguration getIdpAppConfiguration() {
        return idpAppConfiguration;
    }

    
    public void create() {
        log.info("Loading IDP Configuration");

        // load IDP config from DB
        if (!loadIdpConfigFromDb()) {
            log.error("Failed to load IDP configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load SAML configuration from persistence.");
        } else {
            log.error("SAML Configuration loaded successfully - idpLoadedRevision:{}, idpAppConfiguration:{}",
                    this.idpLoadedRevision, getIdpAppConfiguration());
        }

       
    }

    public String getIdpAppConfigurationDn() {
        return this.baseConfiguration.getString(IDP_CONFIGURATION_ENTRY_DN);
    }
    
    public String getConfigurationDn(String key) {
        return this.baseConfiguration.getString(key);
    }

    private void loadBaseConfiguration() {
        log.debug("Loading base configuration - BASE_PROPERTIES_FILE:{}", BASE_PROPERTIES_FILE);

        this.baseConfiguration = createFileConfiguration(BASE_PROPERTIES_FILE);
        this.baseConfigurationFileLastModifiedTime = new File(BASE_PROPERTIES_FILE).lastModified();

        log.debug("Loaded base configuration:{}", baseConfiguration.getProperties());
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

    private boolean loadIdpConfigFromDb() {
        log.debug("Loading IDP configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final IdpConf idpConf = loadConfigurationFromDb(getConfigurationDn(IDP_CONFIGURATION_ENTRY_DN),
                    new IdpConf());
            log.trace("Conf configuration '{}' DB...", idpConf);

            if (idpConf != null) {
                initIdpConf(idpConf);

                // Destroy old configuration
                if (this.idpConfigLoaded) {
                    destroy(IdpAppConfiguration.class);
                }

                this.idpConfigLoaded = true;
                idpConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(idpAppConfiguration);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find api configuration in DB..." + ex.getMessage(), ex);
        }
        return false;
    }

    private void initIdpConf(IdpConf idpConf) {
        log.debug("Initializing IDP Configuration From DB.... idpConf:{}", idpConf);

        if (idpConf == null) {
            throw new ConfigurationException("Failed to load IDP Configuration From DB " + idpConf);
        }

        log.info("idpAppConfigurationFromDb:{}",idpConf);
        if (idpConf.getDynamicConf() != null) {
            this.idpAppConfiguration = idpConf.getDynamicConf();
        }

        this.idpLoadedRevision = idpConf.getRevision();

        log.debug("*** idpAppConfiguration:{}, idpLoadedRevision:{} ",
                this.idpAppConfiguration, idpLoadedRevision);

    }

   
    private <T> T loadConfigurationFromDb(String dn, T obj, String... returnAttributes) {
        log.debug("Load IDP configuration from DB -  dn:{}, clazz:{}, returnAttributes:{}", dn, obj, returnAttributes);
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return (T) persistenceEntryManager.find(dn, obj.getClass(), returnAttributes);
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    private boolean isIdpRevisionIncreased() {
        final IdpConf idpConf = loadConfigurationFromDb(getConfigurationDn(IDP_CONFIGURATION_ENTRY_DN_DN),
                new IdpConf(), "jansRevision");
        if (idpConf == null) {
            return false;
        }

        log.debug("Saml Config - DB revision: {}, server revision: {}", idpConf.getRevision(), idpLoadedRevision);
        return idpConf.getRevision() > this.idpLoadedRevision;
    }

    public boolean reloadIdpConfFromLdap() {
        log.debug("Reload api configuration TimerEvent");
        if (!isIdpRevisionIncreased()) {
            return false;
        }
        return this.loadIdpConfigFromDb();
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
                event.select(BaseConfigurationReload.Literal.INSTANCE).fire(SAML_BASE_CONFIGURATION_RELOAD_EVENT_TYPE);
            }
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            reloadIdpConfFromLdap();
        } catch (Exception ex) {
            log.error("Exception happened while reloading application configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

}
