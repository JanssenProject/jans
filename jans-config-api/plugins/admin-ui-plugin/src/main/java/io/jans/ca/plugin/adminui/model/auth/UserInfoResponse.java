package io.jans.ca.plugin.adminui.model.auth;

import java.util.Map;

public class UserInfoResponse {
    private Map<String, Object> claims;
    private String jwtUserInfo;

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    public void addClaims(String key, Object value) {
        this.claims.put(key, value);
    }

    public String getJwtUserInfo() {
        return jwtUserInfo;
    }

    public void setJwtUserInfo(String jwtUserInfo) {
        this.jwtUserInfo = jwtUserInfo;
    }

    @Override
    public String toString() {
        return "UserInfoResponse{" +
                "claims=" + claims +
                ", jwtUserInfo='" + jwtUserInfo + '\'' +
                '}';
    }
}
