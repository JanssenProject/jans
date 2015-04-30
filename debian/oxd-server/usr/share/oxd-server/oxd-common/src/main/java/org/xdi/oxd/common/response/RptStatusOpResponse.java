package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;

import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 21/08/2013
 */

public class RptStatusOpResponse implements IOpResponse {

    @JsonProperty(value = "active")
    private boolean active;
    @JsonProperty(value = "expires_at")
    private Date expiresAt;
    @JsonProperty(value = "issued_at")
    private Date issuedAt;
    @JsonProperty(value = "permissions")
    private List<RegisterPermissionRequest> permissions;

    public RptStatusOpResponse() {
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

    public List<RegisterPermissionRequest> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<RegisterPermissionRequest> p_permissions) {
        permissions = p_permissions;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RptStatusOpResponse");
        sb.append("{active=").append(active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append(", permissions=").append(permissions);
        sb.append('}');
        return sb.toString();
    }
}
