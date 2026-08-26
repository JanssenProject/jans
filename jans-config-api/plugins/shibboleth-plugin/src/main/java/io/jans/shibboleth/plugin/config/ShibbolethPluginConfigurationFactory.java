package io.jans.shibboleth.plugin.config;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jans.as.model.configuration.Configuration;

import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;

import io.jans.orm.util.properties.FileConfiguration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.shibboleth.plugin.config.model.ShibbolethPluginAppConfiguration;
import io.jans.shibboleth.plugin.config.model.ShibbolethPluginConfiguration;
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

import org.slf4j.Logger;


@ApplicationScoped
@Alternative
@Priority(1)
public class ShibbolethPluginConfigurationFactory {
    
    private static final int DEFAULT_CONFIG_UPDATE_INTERVAL = 30; // 30 seconds
    private static final String BASE_CONFIG_PROPERTIES_FILE_NAME = "jans.properties";
    private static final String BASE_CONFIG_PROPERTIES_FILE_PATH = resolveConfigurationDir() + BASE_CONFIG_PROPERTIES_FILE_NAME;

    private static final String SHIBBOLETH_BASE_CONFIGURATION_RELOAD_EVENT_TYPE = "shibboleth_baseConfigurationReloadEvent";
    private static final String SHIBBOLETH_BASE_CONFIG_ENTRY_KEY = "shibboleth_ConfigurationEntryDN";

    private static final String PERSISTENCE_ENTRY_MANAGER_NAME = "persistenceEntryManager";

    private long baseConfigurationLastModifiedTime = 0;

    @Inject
    private Logger log;

    @Inject
    private Event<String> event;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    @Named(PERSISTENCE_ENTRY_MANAGER_NAME)
    private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    private Event<ShibbolethPluginConfiguration> pluginConfigurationUpdateEvent;

    private FileConfiguration baseConfiguration;
    private AtomicBoolean isActive;
    private long lastConfigurationRevision;

    private ShibbolethPluginConfiguration pluginConfiguration;
    private boolean configLoaded = false;

    @PostConstruct
    private void init() {

        log.info("Initializing shibboleth plugin configuration factory");
        isActive = new AtomicBoolean(true);
        try {

            loadBaseConfiguration();
        }finally {
            isActive.set(false);
        }
    }

    @Produces
    @ApplicationScoped
    public ShibbolethPluginConfiguration getPluginConfiguration() {

        return pluginConfiguration;
    }

    @Asynchronous
    public void reloadConfigurationTimerEvent(@Observes @Scheduled ConfigurationEvent configurationEvent) {
        
       if (baseConfigurationFileExists() && baseConfigurationFileWasModified(baseConfigurationLastModifiedTime)) {

            loadBaseConfiguration();
            triggerShibbolethConfigurationReloadEvent();
       }

       if (!tryActivate()) {

            return;
       }

       try {
            reloadShibbolethPluginConfiguration();
       }catch(Exception ex) {
            log.error("An error occured while reloading the shibboleth plugin configuration",ex);
       }finally {
            isActive.set(false);
       }
    }

    private boolean baseConfigurationFileExists() {

        return new File(BASE_CONFIG_PROPERTIES_FILE_PATH).exists();
    }

    private boolean baseConfigurationFileWasModified(long lastModifiedTime) {

        return new File(BASE_CONFIG_PROPERTIES_FILE_PATH).lastModified() > lastModifiedTime;
    }

    private void loadBaseConfiguration() {

        log.debug("Loading base configuration from file {}",BASE_CONFIG_PROPERTIES_FILE_PATH);
        
        baseConfiguration = createFileConfiguration(BASE_CONFIG_PROPERTIES_FILE_PATH);
        baseConfigurationLastModifiedTime = new File(BASE_CONFIG_PROPERTIES_FILE_PATH).lastModified();
        
        log.debug("Loading base configuration complete");
    }

