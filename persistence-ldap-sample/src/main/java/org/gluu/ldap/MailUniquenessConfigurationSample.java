package org.gluu.ldap;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.ldap.model.MailUniquenessConfiguration;
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
public final class MailUniquenessConfigurationSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(MailUniquenessConfigurationSample.class);
    }

    private MailUniquenessConfigurationSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

        // Create LDAP entry manager
        LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

        MailUniquenessConfiguration conf = ldapEntryManager.find("cn=Unique mail address,cn=Plugins,cn=config", MailUniquenessConfiguration.class, null);
        System.out.println("Current mail uniqueness: " + conf.isEnabled());
        
        conf.setEnabled(!conf.isEnabled());

        // Upate configuration in LDAP
        ldapEntryManager.merge(conf);

        MailUniquenessConfiguration conf2 = ldapEntryManager.find("cn=Unique mail address,cn=Plugins,cn=config", MailUniquenessConfiguration.class, null);
        System.out.println("After update mail uniqueness: " + conf2.isEnabled());
    }

}
