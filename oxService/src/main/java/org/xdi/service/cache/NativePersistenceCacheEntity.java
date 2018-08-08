package org.xdi.service.cache;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;
import java.util.Date;

@LdapEntry
@LdapObjectClass(values = {"top", "oxCacheEntity"})
public class NativePersistenceCacheEntity implements Serializable {

    @LdapDN
    private String dn;
    @LdapAttribute(name = "uniqueIdentifier")
    private String id;
    @LdapAttribute(name = "oxAuthCreation")
    private Date creationDate;
    @LdapAttribute(name = "oxAuthExpiration")
    private Date expirationDate;
    @LdapAttribute(name = "oxData")
    private String data;

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

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "NativePersistenceCacheEntity{" +
                "dn='" + dn + '\'' +
                ", id='" + id + '\'' +
                ", creationDate=" + creationDate +
                ", expirationDate=" + expirationDate +
                ", data='" + data + '\'' +
                '}';
    }
}
