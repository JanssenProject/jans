package org.gluu.ldap;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.ldap.model.CustomAttribute;
import org.apache.log4j.Logger;

import com.unboundid.ldap.sdk.ResultCode;

/**
 * @author Yuriy Movchan
 * Date: 11/03/2016
 */
public class LdapSample {

	private static final Logger log;

	static {
		// Add console appender
		LogManager.getRootLogger().removeAllAppenders();

		ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
		LogManager.getRootLogger().addAppender(consoleAppender);

		log = Logger.getLogger(LdapSample.class);
	}

	private Properties getSampleConnectionProperties() {
		Properties connectionProperties = new Properties();

		connectionProperties.put("bindDN", "cn=directory manager");
		connectionProperties.put("bindPassword", "secret");
		connectionProperties.put("servers", "localhost:31389");
		connectionProperties.put("useSSL", "false");
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

	public static void main(String[] args) {
		// Prepare sample connection details
		LdapSample ldapSample = new LdapSample();

		// Create LDAP entry manager
		LdapEntryManager ldapEntryManager = ldapSample.createLdapEntryManager();

		// Find all users which have specified object classes defined in
		// SimpleUser
		List<SimpleUser> users = ldapEntryManager.findEntries("o=gluu", SimpleUser.class, null);
		for (SimpleUser user : users) {
			log.debug("User with uid: " + user.getUserId());
		}

		if (users.size() > 0) {
			// Add attribute "streetAddress" to first user
			SimpleUser user = users.get(0);
			user.getCustomAttributes()
					.add(new CustomAttribute("streetAddress", "Somewhere: " + System.currentTimeMillis()));

			ldapEntryManager.merge(user);
		}
	}

}
