/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenParams implements IParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "discovery_url")
    private String discoveryUrl; // ignore if oxd_id is specified
    @JsonProperty(value = "id_token")
    private String idToken;

    public CheckIdTokenParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String p_idToken) {
        idToken = p_idToken;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckIdTokenParams");
        sb.append("{discoveryUrl='").append(discoveryUrl).append('\'');
        sb.append(", idToken='").append(idToken).append('\'');
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
