package org.gluu.oxd.server.introspection;

/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */
@JsonPropertyOrder({"active", "scopes", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
public class BackCompatibleIntrospectionResponse {

    @JsonProperty(value = "active")
    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    @Deprecated // redundant, in spec we have just "scope", leave it for back compatiblity
    @JsonProperty(value = "scopes")
    private List<String> scopes;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "username")
    private String username;
    @JsonProperty(value = "token_type")
    private String tokenType;
    @JsonProperty(value = "exp")
    private Date expiresAt;
    @JsonProperty(value = "iat")
    private Date issuedAt;
    @JsonProperty(value = "sub")
    private String subject;
    @JsonProperty(value = "aud")
    private String audience;
    @JsonProperty(value = "iss")
    private String issuer;
    @JsonProperty(value = "jti")
    private String jti;
    @JsonProperty(value = "acr_values")
    private String acrValues;

    public BackCompatibleIntrospectionResponse() {
    }

    public BackCompatibleIntrospectionResponse(boolean p_active) {
        active = p_active;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String p_authMode) {
        acrValues = p_authMode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean p_active) {
        active = p_active;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes != null ? new ArrayList<String>(scopes) : new ArrayList<String>();
    }

    public List<String> getScope() {
        if (scope == null) {
            scope = new ArrayList<>();
        }
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @Override
    public String toString() {
        return "BackCompatibleIntrospectionResponse{" +
                "active=" + active +
                ", scopes=" + scopes +
                ", scope=" + scope +
                ", clientId='" + clientId + '\'' +
                ", username='" + username + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresAt=" + expiresAt +
                ", issuedAt=" + issuedAt +
                ", subject='" + subject + '\'' +
                ", audience='" + audience + '\'' +
                ", issuer='" + issuer + '\'' +
                ", jti='" + jti + '\'' +
                ", acrValues='" + acrValues + '\'' +
                '}';
    }
}
