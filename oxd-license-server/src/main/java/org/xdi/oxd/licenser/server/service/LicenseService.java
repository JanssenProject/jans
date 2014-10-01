package org.xdi.oxd.licenser.server.service;

import com.google.inject.Inject;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.oxd.licenser.server.conf.Configuration;
import org.xdi.oxd.licenser.server.ldap.LdapStructure;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2014
 */

public class LicenseService {
    @Inject
    LdapEntryManager ldapEntryManager;
    @Inject
    Configuration conf;
    @Inject
    LdapStructure ldapStructure;
}
