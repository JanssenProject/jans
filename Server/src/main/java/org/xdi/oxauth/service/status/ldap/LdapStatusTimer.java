/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.status.ldap;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
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
import org.xdi.oxauth.service.AppInitializer;

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
        log.info("Initializing LdapStatusTimer");
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
    	LdapEntryManager ldapEntryManager = (LdapEntryManager) Component.getInstance(AppInitializer.LDAP_ENTRY_MANAGER_NAME, ScopeType.APPLICATION); 
    	List<LdapEntryManager> ldapAuthEntryManagers = (List<LdapEntryManager>) Component.getInstance(AppInitializer.LDAP_AUTH_ENTRY_MANAGER_NAME, ScopeType.APPLICATION); 

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
        	log.error("{0} is empty", connectionProviderName);
        } else {
            if (ldapConnectionProvider.getConnectionPool() == null) {
            	log.error("{0} is empty", connectionProviderName);
            } else {
            	log.info("{0} statistics: {1}", connectionProviderName, ldapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }

        if (bindLdapConnectionProvider == null) {
        	log.error("{0} is empty", bindConnectionProviderName);
        } else {
            if (bindLdapConnectionProvider.getConnectionPool() == null) {
            	log.error("{0} is empty", bindConnectionProviderName);
            } else {
            	log.info("{0} statistics: {1}", bindConnectionProviderName, bindLdapConnectionProvider.getConnectionPool().getConnectionPoolStatistics());
            }
        }
	}

}