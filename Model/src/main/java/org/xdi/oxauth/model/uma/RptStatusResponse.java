/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * Token status response according to RPT introspection profile:
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 *         Date: 10/24/2012
 */

// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"active", "expires_at", "issued_at", "permissions"})
@XmlRootElement
public class RptStatusResponse {

    private boolean m_active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    private Date expiresAt;
    private Date issuedAt;
    private List<ResourceSetPermissionRequest> m_permissions;

    public RptStatusResponse() {
    }

    public RptStatusResponse(boolean status) {
        this.m_active = status;
    }

    @JsonProperty(value = "active")
    @XmlElement(name = "active")
    // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    public boolean getActive() {
        return m_active;
    }

    public void setActive(boolean status) {
        this.m_active = status;
    }

    @JsonProperty(value = "expires_at")
    @XmlElement(name = "expires_at")
    public Date getExpiresAt() {
        return expiresAt != null ? new Date(expiresAt.getTime()) : null;
    }

    public void setExpiresAt(Date expirationDate) {
        this.expiresAt = expirationDate != null ? new Date(expirationDate.getTime()) : null;
    }

    @JsonProperty(value = "issued_at")
    @XmlElement(name = "issued_at")
    public Date getIssuedAt() {
        return issuedAt != null ? new Date(issuedAt.getTime()) : null;
    }

    public void setIssuedAt(Date p_issuedAt) {
        issuedAt = p_issuedAt != null ? new Date(p_issuedAt.getTime()) : null;
    }

    @JsonProperty(value = "permissions")
    @XmlElement(name = "permissions")
    public List<ResourceSetPermissionRequest> getPermissions() {
        return m_permissions;
    }

    public void setPermissions(List<ResourceSetPermissionRequest> p_permissions) {
        m_permissions = p_permissions;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RptStatusResponse");
        sb.append("{m_active=").append(m_active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append(", m_permissions=").append(m_permissions);
        sb.append('}');
        return sb.toString();
    }
}
