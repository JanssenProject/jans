/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/10/2013
 */

public class CheckAccessTokenResponse implements IOpResponse {

    @JsonProperty(value = "active")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "active")
    private boolean active;
    @JsonProperty(value = "expires_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expires_at")
    private Date expiresAt;
    @JsonProperty(value = "issued_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issued_at")
    private Date issuedAt;

    public CheckAccessTokenResponse() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean p_active) {
        active = p_active;
    }

    public Date getExpiresAt() {
        return expiresAt != null ? new Date(expiresAt.getTime()) : null;
    }

    public void setExpiresAt(Date p_expiresAt) {
        expiresAt = p_expiresAt != null ? new Date(p_expiresAt.getTime()) : null;
    }

    public Date getIssuedAt() {
        return issuedAt != null ? new Date(issuedAt.getTime()) : null;
    }

    public void setIssuedAt(Date p_issuedAt) {
        issuedAt = p_issuedAt != null ? new Date(p_issuedAt.getTime()) : null;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckAccessTokenResponse");
        sb.append("{active=").append(active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append('}');
        return sb.toString();
    }
}
