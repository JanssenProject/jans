package org.xdi.oxd.licenser.server.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/09/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "organizationalUnit"})
public class LdapOu implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "ou")
    private String ou;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getOu() {
        return ou;
    }

    public void setOu(String ou) {
        this.ou = ou;
    }
}
