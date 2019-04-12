package org.oxauth.persistence.model;

import java.net.URI;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Javier Rojas Blum
 * @version June 30, 2018
 */
@DataEntry
@ObjectClass(values = {"top", "pairwiseIdentifier"})
public class PairwiseIdentifier extends BaseEntry {

    @AttributeName(ignoreDuringUpdate = true, name = "oxId")
    private String id;

    @AttributeName(name = "oxSectorIdentifier")
    private String sectorIdentifier;

    @AttributeName(name = "oxAuthClientId")
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
