package org.xdi.oxd.licenser.server.service;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.js.Configuration;
import org.xdi.oxd.licenser.server.ldap.LdapOu;
import org.xdi.oxd.licenser.server.ldap.LdapStructure;

import java.util.Arrays;

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

        LdapOu licenseCryptBaseDn = new LdapOu();
        licenseCryptBaseDn.setDn(ldapStructure.getLicenseCryptBaseDn());
        licenseCryptBaseDn.setOu(ldapStructure.getLicenseCryptOu());

        for (LdapOu ldapOu : Arrays.asList(customerBaseDn, licenseIdBaseDn, licenseCryptBaseDn)) {
            if (!ldapEntryManager.contains(ldapOu)) {
                ldapEntryManager.persist(ldapOu);
                LOG.info("Created: " + ldapOu.getDn());
            }
        }
    }
}
