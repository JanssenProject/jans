package io.jans.orm.ldap;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.model.SimpleUser;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 2021/09/08
 */
public final class LdapUserFieldRemove {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapUserFieldRemove.class);
    }

    private LdapUserFieldRemove() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapEntryManagerSample ldapSampleEntryManager = new LdapEntryManagerSample();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        // Find all users which have specified object classes defined in SimpleUser
        Filter filter = Filter.createEqualityFilter("uid", "admin");
        List<SimpleUser> users = ldapEntryManager.findEntries("o=jans", SimpleUser.class, filter);
        if (users.size() == 0) {
            LOG.error("Failed to find user by filter: " + filter);
            return;
        }

        LOG.debug("Find user by filter: " + filter);

        // Add dummy oxEnrollmentCode attribute
        SimpleUser user = users.get(0);
        user.setAttribute("jansEnrollmentCode", "test-enrollment-code", false);
        ldapEntryManager.merge(user);

        // Reload user by DN
        SimpleUser userWithEnrollment = ldapEntryManager.find(SimpleUser.class, user.getDn());
        if (users.size() == 0) {
            LOG.error("Failed to find user by DN: " + user.getDn());
            return;
        }

        // Clean dummy oxEnrollmentCode attribute
        userWithEnrollment.setAttribute("jansEnrollmentCode", "", false);
        ldapEntryManager.merge(userWithEnrollment);

        // Reload user by DN
        SimpleUser userWithoutEnrollment = ldapEntryManager.find(SimpleUser.class, user.getDn());
        if (users.size() == 0) {
            LOG.error("Failed to find user by DN: " + user.getDn());
            return;
        }
        
        String enrollmentCode = userWithoutEnrollment.getAttribute("jansEnrollmentCode");
        LOG.debug("jansEnrollmentCode: " + enrollmentCode);
    }

}
