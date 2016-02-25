package org.xdi.oxauth.model.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.util.Date;

/**
 * @author Javier Rojas Blum
 * @version February 24, 2016
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxAuthGrant"})
public class Grant {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "oxAuthGrantId")
    private String id;
    @LdapAttribute(name = "oxAuthCreation")
    private Date creationDate;

    public Grant() {
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Grant grant = (Grant) o;

        if (!dn.equals(grant.dn)) return false;
        if (!id.equals(grant.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dn.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
