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

	private static final Logger log = Logger.getLogger(LdapSampleEntryManager.class);

	private Properties getSampleConnectionProperties() {
		Properties connectionProperties = new Properties();

		connectionProperties.put("bindDN", "cn=Directory Manager,o=gluu");
		connectionProperties.put("bindPassword", "secret");
		connectionProperties.put("servers", "localhost:389");
		connectionProperties.put("useSSL", "false");
		connectionProperties.put("maxconnections", "3");

		return connectionProperties;
	}

	public LdapEntryManager createLdapEntryManager() {
		LdapEntryManagerFactory ldapEntryManagerFactory = new LdapEntryManagerFactory();
		Properties connectionProperties = getSampleConnectionProperties();

		LdapEntryManager ldapEntryManager = ldapEntryManagerFactory.createEntryManager(connectionProperties);
		log.debug("Created LdapEntryManager: " + ldapEntryManager);

		return ldapEntryManager;
	}

}
