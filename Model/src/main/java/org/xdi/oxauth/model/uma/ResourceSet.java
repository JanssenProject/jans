/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

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
@JsonPropertyOrder({"name", "icon_uri", "scopes", "type"})
@JsonIgnoreProperties(value = {"type"})
//@JsonRootName(value = "resourceSet")
@XmlRootElement
public class ResourceSet {

    private String name;
    private String iconUri;
    private List<String> scopes;
    private String type;

    @JsonProperty(value = "type")
    @XmlElement(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String p_type) {
        type = p_type;
    }

    @JsonProperty(value = "name")
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty(value = "icon_uri")
    @XmlElement(name = "icon_uri")
    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
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
        sb.append("ResourceSet");
        sb.append("{name='").append(name).append('\'');
        sb.append(", iconUri='").append(iconUri).append('\'');
        sb.append(", scopes=").append(scopes);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
