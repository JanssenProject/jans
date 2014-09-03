package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * A scope is a bounded extent of access that is possible to perform on a
 * resource set.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/03/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "name", "icon_uri" })
@XmlRootElement()
public class ScopeDescription {

	private String name;
	private String iconUri;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ScopeDescription");
        sb.append("{iconUri='").append(iconUri).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
