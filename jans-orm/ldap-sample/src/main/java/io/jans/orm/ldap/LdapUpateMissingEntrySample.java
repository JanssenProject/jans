/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap;

import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.model.SimpleSessionState;

/**
 * @author Yuriy Movchan Date: 03/09/2020
 */
public final class LdapUpateMissingEntrySample {
    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapUpateMissingEntrySample.class);
    }

    private LdapUpateMissingEntrySample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
    	LdapEntryManagerSample sqlSampleEntryManager = new LdapEntryManagerSample();

        // Create SQL entry manager
        LdapEntryManager sqlEntryManager = sqlSampleEntryManager.createLdapEntryManager();

        String sessionId = UUID.randomUUID().toString();
        final String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=jans";

        final SimpleSessionState simpleSessionState = new SimpleSessionState();
        simpleSessionState.setDn(sessionDn);
        simpleSessionState.setId(sessionId);
        simpleSessionState.setLastUsedAt(new Date());

        try {
			sqlEntryManager.merge(simpleSessionState);
			System.out.println("Updated");
		} catch (EntryPersistenceException ex) {
            LOG.info(String.format("Failed to update, root case exception: %s", ex.getCause().getClass()), ex);
		}
    }

}
