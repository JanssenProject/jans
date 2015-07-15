package org.gluu.site.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/07/2015
 */

@LdapEntry
@LdapObjectClass(values = {"top"})
public class DummyEntry {

    @LdapDN
    private String dn;

    public DummyEntry() {
    }

    public DummyEntry(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}
