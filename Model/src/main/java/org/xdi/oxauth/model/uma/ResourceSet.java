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
import java.util.List;

/**
 * Resource set that needs protection by registering a resource set description
 * at the AM.
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/03/2012
 */

@IgnoreMediaTypes("application/*+json")
// try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({"name", "uri", "type", "scopes", "icon_uri"})
@JsonIgnoreProperties(value = {"type"})
//@JsonRootName(value = "resourceSet")
@XmlRootElement
public class ResourceSet {

    private String name;
    private String uri;
    private String iconUri;
    private List<String> scopes;
    private String type;

    @JsonProperty(value = "uri")
    @XmlElement(name = "uri")
    public String getUri() {
        return uri;
    }

    public ResourceSet setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @JsonProperty(value = "type")
    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    public ResourceSet setType(String p_type) {
        type = p_type;
        return this;
    }

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public ResourceSet setName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty(value = "icon_uri")
    @XmlElement(name = "icon_uri")
    public String getIconUri() {
        return iconUri;
    }

    public ResourceSet setIconUri(String iconUri) {
        this.iconUri = iconUri;
        return this;
    }

    @JsonProperty(value = "scopes")
    @XmlElement(name = "scopes")
    public List<String> getScopes() {
        return scopes;
    }

    public ResourceSet setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ResourceSet");
        sb.append("{name='").append(name).append('\'');
        sb.append(", uri='").append(uri).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append(", iconUri='").append(iconUri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
