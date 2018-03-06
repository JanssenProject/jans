package org.xdi.oxauth.model.ldap;

import java.io.Serializable;
import java.util.List;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@LdapEntry(sortBy = {"id"})
@LdapObjectClass(values = {"top", "oxSectorIdentifier"})
public class SectorIdentifier extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -2812480357430436514L;

    @LdapAttribute(name = "oxId", ignoreDuringUpdate = true)
    private String id;

    @LdapAttribute(name = "oxAuthRedirectURI")
    private List<String> redirectUris;

    @LdapAttribute(name = "oxAuthClientId")
    private List<String> clientIds;

    public String getInum() {
        return id;
    }

    public void setInum(String id) {
        this.id = id;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(List<String> clientIds) {
        this.clientIds = clientIds;
    }

    @Override
    public String toString() {
        return String
                .format("OxAuthSectorIdentifier [id=%s, toString()=%s]",
                        id, super.toString());
    }
}
