package org.xdi.oxauth.service.status.ldap;

import java.util.concurrent.atomic.AtomicBoolean;

import org.gluu.site.ldap.LDAPConnectionProvider;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.TimerSchedule;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;

/**
 * @author Yuriy Movchan
 * @version 0.1, 11/18/2012
 */
@Name("ldapStatusTimer")
@AutoCreate
@Scope(ScopeType.APPLICATION)
public class LdapStatusTimer {

    private final static String EVENT_TYPE = "LdapStatusTimerEvent";
    private final static long DEFAULT_INTERVAL = 60 * 1000; // 1 minute

    @Logger
    private Log log;


    private AtomicBoolean isActive;

    @Observer("org.jboss.seam.postInitialization")
    public void init() {
        log.debug("Initializing LdapStatusTimer");
        this.isActive = new AtomicBoolean(false);

        Events.instance().raiseTimedEvent(EVENT_TYPE, new TimerSchedule(DEFAULT_INTERVAL, DEFAULT_INTERVAL));
    }

    @Observer(EVENT_TYPE)
    @Asynchronous
    public void process() {
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
        logConnectionProviderStatistic("centralConnectionProvider", "bindCentralConnectionProvider");
        logConnectionProviderStatistic("authConnectionProvider", "bindAuthConnectionProvider");
    }

	public void logConnectionProviderStatistic(String connectionProvider, String bindConnectionProvider) {
		LDAPConnectionProvider ldapConnectionProvider = (LDAPConnectionProvider) Contexts.getApplicationContext().get(connectionProvider);
        LDAPConnectionProvider bindLdapConnectionProvider = (LDAPConnectionProvider) Contexts.getApplicationContext().get(bindConnectionProvider);
        
        if (ldapConnectionProvider == null) {
        	log.error("{0} is empty", connectionProvider);
        } else {
            if (ldapConnectionProvider.getConnectionPool() == null) {
            	log.error("{0} is empty", connectionProvider);
            } else {
            	log.debug("{0} statistics: {1}", connectionProvider, ldapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }

        if (bindLdapConnectionProvider == null) {
        	log.error("{0} is empty", bindConnectionProvider);
        } else {
            if (bindLdapConnectionProvider.getConnectionPool() == null) {
            	log.error("{0} is empty", bindConnectionProvider);
            } else {
            	log.debug("{0} statistics: {1}", bindConnectionProvider, bindLdapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }
	}

}