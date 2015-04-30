package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/01/2014
 */
public class ObtainAatOpResponse implements IOpResponse {

    @JsonProperty(value = "aat_token")
    private String aatToken;
    @JsonProperty(value = "expires_in_seconds")
    private long expiresIn;
    @JsonProperty(value = "aat_refresh_token")
    private String aatRefreshToken;
    @JsonProperty(value = "authorization_code")
    private String authorizationCode;
    @JsonProperty(value = "scope")
    private String scope;

    public ObtainAatOpResponse() {
    }

    public String getAatToken() {
        return aatToken;
    }

    public void setAatToken(String p_patToken) {
        aatToken = p_patToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long p_expiresIn) {
        expiresIn = p_expiresIn;
    }

    public String getAatRefreshToken() {
        return aatRefreshToken;
    }

    public void setAatRefreshToken(String p_patRefreshToken) {
        aatRefreshToken = p_patRefreshToken;
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

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObtainPatOpResponse");
        sb.append("{aatToken='").append(aatToken).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", aatRefreshToken='").append(aatRefreshToken).append('\'');
        sb.append(", authorizationCode='").append(authorizationCode).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}