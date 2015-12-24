/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * UMA Permission. Used for both:
 * 1. register permission ticket
 * 2. by introspection RPT endpoint to return RPT status.
 *
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 *         Date: 24/12/2015
 */

// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({"resource_set_id", "scopes", "exp", "iat", "nbf"})
@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonRootName(value = "resourceSetPermissionRequest")
@XmlRootElement
@ApiModel(value = "Register permission request.")
public class UmaPermission implements Serializable {

    @ApiModelProperty(value = "The identifier for a resource set to which this client is seeking access. The identifier MUST correspond to a resource set that was previously registered."
            , required = true)
    private String resourceSetId;
    @ApiModelProperty(value = "An array referencing one or more identifiers of scopes to which access is needed for this resource set. Each scope identifier MUST correspond to a scope that was registered by this resource server for the referenced resource set."
            , required = true)
    private List<String> scopes;
    private Date expiresAt;
    private Date issuedAt;
    private Date nbf;

    public UmaPermission() {
    }

    public UmaPermission(String p_resourceSetId, List<String> p_scopes) {
        resourceSetId = p_resourceSetId;
        scopes = p_scopes;
    }

    @JsonProperty(value = "resource_set_id")
    @XmlElement(name = "resource_set_id")
    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    @JsonProperty(value = "nbf")
    @XmlElement(name = "nbf")
    public Date getNbf() {
        return nbf;
    }

    public void setNbf(Date nbf) {
        this.nbf = nbf;
    }

    @JsonProperty(value = "iat")
    @XmlElement(name = "iat")
    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    @JsonProperty(value = "scopes")
    @XmlElement(name = "scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Date getExpiresAt() {
        return expiresAt != null ? new Date(expiresAt.getTime()) : null;
    }

    public void setExpiresAt(Date p_expiresAt) {
        expiresAt = p_expiresAt != null ? new Date(p_expiresAt.getTime()) : null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ResourceSetPermissionRequest");
        sb.append("{resourceSetId='").append(resourceSetId).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", issuedAt=").append(issuedAt);
        sb.append(", nbf=").append(nbf);
        sb.append('}');
        return sb.toString();
    }
}
