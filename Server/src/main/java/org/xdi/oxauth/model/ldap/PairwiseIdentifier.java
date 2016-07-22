package org.xdi.oxauth.model.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;

import java.net.URI;

/**
 * @author Javier Rojas Blum
 * @version July 22, 2016
 */
@LdapEntry
@LdapObjectClass(values = {"top", "pairwiseIdentifier"})
public class PairwiseIdentifier extends BaseEntry {

    @LdapAttribute(ignoreDuringUpdate = true, name = "oxId")
    private String id;

    @LdapAttribute(name = "oxSectorIdentifier")
    private String sectorIdentifier;

    public PairwiseIdentifier() {
    }

    public PairwiseIdentifier(String sectorIdentifierUri) {
        this.sectorIdentifier = URI.create(sectorIdentifierUri).getHost();
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PairwiseIdentifier [id=")
                .append(id)
                .append(", sectorIdentifier=").append(sectorIdentifier)
                .append("]");
        return builder.toString();
    }
}
