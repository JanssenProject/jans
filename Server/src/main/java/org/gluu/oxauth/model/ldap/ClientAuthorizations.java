package org.gluu.oxauth.model.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapDN;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version October 16, 2015
 */
@LdapEntry
@LdapObjectClass(values = {"top", "oxClientAuthorizations"})
public class ClientAuthorizations implements Serializable {

    @LdapDN
    private String dn;

    @LdapAttribute(name = "oxId")
    private String id;

    @LdapAttribute(name = "oxAuthClientId")
    private String clientId;

    @LdapAttribute(name = "oxAuthScope")
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
