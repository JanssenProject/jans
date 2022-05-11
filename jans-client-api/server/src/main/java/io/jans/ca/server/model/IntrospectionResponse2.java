package io.jans.ca.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.jans.as.model.common.converter.ListConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

@JsonPropertyOrder({"active", "scope", "client_id", "username", "token_type", "exp", "iat", "sub", "aud", "iss", "jti", "acr_values"})
@IgnoreMediaTypes({"application/*+json"})
@XmlRootElement
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class IntrospectionResponse2 {
    @JsonProperty("active")
    @XmlElement(name = "active")
    private boolean active;
    @JsonProperty("scope")
    @XmlElement(name = "scope")
    @JsonDeserialize(
            converter = ListConverter.class
    )
    private List<String> scope;
    @JsonProperty("client_id")
    @XmlElement(name = "client_id")
    private String clientId;
    @JsonProperty("username")
    @XmlElement(name = "username")
    private String username;
    @JsonProperty("token_type")
    @XmlElement(name = "token_type")
    private String tokenType;
    @JsonProperty("exp")
    @XmlElement(name = "exp")
    private Integer expiresAt;
    @JsonProperty("iat")
    @XmlElement(name = "iat")
    private Integer issuedAt;
    @JsonProperty("sub")
    @XmlElement(name = "sub")
    private String subject;
    @JsonProperty("aud")
    @XmlElement(name = "aud")
    private String audience;
    @JsonProperty("iss")
    @XmlElement(name = "iss")
    private String issuer;
    @JsonProperty("jti")
    @XmlElement(name = "jti")
    private String jti;
    @JsonProperty("acr_values")
    @XmlElement(name = "arc_values")
    private String acrValues;
    @JsonProperty("nbf")
    @XmlElement(name = "nbf")
    private Long notBefore;
    @JsonProperty("cnf")
    @XmlElement(name = "cnf")
    private Map<String, String> cnf;

    public IntrospectionResponse2() {
    }

    public IntrospectionResponse2(boolean active) {
        this.active = active;
    }

    public String getAcrValues() {
        return this.acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public Long getNotBefore() {
        return this.notBefore;
    }

    public void setNotBefore(Long notBefore) {
        this.notBefore = notBefore;
    }

    public Map<String, String> getCnf() {
        return this.cnf;
    }

    public void setCnf(Map<String, String> cnf) {
        this.cnf = cnf;
    }

    public String toString() {
        return "IntrospectionResponse{active=" + this.active + ", scope=" + this.scope + ", clientId='" + this.clientId + '\'' + ", username='" + this.username + '\'' + ", tokenType='" + this.tokenType + '\'' + ", expiresAt=" + this.expiresAt + ", issuedAt=" + this.issuedAt + ", subject='" + this.subject + '\'' + ", audience='" + this.audience + '\'' + ", issuer='" + this.issuer + '\'' + ", jti='" + this.jti + '\'' + ", acrValues='" + this.acrValues + '\'' + '}';
    }
}
