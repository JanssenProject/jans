package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/01/2013
 */
@Name("configurationUpdateTimer")
@AutoCreate
@Scope(ScopeType.APPLICATION)
public class ConfigurationUpdateTimer {

    private final static String EVENT_TYPE = "ConfigurationUpdateTimerEvent";
    private final static long DEFAULT_INTERVAL = TimeUnit.HOURS.toMillis(1); // 1 hour

    @Logger
    private Log log;
//    @In
//    private LdapEntryManager ldapEntryManager;

    private AtomicBoolean isActive;

    @Observer("org.jboss.seam.postInitialization")
    public void init() {
        this.isActive = new AtomicBoolean(false);
        long interval = ConfigurationFactory.getConfiguration().getConfigurationUpdateInterval();
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }
        interval = interval * 1000L;
        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(interval, interval));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
        if (this.isActive.get()) {
            return;
        }

        if (this.isActive.compareAndSet(false, true)) {
            try {
                if (ConfigurationFactory.updateFromLdap()) {
                    log.trace("Configuration updated from LDAP successfully.");
                } else {
                    log.trace("Failed to update configuration from LDAP successfully.");
                }
            } finally {
                this.isActive.set(false);
            }
        }
    }
}
