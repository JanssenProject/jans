package org.gluu.ldap;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.VirtualListViewResponse;
import org.xdi.log.LoggingHelper;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResult;

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
		LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

		// Create LDAP entry manager
		LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

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
		List<SimpleAttribute> attributes = ldapEntryManager.findEntries("o=gluu", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 10, 0, 0);
		for (SimpleAttribute attribute : attributes) {
			log.debug("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
		}

		List<SimpleSession> sessions = ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, null, null, 10, 0, 0);
		log.debug("Found sessions: " + sessions.size());

		List<SimpleGrant> grants = ldapEntryManager.findEntries("o=gluu", SimpleGrant.class, null, SearchScope.SUB, new String[] { "oxAuthGrantId" }, null, 10, 0, 0);
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


}
