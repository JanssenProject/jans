/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */
@JsonPropertyOrder({"active", "exp", "iat", "auth_mode", "auth_level"})
// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
public class IntrospectionResponse {

    @JsonProperty(value = "active")
    private boolean m_active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    @JsonProperty(value = "exp")
    private Date expiresAt;
    @JsonProperty(value = "iat")
    private Date issuedAt;
    @JsonProperty(value = "auth_mode")
    private String authMode;
    @JsonProperty(value = "auth_level")
    private String authLevel;

    public IntrospectionResponse() {
    }

    public IntrospectionResponse(boolean p_active) {
        m_active = p_active;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String p_authMode) {
        authMode = p_authMode;
    }

    public String getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(String p_authLevel) {
        authLevel = p_authLevel;
    }

    public boolean isActive() {
        return m_active;
    }

    public void setActive(boolean p_active) {
        m_active = p_active;
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
        issuedAt = p_issuedAt;
    }
}
