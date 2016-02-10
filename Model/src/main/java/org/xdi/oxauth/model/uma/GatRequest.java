package org.xdi.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/01/2016
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"scopes"})
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatRequest {

    private List<String> scopes;
    private ClaimTokenList claims;

    public GatRequest() {
    }

    public GatRequest(List<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty(value = "scopes")
    @XmlElement(name = "scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public ClaimTokenList getClaims() {
        return claims;
    }

    public void setClaims(ClaimTokenList claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("GatRequest");
        sb.append("{scopes=").append(scopes);
        sb.append("{claims=").append(claims);
        sb.append('}');
        return sb.toString();
    }
}
