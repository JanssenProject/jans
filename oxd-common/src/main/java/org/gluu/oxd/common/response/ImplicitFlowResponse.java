/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

public class ImplicitFlowResponse implements IOpResponse {

    @JsonProperty(value = "access_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "access_token")
    private String accessToken;
    @JsonProperty(value = "expires_in_seconds")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expires_in_seconds")
    private long expiresIn;
    @JsonProperty(value = "refresh_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "refresh_token")
    private String refreshToken;
    @JsonProperty(value = "authorization_code")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorization_code")
    private String authorizationCode;
    @JsonProperty(value = "scope")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope")
    private String scope;
    @JsonProperty(value = "id_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id_token")
    private String idToken;


    public ImplicitFlowResponse() {
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String p_patToken) {
        accessToken = p_patToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long p_expiresIn) {
        expiresIn = p_expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String p_patRefreshToken) {
        refreshToken = p_patRefreshToken;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String p_authorizationCode) {
        authorizationCode = p_authorizationCode;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String p_scope) {
        scope = p_scope;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ImplicitFlowResponse");
        sb.append("{accessToken='").append(accessToken).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", refreshToken='").append(refreshToken).append('\'');
        sb.append(", authorizationCode='").append(authorizationCode).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", idToken='").append(idToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}