package org.xdi.oxd.licenser.server.ldap;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.licenser.server.conf.Configuration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/09/2014
 */

public class LdapStructureChecker {

    private static final Logger LOG = LoggerFactory.getLogger(LdapStructureChecker.class);

    private LdapEntryManager ldapEntryManager;
    private LdapStructure ldapStructure;

    public LdapStructureChecker(LdapEntryManager ldapEntryManager, Configuration conf) {
        this.ldapEntryManager = ldapEntryManager;
        this.ldapStructure = new LdapStructure(ldapEntryManager, conf);
    }

    public void checkLdapStructure() {
        LdapOu customerBaseDn = new LdapOu();
        customerBaseDn.setDn(ldapStructure.getCustomerBaseDn());
        customerBaseDn.setOu(ldapStructure.getCustomerOu());

        LdapOu licenseIdBaseDn = new LdapOu();
        licenseIdBaseDn.setDn(ldapStructure.getLicenseIdBaseDn());
        licenseIdBaseDn.setOu(ldapStructure.getLicenseIdOu());

        if (!ldapEntryManager.contains(customerBaseDn)) {
            ldapEntryManager.persist(customerBaseDn);
            LOG.info("Created: " + customerBaseDn.getDn());
        }

        if (!ldapEntryManager.contains(licenseIdBaseDn)) {
            ldapEntryManager.persist(licenseIdBaseDn);
            LOG.info("Created: " + licenseIdBaseDn.getDn());
        }
    }

}
