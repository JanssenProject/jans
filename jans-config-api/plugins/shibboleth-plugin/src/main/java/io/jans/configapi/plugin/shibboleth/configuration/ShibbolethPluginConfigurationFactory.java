/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.shibboleth.configuration;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.model.config.Constants;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginAppConf;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginConfiguration;
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
public class ShibbolethPluginConfigurationFactory {

    public static final String SHIBBOLETH_CONFIGURATION_ENTRY_DN = "shibboleth_ConfigurationEntryDN";

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
    private Event<ShibbolethPluginConfiguration> shibbolethPluginConfigurationUpdateEvent;

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
    public static final String SHIBBOLETH_BASE_CONFIGURATION_RELOAD_EVENT_TYPE = "shibboleth_baseConfigurationReloadEvent";

    private static final int DEFAULT_INTERVAL = 30; // 30 seconds
    private AtomicBoolean isActive;
    private long baseConfigurationFileLastModifiedTime;

    // base dir
    private static final String BASE_DIR;
    private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;
    private static final String BASE_PROPERTIES_FILE = DIR + Constants.BASE_PROPERTIES_FILE_NAME;

    // shibboleth config
    public static final String SHIBBOLETH_CONFIGURATION_ENTRY = "shibboleth_ConfigurationEntryDN";
    private ShibbolethPluginConfiguration shibbolethPluginConfiguration;
    private boolean shibbolethPluginAppConfigLoaded = false;
    private long shibbolethLoadedRevision = -1;
    private FileConfiguration baseConfiguration;

    public String getShibbolethPluginAppConfigurationDn() {
        return this.baseConfiguration.getString(SHIBBOLETH_CONFIGURATION_ENTRY_DN);
    }

    public FileConfiguration getBaseConfiguration() {
        return baseConfiguration;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing ShibbolethPluginAppConfigurationFactory ");
        this.isActive = new AtomicBoolean(true);
        try {

            loadBaseConfiguration();

        } finally {
            this.isActive.set(false);
        }
    }

    @Produces
    @ApplicationScoped
    public ShibbolethPluginConfiguration getShibbolethPluginConfiguration() {
        return shibbolethPluginConfiguration;
    }

    public void create() {
        log.info("Loading Shibboleth Configuration");

        // load shibboleth config from DB
        if (!loadShibbolethPluginAppConfigFromDb()) {
            log.error("Failed to load Shibboleth configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load Shibboleth configuration from persistence.");
        } else {
            log.info(
                    "Shibboleth Configuration loaded successfully - shibbolethLoadedRevision:{}, ShibbolethPluginConfiguration:{}",
                    this.shibbolethLoadedRevision, getShibbolethPluginConfiguration());
        }

    }

    public String getShibbolethPluginConfigurationDn() {
        return this.baseConfiguration.getString(SHIBBOLETH_CONFIGURATION_ENTRY);
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

    private boolean loadShibbolethPluginAppConfigFromDb() {
        log.debug("Loading Api configuration from '{}' DB...", baseConfiguration.getString("persistence.type"));
        try {
            final ShibbolethPluginAppConf shibbolethPluginAppConf = loadConfigurationFromDb(
                    getConfigurationDn(SHIBBOLETH_CONFIGURATION_ENTRY), new ShibbolethPluginAppConf());
            log.trace("Conf configuration '{}' DB...", shibbolethPluginAppConf);

            if (shibbolethPluginAppConf != null) {
                initShibbolethPluginAppConf(shibbolethPluginAppConf);

                // Destroy old configuration
                if (this.shibbolethPluginAppConfigLoaded) {
                    destroy(ShibbolethPluginConfiguration.class);
                }

                this.shibbolethPluginAppConfigLoaded = true;
                shibbolethPluginConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE)
                        .fire(shibbolethPluginConfiguration);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find api configuration in DB..." + ex.getMessage(), ex);
        }
        return false;
    }

    private void initShibbolethPluginAppConf(ShibbolethPluginAppConf shibbolethPluginAppConf) {
        log.debug("Initializing Shibboleth Configuration From DB.... shibbolethPluginAppConf:{}",
                shibbolethPluginAppConf);

        if (shibbolethPluginAppConf == null) {
            throw new ConfigurationException(
                    "Failed to load shibboleth Configuration From DB " + shibbolethPluginAppConf);
        }

        log.info("ShibbolethPluginConfigurationFromDb:{}", shibbolethPluginAppConf);
        if (shibbolethPluginAppConf.getDynamicConf() != null) {
            this.shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        }

        this.shibbolethLoadedRevision = shibbolethPluginAppConf.getRevision();

        log.debug("*** shibbolethPluginConfiguration:{}, shibbolethLoadedRevision:{} ",
                this.shibbolethPluginConfiguration, shibbolethLoadedRevision);

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

    private boolean isShibbolethPluginRevisionIncreased() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = loadConfigurationFromDb(
                getConfigurationDn(SHIBBOLETH_CONFIGURATION_ENTRY_DN), new ShibbolethPluginAppConf(), "jansRevision");
        if (shibbolethPluginAppConf == null) {
            return false;
        }

        log.debug("Shibboleth Config - DB revision: {}, server revision: {}", shibbolethPluginAppConf.getRevision(),
                shibbolethLoadedRevision);
        return shibbolethPluginAppConf.getRevision() > this.shibbolethLoadedRevision;
    }

    public boolean reloadShibbolethPluginAppConfFromLdap() {
        log.debug("Reload api configuration TimerEvent");
        if (!isShibbolethPluginRevisionIncreased()) {
            return false;
        }
        return this.loadShibbolethPluginAppConfigFromDb();
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
                event.select(BaseConfigurationReload.Literal.INSTANCE)
                        .fire(SHIBBOLETH_BASE_CONFIGURATION_RELOAD_EVENT_TYPE);
            }
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            reloadShibbolethPluginAppConfFromLdap();
        } catch (Exception ex) {
            log.error("Exception happened while reloading application configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

}
