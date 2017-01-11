package org.gluu.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.VirtualListViewResponse;
import org.xdi.log.LoggingHelper;

import java.util.List;
import java.util.Properties;

/**
 * @author Yuriy Movchan
 * Date: 11/03/2016
 */
public class LdapSample {

	private static final Logger log;

	static {
		StatusLogger.getLogger().setLevel(Level.OFF);
		LoggingHelper.configureConsoleAppender();
		log = Logger.getLogger(LdapSample.class);
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		LdapSample ldapSample = new LdapSample();

		// Create LDAP entry manager
		LdapEntryManager ldapEntryManager = ldapSample.createLdapEntryManager();

		// Find all users which have specified object classes defined in SimpleUser
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

		Filter filter = Filter.createEqualityFilter("gluuStatus", "active");
		List<SimpleAttribute> attributes = ldapEntryManager.findEntries("o=gluu", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 0, 0);
		for (SimpleAttribute attribute : attributes) {
			log.debug("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
		}

		List<SimpleSession> sessions = ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, null, null, 0, 0);
		log.debug("Found sessions: " + sessions.size());

		List<SimpleGrant> grants = ldapEntryManager.findEntries("o=gluu", SimpleGrant.class, null, SearchScope.SUB, new String[] { "oxAuthGrantId" }, null, 0, 0);
		log.debug("Found grants: " + grants.size());

		try {
			VirtualListViewResponse virtualListViewResponse = new VirtualListViewResponse();
			SearchResult searchResult = ldapEntryManager.getLdapOperationService().searchSearchResult("o=gluu", Filter.createEqualityFilter("objectClass", "gluuPerson"), SearchScope.SUB, 10, 100, 100000, "displayName", null, virtualListViewResponse, "uid", "displayName", "gluuStatus");

			log.debug("Found persons: " + virtualListViewResponse.getTotalResults());
			System.out.println(searchResult.getSearchEntries());
		} catch (Exception ex) {
			log.error("Failed to search", ex);
		}
	}

	private Properties getSampleConnectionProperties() {
		Properties connectionProperties = new Properties();

		connectionProperties.put("bindDN", "cn=directory manager, o=gluu");
		connectionProperties.put("bindPassword", "9lQoXSINUsnP");
		connectionProperties.put("servers", "xeon.gluu.info:1636");
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
