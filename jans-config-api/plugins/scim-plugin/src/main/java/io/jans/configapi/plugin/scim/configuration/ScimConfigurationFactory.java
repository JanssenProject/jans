/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.scim.configuration;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import io.jans.exception.ConfigurationException;
import io.jans.as.model.config.BaseDnConfiguration;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.plugin.scim.model.config.AppConfiguration;
import io.jans.configapi.plugin.scim.model.config.ScimConfigurationEntry;
import io.jans.conf.model.AppConfigurationEntry;
import io.jans.config.oxtrust.Configuration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.BasePersistenceException;
import io.jans.orm.model.PersistenceConfiguration;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.BaseConfigurationReload;
import io.jans.service.cdi.event.ConfigurationEvent;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.LdapConfigurationReload;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import io.jans.util.properties.FileConfiguration;
import org.slf4j.Logger;

@ApplicationScoped
public class ScimConfigurationFactory extends io.jans.conf.service.ConfigurationFactory<AppConfiguration, ScimConfigurationEntry>{
    
    
    public static final String CONFIGURATION_ENTRY_DN = "scim_ConfigurationEntryDN";
    private static final String APP_PROPERTIES_FILE = DIR + "scim.properties";
    private final static int DEFAULT_INTERVAL = 30; // 30 seconds
    
    
    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;
    
    
    public String getDefaultConfigurationFileName() {
        return APP_PROPERTIES_FILE;
    }

    public Class<ScimConfigurationEntry> getAppConfigurationType(){
        return ScimConfigurationEntry.class;
    }

    public String getApplicationConfigurationPropertyName() {
        return getBaseConfiguration().getString(CONFIGURATION_ENTRY_DN);
    }

    public void initTimer() {
        log.debug("Initializing Configuration Timer");

        final int delay = 30;
        final int interval = DEFAULT_INTERVAL;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new ConfigurationEvent(),
                Scheduled.Literal.INSTANCE));
    }


}
