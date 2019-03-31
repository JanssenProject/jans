/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/10/2013
 */

public class CheckIdTokenResponse implements IOpResponse {

    @JsonProperty(value = "active")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "active")
    private boolean active;
    @JsonProperty(value = "expires_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expires_at")
    private long expiresAt;
    @JsonProperty(value = "issued_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issued_at")
    private long issuedAt;
    @JsonProperty("claims")
    @com.fasterxml.jackson.annotation.JsonProperty("claims")
    private Map<String, List<String>> claims;

    public CheckIdTokenResponse() {
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> p_claims) {
        claims = p_claims;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean p_active) {
        active = p_active;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long p_expiresAt) {
        expiresAt = p_expiresAt;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long p_issuedAt) {
        issuedAt = p_issuedAt;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckIdTokenResponse");
        sb.append("{active=").append(active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append('}');
        return sb.toString();
    }
}
