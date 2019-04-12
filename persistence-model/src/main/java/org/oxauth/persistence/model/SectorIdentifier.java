package org.oxauth.persistence.model;

import java.io.Serializable;
import java.util.List;

import org.gluu.persist.model.base.BaseEntry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2018
 */
@DataEntry(sortBy = {"id"})
@ObjectClass(values = {"top", "oxSectorIdentifier"})
public class SectorIdentifier extends BaseEntry implements Serializable {

    private static final long serialVersionUID = -2812480357430436514L;

    @AttributeName(name = "oxId", ignoreDuringUpdate = true)
    private String id;

    @AttributeName(name = "oxAuthRedirectURI")
    private List<String> redirectUris;

    @AttributeName(name = "oxAuthClientId")
    private List<String> clientIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
