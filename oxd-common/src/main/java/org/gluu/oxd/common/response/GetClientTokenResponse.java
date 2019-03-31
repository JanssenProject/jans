package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/03/2017
 */

public class GetClientTokenResponse implements IOpResponse {

    @JsonProperty(value = "access_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "access_token")
    private String accessToken;
    @JsonProperty(value = "expires_in")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expires_in")
    private int expiresIn; // expiration time in seconds
    @JsonProperty(value = "refresh_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "refresh_token")
    private String refreshToken;
    @JsonProperty(value = "scope")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope")
    private List<String> scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "GetClientTokenResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshToken=" + refreshToken +
                ", scope=" + scope +
                '}';
    }
}
