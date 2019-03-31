package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;

    @JsonProperty(value = "code")
    private String code;
    @JsonProperty(value = "id_token")
    private String id_token;
    @JsonProperty(value = "access_token")
    private String access_token;

    @JsonProperty(value = "nonce")
    private String nonce;
    @JsonProperty(value = "state")
    private String state;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getIdToken() {
        return id_token;
    }

    public void setIdToken(String idToken) {
        this.id_token = idToken;
    }

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String accessToken) {
        this.access_token = accessToken;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    @Override
    public String getOxdId() {
        return oxd_id;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @Override
    public String toString() {
        return "ValidateParams{" +
                "oxd_id='" + oxd_id + '\'' +
                ", code='" + code + '\'' +
                ", id_token='" + id_token + '\'' +
                ", access_token='" + access_token + '\'' +
                ", nonce='" + nonce + '\'' +
                ", state='" + state + '\'' +
                ", protection_access_token='" + protection_access_token + '\'' +
                '}';
    }
}
