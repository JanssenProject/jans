package org.gluu.ldap;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.log.LoggingHelper;
import org.gluu.persist.ldap.impl.LdapEntryManager;
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;

import java.util.Date;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class LdapSampleDelete {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(LdapSampleDelete.class);
    }

    private LdapSampleDelete() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        String baseDn = "ou=cache,o=gluu";
		Filter filter = Filter.createANDFilter(
		        Filter.createEqualityFilter("del", true),
				Filter.createLessOrEqualFilter("exp", ldapEntryManager.encodeTime(baseDn, new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)))
        );

        int result = ldapEntryManager.remove(baseDn, DeletableEntity.class, filter, 100);
        System.out.println(result);
    }

}
