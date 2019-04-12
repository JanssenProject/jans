package org.gluu.service.cache;

import org.gluu.persist.model.base.Deletable;
import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.LdapDN;
import org.gluu.persist.annotation.LdapEntry;
import org.gluu.persist.annotation.LdapObjectClass;

import java.io.Serializable;
import java.util.Date;

@Entry
@ObjectClass(values = {"top", "oxCacheEntity"})
public class NativePersistenceCacheEntity implements Serializable, Deletable {

    @DN
    private String dn;
    @Attribute(name = "uniqueIdentifier")
    private String id;
    @Attribute(name = "oxAuthCreation")
    private Date creationDate;
    @Attribute(name = "oxAuthExpiration")
    private Date expirationDate;
    @Attribute(name = "oxDeletable")
    private boolean deletable = true;
    @Attribute(name = "oxData")
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

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public String toString() {
        return "NativePersistenceCacheEntity{" +
                "dn='" + dn + '\'' +
                ", id='" + id + '\'' +
                ", creationDate=" + creationDate +
                ", expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                ", data='" + data + '\'' +
                '}';
    }
}
