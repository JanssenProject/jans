/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.test;

import java.util.Properties;

import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.impl.LdapEntryManagerFactory;
import org.apache.log4j.Logger;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class LdapEntryManagerSample {

    private static final Logger LOG = Logger.getLogger(LdapEntryManagerSample.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("ldap#bindDN", "cn=Directory Manager");
        connectionProperties.put("ldap#bindPassword", "Gluu2022#");
        connectionProperties.put("ldap#servers", "localhost:1636");
        connectionProperties.put("ldap#useSSL", "true");
        connectionProperties.put("ldap#maxconnections", "3");

        return connectionProperties;
    }

    public LdapEntryManager createLdapEntryManager() {
        LdapEntryManagerFactory ldapEntryManagerFactory = new LdapEntryManagerFactory();
        Properties connectionProperties = getSampleConnectionProperties();

        LdapEntryManager ldapEntryManager = ldapEntryManagerFactory.createEntryManager(connectionProperties);
        LOG.debug("Created LdapEntryManager: " + ldapEntryManager);

        return ldapEntryManager;
    }

}
