package org.gluu.ldap;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;

import com.unboundid.ldap.sdk.ResultCode;

/**
 * @author Yuriy Movchan
 * Date: 01/13/2017
 */
public class LdapSampleEntryManager {

	private static final Logger log = Logger.getLogger(LdapSampleEntryManager.class);

	private Properties getSampleConnectionProperties() {
		Properties connectionProperties = new Properties();

		connectionProperties.put("bindDN", "cn=directory manager, o=gluu");
		connectionProperties.put("bindPassword", "secret");
		connectionProperties.put("servers", "server:1636");
		connectionProperties.put("useSSL", "true");
		connectionProperties.put("maxconnections", "3");

		return connectionProperties;
	}

	private LDAPConnectionProvider createConnectionProvider(Properties connectionProperties) {
		LDAPConnectionProvider connectionProvider = new LDAPConnectionProvider(connectionProperties);

		return connectionProvider;
	}

	private LDAPConnectionProvider createBindConnectionProvider(Properties bindConnectionProperties,
			Properties connectionProperties) {
		LDAPConnectionProvider bindConnectionProvider = createConnectionProvider(bindConnectionProperties);
		if (ResultCode.INAPPROPRIATE_AUTHENTICATION.equals(bindConnectionProvider.getCreationResultCode())) {
			log.warn(
					"It's not possible to create authentication LDAP connection pool using anonymous bind. Attempting to create it using binDN/bindPassword");
			bindConnectionProvider = createConnectionProvider(connectionProperties);
		}

		return bindConnectionProvider;
	}

	private Properties prepareBindConnectionProperties(Properties connectionProperties) {
		Properties bindProperties = (Properties) connectionProperties.clone();
		bindProperties.remove("bindDN");
		bindProperties.remove("bindPassword");

		return bindProperties;
	}

	public LdapEntryManager createLdapEntryManager() {
		Properties connectionProperties = getSampleConnectionProperties();
		LDAPConnectionProvider connectionProvider = createConnectionProvider(connectionProperties);

		Properties bindConnectionProperties = prepareBindConnectionProperties(connectionProperties);
		LDAPConnectionProvider bindConnectionProvider = createBindConnectionProvider(bindConnectionProperties,
				connectionProperties);

		LdapEntryManager ldapEntryManager = new LdapEntryManager(
				new OperationsFacade(connectionProvider, bindConnectionProvider));
		log.debug("Created LdapEntryManager: " + ldapEntryManager);

		return ldapEntryManager;
	}

}
