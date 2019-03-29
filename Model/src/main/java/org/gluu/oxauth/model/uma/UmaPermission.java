/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * UMA Permission. Used for both:
 * 1. register permission ticket
 * 2. by introspection RPT endpoint to return RPT status.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 */

// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"resource_id", "resource_scopes", "exp"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@ApiModel(value = "Register permission request.")
public class UmaPermission implements Serializable {

    @ApiModelProperty(value = "The identifier for a resource to which this client is seeking access. The identifier MUST correspond to a resource that was previously registered."
            , required = true)
    private String resourceId;
    @ApiModelProperty(value = "An array referencing one or more identifiers of scopes to which access is needed for this resource. Each scope identifier MUST correspond to a scope that was registered by RS for the referenced resource."
            , required = true)
    private List<String> scopes;
    private Integer expiresAt;

    private Map<String, String> params;

    public UmaPermission() {
    }

    public UmaPermission(String resourceId, List<String> scopes) {
        this.resourceId = resourceId;
        this.scopes = scopes;
    }

    @JsonProperty(value = "resource_id")
    @XmlElement(name = "resource_id")
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @JsonProperty(value = "resource_scopes")
    @XmlElement(name = "resource_scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Integer getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Integer expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty(value = "params")
    @XmlElement(name = "params")
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
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
