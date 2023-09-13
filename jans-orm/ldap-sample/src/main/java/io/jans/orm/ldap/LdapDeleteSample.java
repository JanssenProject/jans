/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.persistence.LdapEntryManagerSample;
import io.jans.orm.model.base.DeletableEntity;
import io.jans.orm.search.filter.Filter;

import java.util.Date;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class LdapDeleteSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapDeleteSample.class);
    }

    private LdapDeleteSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapEntryManagerSample ldapEntryManagerSample = new LdapEntryManagerSample();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapEntryManagerSample.createLdapEntryManager();

        String baseDn = "ou=cache,o=jans";
		Filter filter = Filter.createANDFilter(
		        Filter.createEqualityFilter("del", true),
				Filter.createLessOrEqualFilter("exp", ldapEntryManager.encodeTime(baseDn, new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)))
        );

        int result = ldapEntryManager.remove(baseDn, DeletableEntity.class, filter, 100);
        System.out.println(result);
    }

}
