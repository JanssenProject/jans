package org.gluu.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonPropertyOrder({"active", "scopes", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
@com.fasterxml.jackson.annotation.JsonPropertyOrder({"active", "scopes", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
@IgnoreMediaTypes({"application/*+json"})
public class IntrospectAccessTokenResponse {
    @JsonProperty("active")
    private boolean active;

    @Deprecated
    @JsonProperty("scopes")
    @com.fasterxml.jackson.annotation.JsonProperty("scopes")
    private List<String> scopes;
    @JsonProperty("scope")
    @com.fasterxml.jackson.annotation.JsonProperty("scope")
    private List<String> scope;
    @JsonProperty("client_id")
    @com.fasterxml.jackson.annotation.JsonProperty("client_id")
    private String clientId;
    @JsonProperty("username")
    @com.fasterxml.jackson.annotation.JsonProperty("username")
    private String username;
    @JsonProperty("token_type")
    @com.fasterxml.jackson.annotation.JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("exp")
    @com.fasterxml.jackson.annotation.JsonProperty("exp")
    private Integer expiresAt;
    @JsonProperty("iat")
    @com.fasterxml.jackson.annotation.JsonProperty("iat")
    private Integer issuedAt;
    @JsonProperty("sub")
    @com.fasterxml.jackson.annotation.JsonProperty("sub")
    private String subject;
    @JsonProperty("aud")
    @com.fasterxml.jackson.annotation.JsonProperty("aud")
    private String audience;
    @JsonProperty("iss")
    @com.fasterxml.jackson.annotation.JsonProperty("iss")
    private String issuer;
    @JsonProperty("jti")
    @com.fasterxml.jackson.annotation.JsonProperty("jti")
    private String jti;
    @JsonProperty("acr_values")
    @com.fasterxml.jackson.annotation.JsonProperty("acr_values")
    private String acrValues;

    public IntrospectAccessTokenResponse() {
    }

    public IntrospectAccessTokenResponse(boolean p_active) {
        this.active = p_active;
    }

    public String getAcrValues() {
        return this.acrValues;
    }

    public void setAcrValues(String p_authMode) {
        this.acrValues = p_authMode;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean p_active) {
        this.active = p_active;
    }

    @Deprecated
    public List<String> getScopes() {
        return this.scopes;
    }

    @Deprecated
    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes != null ? new ArrayList(scopes) : new ArrayList();
    }

    public List<String> getScope() {
        return this.scope;
    }

    public void setScope(Collection<String> scope) {
        this.scope = scope != null ? new ArrayList(scope) : new ArrayList();
    }

    public Integer getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getIssuedAt() {
        return this.issuedAt;
    }

    public void setIssuedAt(Integer issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSub(String subject) {
        this.subject = subject;
    }

    public String getAudience() {
        return this.audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJti() {
        return this.jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String toString() {
        return "IntrospectAccessTokenResponse{active=" + this.active + ", scopes=" + this.scopes + ", scope=" + this.scope + ", clientId='" + this.clientId + '\'' + ", username='" + this.username + '\'' + ", tokenType='" + this.tokenType + '\'' + ", expiresAt=" + this.expiresAt + ", issuedAt=" + this.issuedAt + ", subject='" + this.subject + '\'' + ", audience='" + this.audience + '\'' + ", issuer='" + this.issuer + '\'' + ", jti='" + this.jti + '\'' + ", acrValues='" + this.acrValues + '\'' + '}';
    }
}