    private FileConfiguration createFileConfiguration(String filename) {

        try {

            return new FileConfiguration(filename);
        }catch(Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to load configuration from {} ", filename,e);
            }
            throw new ConfigurationException("Failed to load configuration from " + filename,e);
        }
    }

    private boolean tryActivate() {

        return isActive.compareAndSet(false,true);
    }

    private void initializeConfigurationUpdateTimer(int delay) {

        log.debug("Initializing configuration update timer");

        final TimerSchedule configschedule = new TimerSchedule(delay,DEFAULT_CONFIG_UPDATE_INTERVAL);
        timerEvent.fire(new TimerEvent(configschedule,new ConfigurationEvent(),Scheduled.Literal.INSTANCE));
    }

    private void triggerShibbolethConfigurationReloadEvent() {

        event.select(BaseConfigurationReload.Literal.INSTANCE).fire(SHIBBOLETH_BASE_CONFIGURATION_RELOAD_EVENT_TYPE);
    }

    private void triggerShibbolethConfigurationUpdateEvent() {

        pluginConfigurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(pluginConfiguration);
    }

    private boolean reloadShibbolethPluginConfiguration() {

        log.debug("Reload shibboleth configuration timer ");
        if(!shibbolethPluginConfigurationRevisionHasIncreased()) {

            return false;
        }

        return loadShibbolethPluginConfiguration();
    }

    private boolean loadShibbolethPluginConfiguration() {

        log.debug("Loading shib plugin configuration from persistence store of type `{}`",baseConfiguration.getString("persistence.type"));
        try {
            final ShibbolethPluginAppConfiguration appconf = shibbolethPluginAppConfigurationFromPersistence(
                configurationDnForShibbolethPluginApp()
            );

            log.debug("Shibboleth plugin application configuration : {} ",appconf);
            
            if (appconf != null) {

                applyShibbolethPluginConfigurationFromAppConfiguration(appconf);

                if (configLoaded) {
                     discardPreviouslyLoadedConfiguration();
                }

                configLoaded = true;
                triggerShibbolethConfigurationUpdateEvent();

                return true;
            }
        }catch(Exception e) {
            log.error("Unable to load shibboleth plugin from persistence",e);
        }
        return false;
    }

    private boolean shibbolethPluginConfigurationRevisionHasIncreased() {

        final ShibbolethPluginAppConfiguration appconf = shibbolethPluginAppConfigurationFromPersistence(
            configurationDnForShibbolethPluginApp(),
            "jansRevision"
        );

        if (appconf == null) {
            return false;
        }
        log.debug("Shibboleth configuration - last revision number: {}, current revision number: {}",lastConfigurationRevision,
            appconf.getRevision());
        return appconf.getRevision() > lastConfigurationRevision;
    }

    private ShibbolethPluginAppConfiguration shibbolethPluginAppConfigurationFromPersistence(String dn, String ... returnAttributes) {

        log.debug("loading shibboleth configuration from persistence. dn: {}, clazz: {}, returnAttributes: {}",dn,
        ShibbolethPluginAppConfiguration.class,returnAttributes);
        final PersistenceEntryManager persistenceEntryManager = persistenceEntryManagerInstance.get();
        try {
            return (ShibbolethPluginAppConfiguration) persistenceEntryManager.find(dn,ShibbolethPluginAppConfiguration.class,returnAttributes);
        }catch(BasePersistenceException e) {
            log.error("Error loading shibboleth configuration from persistence",e);
            return null;
        }
    }

    private void applyShibbolethPluginConfigurationFromAppConfiguration(ShibbolethPluginAppConfiguration appconfig) {

        log.info("Applying shibboleth plugin configuration : {}",appconfig);
        if (appconfig.getDynamicConf() != null) {

            pluginConfiguration = appconfig.getDynamicConf();
        }

        lastConfigurationRevision = appconfig.getRevision();

        log.debug("Shibboleth configuration applied. config={}, revision={}",pluginConfiguration,lastConfigurationRevision);
    }

    private void discardPreviouslyLoadedConfiguration() {

        Instance<? extends Configuration> config_instance = configurationInstance.select(ShibbolethPluginConfiguration.class);
        configurationInstance.destroy(config_instance.get());
    }

    private String configurationDnForShibbolethPluginApp() {

        return baseConfiguration.getString(SHIBBOLETH_BASE_CONFIG_ENTRY_KEY);
    }

    private static String resolveBaseDir() {

        String jansBase = System.getProperty("jans.base");
        if (jansBase != null) {

            return jansBase;
        }

        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && System.getProperty("catalina.base.ignore") == null) {

            return catalinaBase;
        }

        String catalinaHome = System.getProperty("catalina.home");
        if (catalinaHome != null) {

            return catalinaHome;
        }

        String jbossHome = System.getProperty("jboss.home.dir");
        if (jbossHome != null) {

            return jbossHome;
        }

        return null;
    }

    private static String resolveConfigurationDir() {

        return resolveBaseDir() + File.separator + "conf" + File.separator;
    }

}
