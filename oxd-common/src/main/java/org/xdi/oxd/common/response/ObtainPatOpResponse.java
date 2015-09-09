/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/08/2013
 */

public class ObtainPatOpResponse implements IOpResponse {

    @JsonProperty(value = "pat_token")
    private String patToken;
    @JsonProperty(value = "expires_in_seconds")
    private long expiresIn;
    @JsonProperty(value = "pat_refresh_token")
    private String patRefreshToken;
    @JsonProperty(value = "authorization_code")
    private String authorizationCode;
    @JsonProperty(value = "scope")
    private String scope;

    public ObtainPatOpResponse() {
    }

    public String getPatToken() {
        return patToken;
    }

    public void setPatToken(String p_patToken) {
        patToken = p_patToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long p_expiresIn) {
        expiresIn = p_expiresIn;
    }

    public String getPatRefreshToken() {
        return patRefreshToken;
    }

    public void setPatRefreshToken(String p_patRefreshToken) {
        patRefreshToken = p_patRefreshToken;
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
        sb.append("{patToken='").append(patToken).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", patRefreshToken='").append(patRefreshToken).append('\'');
        sb.append(", authorizationCode='").append(authorizationCode).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
