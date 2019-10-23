package org.gluu.oxauth.model.ldap;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Javier Rojas Blum
 * @version October 16, 2015
 */
@DataEntry
@ObjectClass(value = "oxClientAuthorization")
public class ClientAuthorization implements Serializable {

    @DN
    private String dn;

    @AttributeName(name = "oxId")
    private String id;

    @AttributeName(name = "oxAuthClientId", consistency = true)
    private String clientId;

    @AttributeName(name = "oxAuthUserId", consistency = true)
    private String userId;

    @AttributeName(name = "oxAuthScope")
    private String[] scopes;

    @AttributeName(name = "oxAuthExpiration")
    private Date expirationDate;

    @AttributeName(name = "del")
    private boolean deletable = true;

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientAuthorization that = (ClientAuthorization) o;

        if (!dn.equals(that.dn)) return false;
        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dn.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
