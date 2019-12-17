package org.gluu.ldap;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class LdapSampleEntryManager {

    private static final Logger LOG = Logger.getLogger(LdapSampleEntryManager.class);

    private Properties getSampleConnectionProperties() {
        Properties connectionProperties = new Properties();

        connectionProperties.put("ldap.bindDN", "cn=Directory Manager");
        connectionProperties.put("ldap.bindPassword", "test");
        connectionProperties.put("ldap.servers", "localhost:1636");
        connectionProperties.put("ldap.useSSL", "true");
        connectionProperties.put("ldap.maxconnections", "3");

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
