/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

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
@JsonPropertyOrder({"active", "exp", "iat", "nbf", "permissions", "client_id", "sub", "aud", "iss", "jti"})
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class RptIntrospectionResponse {

    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    private Date expiresAt;
    private Date issuedAt;
    private Date nbf;
    private String clientId;
    private String sub;
    private String aud;
    private String iss;
    private String jti;
    private List<UmaPermission> permissions;

    public RptIntrospectionResponse() {
    }

    public RptIntrospectionResponse(boolean status) {
        this.active = status;
    }

    @JsonProperty(value = "aud")
    @XmlElement(name = "aud")
    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    @JsonProperty(value = "iss")
    @XmlElement(name = "iss")
    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    @JsonProperty(value = "jti")
    @XmlElement(name = "jti")
    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    @JsonProperty(value = "sub")
    @XmlElement(name = "sub")
    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    @JsonProperty(value = "client_id")
    @XmlElement(name = "client_id")
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty(value = "active")
    @XmlElement(name = "active")
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean status) {
        this.active = status;
    }

    @JsonProperty(value = "nbf")
    @XmlElement(name = "nbf")
    public Date getNbf() {
        return nbf;
    }

    public void setNbf(Date nbf) {
        this.nbf = nbf;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Date getExpiresAt() {
        return expiresAt != null ? new Date(expiresAt.getTime()) : null;
    }

    public void setExpiresAt(Date expirationDate) {
        this.expiresAt = expirationDate != null ? new Date(expirationDate.getTime()) : null;
    }

    @JsonProperty(value = "iat")
    @XmlElement(name = "iat")
    public Date getIssuedAt() {
        return issuedAt != null ? new Date(issuedAt.getTime()) : null;
    }

    public void setIssuedAt(Date p_issuedAt) {
        issuedAt = p_issuedAt != null ? new Date(p_issuedAt.getTime()) : null;
    }

    @JsonProperty(value = "permissions")
    @XmlElement(name = "permissions")
    public List<UmaPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<UmaPermission> p_permissions) {
        permissions = p_permissions;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RptStatusResponse");
        sb.append("{active=").append(active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append(", nbf=").append(nbf);
        sb.append(", clientId=").append(clientId);
        sb.append(", sub=").append(sub);
        sb.append(", aud=").append(aud);
        sb.append(", iss=").append(iss);
        sb.append(", jti=").append(jti);
        sb.append(", permissions=").append(permissions);
        sb.append('}');
        return sb.toString();
    }
}
