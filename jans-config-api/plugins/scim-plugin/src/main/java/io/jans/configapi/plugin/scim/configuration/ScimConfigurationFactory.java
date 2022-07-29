/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.configuration;

import io.jans.scim.model.conf.Conf;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.config.oxtrust.Configuration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.exception.ConfigurationException;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigurationFactory {

    public static final String CONFIGURATION_ENTRY_DN = "scim_ConfigurationEntryDN";

    @Inject
    private Logger log;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    PersistenceEntryManager persistenceManager;

    @Inject
    private Instance<Configuration> configurationInstance;

    @Inject
    private Event<AppConfiguration> configurationUpdateEvent;

    @Inject
    private Event<TimerEvent> timerEvent;

    private AppConfiguration scimConfiguration;
    private boolean confLoaded = false;
    private long loadedRevision = -1;
    private boolean loadedFromLdap = true;
    private AtomicBoolean isActive;
    private  static final int DEFAULT_INTERVAL = 30; // 30 seconds

    @Produces
    @ApplicationScoped
    public AppConfiguration getAppConfiguration() {
        return scimConfiguration;
    }

    @PostConstruct
    public void init() {
        this.isActive = new AtomicBoolean(true);
        try {
            create();
        } finally {
            this.isActive.set(false);
        }
    }

    public String getScimConfigurationDn() {
        return configurationFactory.getConfigurationDn(CONFIGURATION_ENTRY_DN);
    }

    public void create() {
        log.info("Loading SCIM App Configuration");

        if (!loadScimConfigFromDb()) {
            log.error("Failed to load auth configuration from persistence. Please fix it!!!.");
            throw new ConfigurationException("Failed to load auth configuration from persistence.");
        } else {
            log.info("Auth Configuration loaded successfully - loadedRevision:{}", this.loadedRevision);
        }
    }

    private boolean loadScimConfigFromDb() {
        log.debug("Loading Scim configuration from '{}' DB...",
                configurationFactory.getBaseConfiguration().getString("persistence.type"));
        try {
            final Conf c = loadConfigurationFromDb(getScimConfigurationDn());
            log.trace("Auth configuration '{}' DB...", c);

            if (c != null) {
                initSimConf(c);

                // Destroy old configuration
                if (this.confLoaded) {
                    destroy(AppConfiguration.class);
                }

                this.confLoaded = true;
                configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(scimConfiguration);

                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to find auth configuration in DB " + ex.getMessage(), ex);
        }
        return false;
    }

    private Conf loadConfigurationFromDb(String dn, String... returnAttributes) {
        log.debug("loadConfigurationFromDb dn:{}, , returnAttributes:{}", dn, returnAttributes);

        try {
            return this.persistenceManager.find(dn, Conf.class, returnAttributes);
        } catch (BasePersistenceException ex) {
            log.error(ex.getMessage());
        }

        return null;
    }

    private void initSimConf(Conf conf) {
        initScimConfiguration(conf);
        this.loadedRevision = conf.getRevision();
    }

    private void initScimConfiguration(Conf conf) {
        if (conf.getDynamicConf() != null) {
            scimConfiguration = conf.getDynamicConf();
            log.trace("Scim Config - appConfiguration: {}", scimConfiguration);
        }
    }

    public boolean reloadScimConfFromLdap() {
        if (!isRevisionIncreased()) {
            return false;
        }

        return loadScimConfigFromDb();
    }

    private boolean isRevisionIncreased() {
        final Conf conf = loadConfigurationFromDb("jansRevision");
        if (conf == null) {
            return false;
        }

        log.trace("Scim DB revision: " + conf.getRevision() + ", server revision:" + loadedRevision);
        return conf.getRevision() > this.loadedRevision;
    }

    private void destroy(Class<? extends Configuration> clazz) {
        Instance<? extends Configuration> configInstance = configurationInstance.select(clazz);
        configurationInstance.destroy(configInstance.get());
    }

    public void initTimer() {
        log.debug("Initializing Configuration Timer");

        final int delay = 30;
        final int interval = DEFAULT_INTERVAL;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new ConfigurationEvent(),
                Scheduled.Literal.INSTANCE));
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
        } catch (Throwable ex) {
            log.error("Exception happened while reloading application configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private void reloadConfiguration() {
        if (!loadedFromLdap) {
            return;
        }

        reloadScimConfFromLdap();
    }

}
