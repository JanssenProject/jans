/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Resource that needs protection by registering a resource description at the AS.
 *
 * @author Yuriy Zabrovarnyy
 *         Date: 17/05/2017
 */

@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"name", "uri", "type", "scopes", "scopeExpression", "icon_uri"})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@ApiModel(value = "The resource server defines a resource set that the authorization server needs to be aware of by registering a resource set description at the authorization server. This registration process results in a unique identifier for the resource set that the resource server can later use for managing its description.")
public class UmaResource {

    @ApiModelProperty(value = "An array of strings, any of which MAY be a URI, indicating the available scopes for this resource set. URIs MUST resolve to scope descriptions as defined in Section 2.1. Published scope descriptions MAY reside anywhere on the web; a resource server is not required to self-host scope descriptions and may wish to point to standardized scope descriptions residing elsewhere. It is the resource server's responsibility to ensure that scope description documents are accessible to authorization servers through GET calls to support any user interface requirements. The resource server and authorization server are presumed to have separately negotiated any required interpretation of scope handling not conveyed through scope descriptions."
            , required = false)
    private List<String> scopes;

    @ApiModelProperty(value = "Scope expression.", required = false)
    private String scopeExpression;

    @ApiModelProperty(value = "A human-readable string describing the resource at length. The authorization server MAY use this description in any user interface it presents to a resource owner, for example, for resource protection monitoring or policy setting."
            , required = false)
    private String description;

    @ApiModelProperty(value = "A URI for a graphic icon representing the resource set. The referenced icon MAY be used by the authorization server in its resource owner user interface for the resource owner."
            , required = false)
    private String iconUri;

    @ApiModelProperty(value = " A human-readable string describing a set of one or more resources. This name MAY be used by the authorization server in its resource owner user interface for the resource owner."
              , required = false)
    private String name;

    @ApiModelProperty(value = " A string uniquely identifying the semantics of the resource set. For example, if the resource set consists of a single resource that is an identity claim that leverages standardized claim semantics for \"verified email address\", the value of this property could be an identifying URI for this claim."
                  , required = false)
    private String type;

    @ApiModelProperty(value = "Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this resource was originally issued.", required = true)
    private Integer iat;

    @ApiModelProperty(value = " Integer timestamp, measured in the number of seconds since January 1 1970 UTC, indicating when this resource will expire. ", required = true)
    private Integer exp;

    @JsonProperty(value = "iat")
    @XmlElement(name = "iat")
    public Integer getIat() {
        return iat;
    }

    public void setIat(Integer iat) {
        this.iat = iat;
    }

    @JsonProperty(value = "exp")
    @XmlElement(name = "exp")
    public Integer getExp() {
        return exp;
    }

    public void setExp(Integer exp) {
        this.exp = exp;
    }

    @JsonProperty(value = "description")
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty(value = "type")
    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    public UmaResource setType(String p_type) {
        type = p_type;
        return this;
    }

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public UmaResource setName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty(value = "icon_uri")
    @XmlElement(name = "icon_uri")
    public String getIconUri() {
        return iconUri;
    }

    public UmaResource setIconUri(String iconUri) {
        this.iconUri = iconUri;
        return this;
    }

    @JsonProperty(value = "resource_scopes")
    @XmlElement(name = "resource_scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public UmaResource setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    @JsonProperty(value = "scope_expression")
    @XmlElement(name = "scope_expression")
    public String getScopeExpression() {
        return scopeExpression;
    }

    public void setScopeExpression(String scopeExpression) {
        assertValidExpression(scopeExpression);
        this.scopeExpression = scopeExpression;
    }

    @JsonIgnore
    public static void assertValidExpression(String scopeExpression) {
        if (!isValidExpression(scopeExpression)) {
            throw new RuntimeException("Scope expression is not valid json logic expression. Expression:" + scopeExpression);
        }
    }

    @JsonIgnore
    public static boolean isValidExpression(String scopeExpression) {
        return StringUtils.isBlank(scopeExpression) || JsonLogicNodeParser.isNodeValid(scopeExpression);
    }

    @Override
    public String toString() {
        return "UmaResource{" +
                "name='" + name + '\'' +
                ", scopes=" + scopes +
                ", scopeExpression=" + scopeExpression +
                ", description='" + description + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
