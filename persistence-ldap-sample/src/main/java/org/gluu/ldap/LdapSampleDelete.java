package org.gluu.ldap;

import java.util.Date;
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
import org.gluu.persist.model.base.DeletableEntity;
import org.gluu.search.filter.Filter;

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
		        Filter.createEqualityFilter("oxDeletable", true),
				Filter.createLessOrEqualFilter("oxAuthExpiration", ldapEntryManager.encodeTime(baseDn, new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)))
        );

        int result = ldapEntryManager.remove(baseDn, DeletableEntity.class, filter, 100);
        System.out.println(result);
    }

}
