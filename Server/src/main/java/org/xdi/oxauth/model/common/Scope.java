package org.xdi.oxauth.model.common;

import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Javier Rojas Blum Date: 07.05.2012
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthCustomScope"})
public class Scope {

    private static final long serialVersionUID = 4308826784917052508L;
    private transient boolean isDefault;

    @LdapDN
    private String dn;
    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute
    private String displayName;

    @LdapAttribute
    private String description;
 
    @LdapAttribute(name = "oxAuthClaim")
    private List<String> oxAuthClaims;

    @LdapAttribute(name = "defaultScope")
    private String defaultScope;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getInum() {
        return this.inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getOxAuthClaims() {
        return this.oxAuthClaims;
    }

    public void setOxAuthClaims(List<String> oxAuthClaims) {
        this.oxAuthClaims = oxAuthClaims;
    }

    public String getDefaultScope() {
        return this.defaultScope;
    }

    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }

    public boolean getIsDefault() {
        if (this.defaultScope == null) {
            return false;
        }
        if (this.defaultScope.equalsIgnoreCase("true")) {
            this.isDefault = true;
            return this.isDefault;
        }
        this.isDefault = false;
        return this.isDefault;
    }

    @Override
    public String toString() {
        return String.format(
                "Scope [description=%s, displayName=%s, inum=%s, oxAuthClaims=%s, defaultScope=%s, toString()=%s]",
                description, displayName, inum, oxAuthClaims, defaultScope, super.toString());
    }
}