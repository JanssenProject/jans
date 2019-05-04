/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/09/2013
 */
@JsonPropertyOrder({"active", "scope", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@ApiModel(value = "RPT introspection endpoint")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectionResponse {

    @JsonProperty(value = "active")
    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2

    @JsonProperty(value = "scope")
    @ApiModelProperty(value = " An array referencing zero or more strings representing scopes to which access was granted for this resource. Each string MUST correspond to a scope that was registered by this resource server for the referenced resource.", required = true)
    private List<String> scope;
    
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "username")
    private String username;
    @JsonProperty(value = "token_type")
    private String tokenType;
    
    @JsonProperty(value = "exp")
    @ApiModelProperty(value = "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this permission will expire. If the token-level exp value pre-dates a permission-level exp value, the token-level value takes precedence.", required = false)
    private Integer expiresAt;
    
    @JsonProperty(value = "iat")
    @ApiModelProperty(value = "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this permission was originally issued. If the token-level iat value post-dates a permission-level iat value, the token-level value takes precedence.", required = false)   
    private Integer issuedAt;
    
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

    public IntrospectionResponse() {
    }

    public IntrospectionResponse(boolean p_active) {
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

    public List<String> getScope() {
        return scope;
    }

    public void setScope(Collection<String> scope) {
        this.scope = scope != null ? new ArrayList<String>(scope) : new ArrayList<String>();;
    }

    public Integer getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Integer issuedAt) {
        this.issuedAt = issuedAt;
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

    public void setSub(String subject) {
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
        return "IntrospectionResponse{" +
                "active=" + active +
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
