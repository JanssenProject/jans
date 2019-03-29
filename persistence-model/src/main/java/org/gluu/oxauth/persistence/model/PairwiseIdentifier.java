package org.gluu.oxauth.persistence.model;

import java.net.URI;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * @author Javier Rojas Blum
 * @version June 30, 2018
 */
@LdapEntry
@LdapObjectClass(values = {"top", "pairwiseIdentifier"})
public class PairwiseIdentifier extends BaseEntry {

    @LdapAttribute(ignoreDuringUpdate = true, name = "oxId")
    private String id;

    @LdapAttribute(name = "oxSectorIdentifier")
    private String sectorIdentifier;

    @LdapAttribute(name = "oxAuthClientId")
    private String clientId;

    public PairwiseIdentifier() {
    }

    public PairwiseIdentifier(String sectorIdentifierUri, String clientId) {
        this.sectorIdentifier = URI.create(sectorIdentifierUri).getHost();
        this.clientId = clientId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSectorIdentifier() {
        return sectorIdentifier;
    }

    public void setSectorIdentifier(String sectorIdentifierUri) {
        this.sectorIdentifier = sectorIdentifierUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PairwiseIdentifier [id=")
                .append(id)
                .append(", sectorIdentifier=").append(sectorIdentifier)
                .append(", clientId=").append(clientId)
                .append("]");
        return builder.toString();
    }
}
