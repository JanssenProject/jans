package org.xdi.oxauth.model.ldap;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;
import org.xdi.ldap.model.BaseEntry;

/**
 * @author Javier Rojas Blum
 * @version August 21, 2015
 */
@LdapEntry
@LdapObjectClass(values = {"top", "pairwiseIdentifier"})
public class PairwiseIdentifier extends BaseEntry {

    @LdapAttribute(ignoreDuringUpdate = true, name = "oxId")
    private String id;

    @LdapAttribute(name = "oxSectorIdentifierURI")
    private String sectorIdentifierUri;

    public PairwiseIdentifier() {
    }

    public PairwiseIdentifier(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PairwiseIdentifier [id=")
                .append(id)
                .append(", sectorIdentifierUri=").append(sectorIdentifierUri)
                .append("]");
        return builder.toString();
    }
}
