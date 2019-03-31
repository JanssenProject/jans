package org.gluu.oxd.server.introspection;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

/**
 * @author yuriyz
 */
@IgnoreMediaTypes({"application/*+json"})
@JsonPropertyOrder({"active", "exp", "iat", "nbf", "permissions", "client_id", "sub", "aud", "iss", "jti"})
@XmlRootElement
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class BadRptIntrospectionResponse {
    private boolean active;
    private Date expiresAt;
    private Date issuedAt;
    private Date nbf;
    private String clientId;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private List<BadUmaPermission> permissions;

    public BadRptIntrospectionResponse() {
    }

    public BadRptIntrospectionResponse(boolean status) {
        this.active = status;
    }

    @JsonProperty("aud")
    @XmlElement(
            name = "aud"
    )
    public String getAud() {
        return this.aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    @JsonProperty("iss")
    @XmlElement(
            name = "iss"
    )
    public String getIss() {
        return this.iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    @JsonProperty("jti")
    @XmlElement(
            name = "jti"
    )
    public String getJti() {
        return this.jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @JsonProperty("sub")
    @XmlElement(
            name = "sub"
    )
    public String getSub() {
        return this.sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    @JsonProperty("client_id")
    @XmlElement(
            name = "client_id"
    )
    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("active")
    @XmlElement(
            name = "active"
    )
    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean status) {
        this.active = status;
    }

    @JsonProperty("nbf")
    @XmlElement(
            name = "nbf"
    )
    public Date getNbf() {
        return this.nbf;
    }

    public void setNbf(Date nbf) {
        this.nbf = nbf;
    }

    @JsonProperty("exp")
    @XmlElement(
            name = "exp"
    )
    public Date getExpiresAt() {
        return this.expiresAt != null ? new Date(this.expiresAt.getTime()) : null;
    }

    public void setExpiresAt(Date expirationDate) {
        this.expiresAt = expirationDate != null ? new Date(expirationDate.getTime()) : null;
    }

    @JsonProperty("iat")
    @XmlElement(
            name = "iat"
    )
    public Date getIssuedAt() {
        return this.issuedAt != null ? new Date(this.issuedAt.getTime()) : null;
    }

    public void setIssuedAt(Date p_issuedAt) {
        this.issuedAt = p_issuedAt != null ? new Date(p_issuedAt.getTime()) : null;
    }

    @JsonProperty("permissions")
    @XmlElement(
            name = "permissions"
    )
    public List<BadUmaPermission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(List<BadUmaPermission> p_permissions) {
        this.permissions = p_permissions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RptStatusResponse");
        sb.append("{active=").append(this.active);
        sb.append(", expiresAt=").append(this.expiresAt);
        sb.append(", issuedAt=").append(this.issuedAt);
        sb.append(", nbf=").append(this.nbf);
        sb.append(", clientId=").append(this.clientId);
        sb.append(", sub=").append(this.sub);
        sb.append(", aud=").append(this.aud);
        sb.append(", iss=").append(this.iss);
        sb.append(", jti=").append(this.jti);
        sb.append(", permissions=").append(this.permissions);
        sb.append('}');
        return sb.toString();
    }
}
