package org.gluu.oxd.server.introspection;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@IgnoreMediaTypes({"application/*+json"})
@JsonPropertyOrder({"resource_id", "resource_scopes", "exp"})
@JsonIgnoreProperties(
        ignoreUnknown = true
)
@XmlRootElement
public class BadUmaPermission implements Serializable {

    private String resourceId;
    private List<String> scopes;
    private Date expiresAt;
    private Map<String, String> params;

    public BadUmaPermission() {
    }

    public BadUmaPermission(String resourceId, List<String> scopes) {
        this.resourceId = resourceId;
        this.scopes = scopes;
    }

    @JsonProperty("resource_id")
    @XmlElement(
            name = "resource_id"
    )
    public String getResourceId() {
        return this.resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @JsonProperty("resource_scopes")
    @XmlElement(
            name = "resource_scopes"
    )
    public List<String> getScopes() {
        return this.scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty("exp")
    @XmlElement(
            name = "exp"
    )
    public Date getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty("params")
    @XmlElement(
            name = "params"
    )
    public Map<String, String> getParams() {
        return this.params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String toString() {
        return "UmaPermission{resourceId=\'" + this.resourceId + '\'' + ", scopes=" + this.scopes + ", expiresAt=" + this.expiresAt + '}';
    }
}
