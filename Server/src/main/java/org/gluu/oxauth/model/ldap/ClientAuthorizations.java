package org.gluu.oxauth.model.ldap;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version October 16, 2015
 */
@DataEntry
@ObjectClass(values = {"top", "oxClientAuthorizations"})
public class ClientAuthorizations implements Serializable {

    @DN
    private String dn;

    @AttributeName(name = "oxId")
    private String id;

    @AttributeName(name = "oxAuthClientId")
    private String clientId;

    @AttributeName(name = "oxAuthScope")
    private String[] scopes;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientAuthorizations that = (ClientAuthorizations) o;

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
