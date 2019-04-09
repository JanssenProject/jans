package org.gluu.ldap;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.ldap.model.SimpleAttribute;
import org.gluu.ldap.model.SimpleGrant;
import org.gluu.ldap.model.SimpleSession;
import org.gluu.ldap.model.SimpleUser;
import org.gluu.log.LoggingHelper;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.SortOrder;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class LdapSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapSample.class);
    }

    private LdapSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        // Find all users which have specified object classes defined in SimpleUser
        List<SimpleUser> users = ldapEntryManager.findEntries("o=gluu", SimpleUser.class, null);
        for (SimpleUser user : users) {
            LOG.debug("User with uid: " + user.getUserId());
        }

        if (users.size() > 0) {
            // Add attribute "streetAddress" to first user
            SimpleUser user = users.get(0);
            user.getCustomAttributes().add(new CustomAttribute("streetAddress", "Somewhere: " + System.currentTimeMillis()));

            ldapEntryManager.merge(user);
        }

        Filter filter = Filter.createEqualityFilter("gluuStatus", "active");
        List<SimpleAttribute> attributes = ldapEntryManager.findEntries("o=gluu", SimpleAttribute.class, filter, SearchScope.SUB, null, null, 10, 0,
                0);
        for (SimpleAttribute attribute : attributes) {
            LOG.debug("Attribute with displayName: " + attribute.getCustomAttributes().get(1));
        }

        List<SimpleSession> sessions = ldapEntryManager.findEntries("o=gluu", SimpleSession.class, filter, SearchScope.SUB, null, null, 10, 0, 0);
        LOG.debug("Found sessions: " + sessions.size());

        List<SimpleGrant> grants = ldapEntryManager.findEntries("o=gluu", SimpleGrant.class, null, SearchScope.SUB, new String[] { "oxAuthGrantId" },
                null, 10, 0, 0);
        LOG.debug("Found grants: " + grants.size());

        try {
            PagedResult<SimpleUser> vlvResponse = ldapEntryManager.findPagedEntries("o=gluu", SimpleUser.class, null,
                    new String[] { "uid", "displayName", "gluuStatus" }, "displayName", SortOrder.ASCENDING, 10, 100000, 1000);

            LOG.debug("Found persons: " + vlvResponse.getTotalEntriesCount());
            System.out.println(vlvResponse.getEntries().size());
        } catch (Exception ex) {
            LOG.error("Failed to search", ex);
        }
    }

}
