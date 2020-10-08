package org.oxauth.persistence.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import java.net.URI;

/**
 * @author Javier Rojas Blum
 * @version June 30, 2018
 */
@DataEntry
@ObjectClass(value = "pairwiseIdentifier")
public class PairwiseIdentifier extends BaseEntry {

    @AttributeName(ignoreDuringUpdate = true, name = "oxId")
    private String id;

    @AttributeName(name = "oxSectorIdentifier")
    private String sectorIdentifier;

    @AttributeName(name = "oxAuthClientId")
    private String clientId;

    @AttributeName(name = "oxAuthUserId")
    private String userInum;

    public PairwiseIdentifier() {
    }

    public PairwiseIdentifier(String sectorIdentifierUri, String clientId, String userInum) {
        this.sectorIdentifier = URI.create(sectorIdentifierUri).getHost();
        this.clientId = clientId;
        this.userInum = userInum;
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

    public String getUserInum() {
        return userInum;
    }

    public void setUserInum(String userInum) {
        this.userInum = userInum;
    }

    @Override
    public String toString() {
        return "PairwiseIdentifier{" +
                "id='" + id + '\'' +
                ", sectorIdentifier='" + sectorIdentifier + '\'' +
                ", clientId='" + clientId + '\'' +
                ", userInum='" + userInum + '\'' +
                "} " + super.toString();
    }
}
