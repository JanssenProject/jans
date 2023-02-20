/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.jans.as.model.common.converter.ListConverter;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 * @version September 30, 2021
 */
@JsonPropertyOrder({"active", "scope", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
// ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectionResponse {

    @JsonProperty(value = "active")
    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    @JsonProperty(value = "scope")
    @JsonDeserialize(converter = ListConverter.class)
    // Force use of List even when value in actual json content is String
    private List<String> scope;
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "username")
    private String username;
    @JsonProperty(value = "token_type")
    private String tokenType;
    @JsonProperty(value = "exp")
    private Integer expiresAt;
    @JsonProperty(value = "iat")
    private Integer issuedAt;
    @JsonProperty(value = "sub")
    private String subject;
    @JsonProperty(value = "aud")
    private String audience;
    @JsonProperty(value = "iss")
    private String issuer;
    @JsonProperty(value = "jti")
    private String jti;
    @JsonProperty(value = "acr")
    private String acr;
    @JsonProperty(value = "auth_time")
    private Integer authTime;

    // DPoP
    @JsonProperty(value = "nbf")
    private Long notBefore;
    @JsonProperty(value = "cnf")
    private Map<String, String> cnf;

    public IntrospectionResponse() {
        // default constructor
    }

    public IntrospectionResponse(boolean active) {
        this.active = active;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public Integer getAuthTime() {
        return authTime;
    }

    public void setAuthTime(Integer authTime) {
        this.authTime = authTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(Collection<String> scope) {
        this.scope = scope != null ? new ArrayList<>(scope) : new ArrayList<>();
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

    public Long getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Long notBefore) {
        this.notBefore = notBefore;
    }

    public Map<String, String> getCnf() {
        return cnf;
    }

    public void setCnf(Map<String, String> cnf) {
        this.cnf = cnf;
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
                ", acr='" + acr + '\'' +
                ", authTime='" + authTime + '\'' +
                '}';
    }
}
