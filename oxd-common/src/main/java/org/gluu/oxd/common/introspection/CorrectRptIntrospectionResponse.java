package org.gluu.oxd.common.introspection;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author yuriyz
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"active", "exp", "iat", "nbf", "permissions", "client_id", "sub", "aud", "iss", "jti"})
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class CorrectRptIntrospectionResponse {

    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    private Integer expiresAt;
    private Integer issuedAt;
    private Integer nbf;
    private String clientId;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private List<CorrectUmaPermission> permissions;

    public CorrectRptIntrospectionResponse() {
    }

    public CorrectRptIntrospectionResponse(boolean status) {
        this.active = status;
    }

    @JsonProperty(value = "aud")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "aud")
    @XmlElement(name = "aud")
    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    @JsonProperty(value = "iss")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "iss")
    @XmlElement(name = "iss")
    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    @JsonProperty(value = "jti")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jti")
    @XmlElement(name = "jti")
    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @JsonProperty(value = "sub")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sub")
    @XmlElement(name = "sub")
    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    @JsonProperty(value = "client_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_id")
    @XmlElement(name = "client_id")
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty(value = "active")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "active")
    @XmlElement(name = "active")
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean status) {
        this.active = status;
    }

    @JsonProperty(value = "nbf")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nbf")
    @XmlElement(name = "nbf")
    public Integer getNbf() {
        return nbf;
    }

    public void setNbf(Integer nbf) {
        this.nbf = nbf;
    }

    @JsonProperty(value = "exp")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Integer getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty(value = "iat")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "iat")
    @XmlElement(name = "iat")
    public Integer getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Integer p_issuedAt) {
        issuedAt = p_issuedAt;
    }

    @JsonProperty(value = "permissions")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permissions")
    @XmlElement(name = "permissions")
    public List<CorrectUmaPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<CorrectUmaPermission> p_permissions) {
        permissions = p_permissions;
    }

    @Override
    public String toString() {
        return "RptStatusResponse" +
                "{active=" + active +
                ", expiresAt=" + expiresAt +
                ", issuedAt=" + issuedAt +
                ", nbf=" + nbf +
                ", clientId=" + clientId +
                ", sub=" + sub +
                ", aud=" + aud +
                ", iss=" + iss +
                ", jti=" + jti +
                ", permissions=" + permissions +
                '}';
    }
}
