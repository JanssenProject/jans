/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Check access token parameters.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckAccessTokenParams implements HasOxdIdParams {

    /**
     * oxd ID
     */
    @JsonProperty(value = "oxd_id")
    private String oxdId;

    /**
     * Id token
     */
    @JsonProperty(value = "id_token")
    private String idToken;

    /**
     * Access token
     */
    @JsonProperty(value = "access_token")
    private String accessToken;

    /**
     * Constructor
     */
    public CheckAccessTokenParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    /**
     * Gets id token.
     *
     * @return id token
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets id token.
     *
     * @param p_idToken id token
     */
    public void setIdToken(String p_idToken) {
        idToken = p_idToken;
    }

    /**
     * Gets access token.
     *
     * @return access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets access token.
     *
     * @param p_accessToken access token
     */
    public void setAccessToken(String p_accessToken) {
        accessToken = p_accessToken;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckAccessTokenParams");
        sb.append("{oxd_id='").append(oxdId).append('\'');
        sb.append(", idToken='").append(idToken).append('\'');
        sb.append(", accessToken='").append(accessToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
