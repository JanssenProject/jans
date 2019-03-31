/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Check access token parameters.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckAccessTokenParams implements HasProtectionAccessTokenParams {

    /**
     * oxd ID
     */
    @JsonProperty(value = "oxd_id")
    private String oxd_id;

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
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    /**
     * Constructor
     */
    public CheckAccessTokenParams() {
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
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

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
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
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", id_token='").append(id_token).append('\'');
        sb.append(", access_token='").append(access_token).append('\'');
        sb.append(", protection_access_token='").append(protection_access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
