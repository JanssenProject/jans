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
import io.jans.orm.ldap.model.MailUniquenessConfiguration;
import io.jans.orm.ldap.persistence.LdapEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class MailUniquenessConfigurationSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(MailUniquenessConfigurationSample.class);
    }

    private MailUniquenessConfigurationSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapEntryManagerSample ldapEntryManagerSample = new LdapEntryManagerSample();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapEntryManagerSample.createLdapEntryManager();

        MailUniquenessConfiguration conf = ldapEntryManager.find("cn=Unique mail address,cn=Plugins,cn=config", MailUniquenessConfiguration.class, null);
        System.out.println("Current mail uniqueness: " + conf.isEnabled());
        
        conf.setEnabled(!conf.isEnabled());

        // Upate configuration in LDAP
        ldapEntryManager.merge(conf);

        MailUniquenessConfiguration conf2 = ldapEntryManager.find("cn=Unique mail address,cn=Plugins,cn=config", MailUniquenessConfiguration.class, null);
        System.out.println("After update mail uniqueness: " + conf2.isEnabled());
    }

}
