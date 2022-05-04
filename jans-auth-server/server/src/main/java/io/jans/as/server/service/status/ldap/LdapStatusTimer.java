/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.status.ldap;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.ldap.operation.LdapOperationService;
import io.jans.orm.ldap.operation.impl.LdapConnectionProvider;
import io.jans.orm.operation.PersistenceOperationService;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.LdapStatusEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import org.slf4j.Logger;

import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Movchan
 * @version 0.1, 11/18/2012
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class LdapStatusTimer {

    private final static int DEFAULT_INTERVAL = 60; // 1 minute

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_AUTH_ENTRY_MANAGER_NAME)
    private List<PersistenceEntryManager> ldapAuthEntryManagers;

    private AtomicBoolean isActive;

    public void initTimer() {
        log.info("Initializing Persistance Layer Status Timer");
        this.isActive = new AtomicBoolean(false);

        timerEvent.fire(new TimerEvent(new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL), new LdapStatusEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void process(@Observes @Scheduled LdapStatusEvent ldapStatusEvent) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            processInt();
        } finally {
            this.isActive.set(false);
        }
    }

    private void processInt() {
        logConnectionProviderStatistic(ldapEntryManager, "connectionProvider", "bindConnectionProvider");

        for (int i = 0; i < ldapAuthEntryManagers.size(); i++) {
            PersistenceEntryManager ldapAuthEntryManager = ldapAuthEntryManagers.get(i);
            logConnectionProviderStatistic(ldapAuthEntryManager, "authConnectionProvider#" + i, "bindAuthConnectionProvider#" + i);
        }
    }

    public void logConnectionProviderStatistic(PersistenceEntryManager ldapEntryManager, String connectionProviderName, String bindConnectionProviderName) {
        PersistenceOperationService persistenceOperationService = ldapEntryManager.getOperationService();
        if (!(persistenceOperationService instanceof LdapOperationService)) {
            return;
        }

        LdapConnectionProvider ldapConnectionProvider = ((LdapOperationService) persistenceOperationService).getConnectionProvider();
        LdapConnectionProvider bindLdapConnectionProvider = ((LdapOperationService) persistenceOperationService).getBindConnectionProvider();

        if (ldapConnectionProvider == null) {
            log.error("{} is empty", connectionProviderName);
        } else {
            if (ldapConnectionProvider.getConnectionPool() == null) {
                log.error("{} is empty", connectionProviderName);
            } else {
                log.info("{} statistics: {}", connectionProviderName, ldapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }

        if (bindLdapConnectionProvider == null) {
            log.error("{} is empty", bindConnectionProviderName);
        } else {
            if (bindLdapConnectionProvider.getConnectionPool() == null) {
                log.error("{} is empty", bindConnectionProviderName);
            } else {
                log.info("{} statistics: {}", bindConnectionProviderName, bindLdapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }
    }

}