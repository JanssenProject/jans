package org.gluu.oxd.common.introspection;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * @author yuriyz
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"resource_id", "resource_scopes", "exp"})
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class CorrectUmaPermission implements Serializable {

    private String resourceId;
    private List<String> scopes;
    private Integer expiresAt;

    public CorrectUmaPermission() {
    }

    public CorrectUmaPermission(String resourceId, List<String> scopes) {
        this.resourceId = resourceId;
        this.scopes = scopes;
    }

    @JsonProperty(value = "resource_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource_id")
    @XmlElement(name = "resource_id")
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @JsonProperty(value = "resource_scopes")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource_scopes")
    @XmlElement(name = "resource_scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
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

    @Override
    public String toString() {
        return "UmaPermission{" +
                "resourceId='" + resourceId + '\'' +
                ", scopes=" + scopes +
                ", expiresAt=" + expiresAt +
                '}';
    }
}