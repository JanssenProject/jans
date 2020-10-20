/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Check access token parameters.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckAccessTokenParams implements HasRpIdParams {

    /**
     * oxd ID
     */
    @JsonProperty(value = "rp_id")
    private String rp_id;

    /**
     * Id token
     */
    @JsonProperty(value = "id_token")
    private String id_token;

    /**
     * Access token
     */
    @JsonProperty(value = "access_token")
    private String access_token;

    /**
     * Constructor
     */
    public CheckAccessTokenParams() {
    }

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
    }

    /**
     * Gets id token.
     *
     * @return id token
     */
    public String getIdToken() {
        return id_token;
    }

    /**
     * Sets id token.
     *
     * @param p_idToken id token
     */
    public void setIdToken(String p_idToken) {
        id_token = p_idToken;
    }

    /**
     * Gets access token.
     *
     * @return access token
     */
    public String getAccessToken() {
        return access_token;
    }

    /**
     * Sets access token.
     *
     * @param p_accessToken access token
     */
    public void setAccessToken(String p_accessToken) {
        access_token = p_accessToken;
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
        sb.append("{rp_id='").append(rp_id).append('\'');
        sb.append(", id_token='").append(id_token).append('\'');
        sb.append(", access_token='").append(access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
