package org.xdi.oxd.licenser.server.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@LdapEntry
@LdapObjectClass(values = {"top", "oxLicenseConfiguration"})
public class Conf {
    @LdapDN
    private String dn;
    @LdapAttribute(name = "oxLicenseConf")
    private String conf;

    public String getConf() {
        return conf;
    }

    public void setConf(String p_conf) {
        conf = p_conf;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Conf");
        sb.append("{conf='").append(conf).append('\'');
        sb.append(", dn='").append(dn).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
