package org.gluu.oxd.common.response;

import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2015
 */

public class GetUserInfoResponse implements IOpResponse {

    @JsonProperty("claims")
    @com.fasterxml.jackson.annotation.JsonProperty("claims")
    private Map<String, List<String>> claims = Maps.newHashMap();
    @JsonProperty("refresh_token")
    @com.fasterxml.jackson.annotation.JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("access_token")
    @com.fasterxml.jackson.annotation.JsonProperty("access_token")
    private String accessToken;

    public GetUserInfoResponse() {
    }

    public GetUserInfoResponse(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "GetUserInfoResponse{" +
                "claims=" + claims +
                ", refreshToken='" + refreshToken + '\'' +
                ", accessToken='" + accessToken + '\'' +
                '}';
    }
}
