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
import java.util.List;

/**
 * Resource that needs protection by registering a resource description at the AS.
 *
 * @author Yuriy Zabrovarnyy
 *         Date: 17/05/2017
 */

@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"name", "uri", "type", "scopes", "icon_uri"})
@JsonIgnoreProperties(value = {"type"})
@XmlRootElement
@ApiModel(value = "The resource server defines a resource set that the authorization server needs to be aware of by registering a resource set description at the authorization server. This registration process results in a unique identifier for the resource set that the resource server can later use for managing its description.")
public class UmaResource {

    @ApiModelProperty(value = " A human-readable string describing a set of one or more resources. This name MAY be used by the authorization server in its resource owner user interface for the resource owner."
              , required = true)
    private String name;

    @ApiModelProperty(value = "A URI that provides the network location for the resource set being registered. For example, if the resource set corresponds to a digital photo, the value of this property could be an HTTP-based URI identifying the location of the photo on the web. The authorization server can use this information in various ways to inform clients about a resource set's location."
              , required = false)
    private String uri;

    @ApiModelProperty(value = "A URI for a graphic icon representing the resource set. The referenced icon MAY be used by the authorization server in its resource owner user interface for the resource owner."
              , required = false)
    private String iconUri;

    @ApiModelProperty(value = "An array of strings, any of which MAY be a URI, indicating the available scopes for this resource set. URIs MUST resolve to scope descriptions as defined in Section 2.1. Published scope descriptions MAY reside anywhere on the web; a resource server is not required to self-host scope descriptions and may wish to point to standardized scope descriptions residing elsewhere. It is the resource server's responsibility to ensure that scope description documents are accessible to authorization servers through GET calls to support any user interface requirements. The resource server and authorization server are presumed to have separately negotiated any required interpretation of scope handling not conveyed through scope descriptions."
              , required = false)
    private List<String> scopes;

    @ApiModelProperty(value = " A string uniquely identifying the semantics of the resource set. For example, if the resource set consists of a single resource that is an identity claim that leverages standardized claim semantics for \"verified email address\", the value of this property could be an identifying URI for this claim."
                  , required = false)
    private String type;

    @JsonProperty(value = "uri")
    @XmlElement(name = "uri")
    public String getUri() {
        return uri;
    }

    public UmaResource setUri(String uri) {
        this.uri = uri;
        return this;
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

    @JsonProperty(value = "scopes")
    @XmlElement(name = "scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public UmaResource setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    @Override
    public String toString() {
        return "UmaResource{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", iconUri='" + iconUri + '\'' +
                ", scopes=" + scopes +
                ", type='" + type + '\'' +
                '}';
    }
}
