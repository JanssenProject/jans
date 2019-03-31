/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckIdTokenParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "id_token")
    private String id_token;
    @JsonProperty(value = "nonce")
    private String nonce;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public CheckIdTokenParams() {
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getIdToken() {
        return id_token;
    }

    public void setIdToken(String p_idToken) {
        id_token = p_idToken;
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
        sb.append("{id_token='").append(id_token).append('\'');
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", nonce='").append(nonce).append('\'');
        sb.append(", protection_access_token='").append(protection_access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
