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
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/01/2016
 */

@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"active", "exp", "iat", "scopes"})
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatIntrospectionResponse {

    private boolean active;   // according spec, must be "active" http://tools.ietf.org/html/draft-richer-oauth-introspection-03#section-2.2
    private Date expiresAt;
    private Date issuedAt;
    private List<String> scopes;

    public GatIntrospectionResponse() {
    }

    public GatIntrospectionResponse(boolean status) {
        this.active = status;
    }

    @JsonProperty(value = "active")
    @XmlElement(name = "active")
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean status) {
        this.active = status;
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

    @JsonProperty(value = "scopes")
    @XmlElement(name = "scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GatStatusResponse");
        sb.append("{active=").append(active);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append(", scopes=").append(scopes);
        sb.append('}');
        return sb.toString();
    }
}
