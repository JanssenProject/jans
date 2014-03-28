package org.xdi.oxauth.model.common;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Javier Rojas Blum Date: 07.10.2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "gluuAttribute"})
public class GluuAttribute {

    private static final long serialVersionUID = 4817004894646725606L;

    @LdapDN
    private String dn;
    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "gluuAttributeName")
    private String name;

    @LdapAttribute
    private String displayName;

    @LdapAttribute
    private String description;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "GluuAttribute [inum=" + inum + ", name=" + name
                + ", displayName=" + displayName + ", description="
                + description + ", toString()=" + super.toString() + "]";
    }
}