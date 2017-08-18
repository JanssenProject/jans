/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.status.ldap;

import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.xdi.oxauth.service.AppInitializer;
import org.xdi.service.cdi.event.LdapStatusEvent;
import org.xdi.service.cdi.event.Scheduled;
import org.xdi.service.timer.event.TimerEvent;
import org.xdi.service.timer.schedule.TimerSchedule;

import org.xdi.service.cdi.async.Asynchronous;
import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
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
    private LdapEntryManager ldapEntryManager;

    @Inject @Named(AppInitializer.LDAP_AUTH_ENTRY_MANAGER_NAME)
    private List<LdapEntryManager> ldapAuthEntryManagers;

    private AtomicBoolean isActive;

    public void initTimer() {
        log.info("Initializing Ldap Status Timer");
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
			LdapEntryManager ldapAuthEntryManager = ldapAuthEntryManagers.get(i);
			logConnectionProviderStatistic(ldapAuthEntryManager, "authConnectionProvider#" + i, "bindAuthConnectionProvider#" + i);
    	}
    }

	public void logConnectionProviderStatistic(LdapEntryManager ldapEntryManager, String connectionProviderName, String bindConnectionProviderName) {
		LDAPConnectionProvider ldapConnectionProvider = ldapEntryManager.getLdapOperationService().getConnectionProvider();
        LDAPConnectionProvider bindLdapConnectionProvider = ldapEntryManager.getLdapOperationService().getBindConnectionProvider();
        
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