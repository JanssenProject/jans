/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.params;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckIdTokenParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "id_token")
    private String id_token;
    @JsonProperty(value = "nonce")
    private String nonce;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "code")
    private String code;
    @JsonProperty(value = "access_token")
    private String access_token;

    public CheckIdTokenParams() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
        sb.append(", code='").append(code).append('\'');
        sb.append(", access_token='").append(access_token).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
