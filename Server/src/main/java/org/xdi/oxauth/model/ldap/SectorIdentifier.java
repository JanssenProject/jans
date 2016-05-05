package org.xdi.oxauth.model.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;

import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@LdapEntry(sortBy = {"inum"})
@LdapObjectClass(values = {"top", "oxSectorIdentifier"})
public class SectorIdentifier extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -2812480357430436514L;

    @LdapAttribute(ignoreDuringUpdate = true)
    private String inum;

    @LdapAttribute(name = "oxAuthRedirectURI")
    private List<String> redirectUris;

    @LdapAttribute(name = "oxAuthClientId")
    private List<String> clientIds;

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
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
                .format("OxAuthSectorIdentifier [inum=%s, toString()=%s]",
                        inum, super.toString());
    }
}
