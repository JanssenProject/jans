package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * Resource set that needs protection by registering a resource set description
 * at the AM.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/04/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "_id", "_rev", "name", "iconUri", "scopes" })
@XmlRootElement
public class VersionedResourceSet extends ResourceSet {

	private String id;
	private String rev;

    @JsonProperty(value = "_id")
	@XmlElement(name = "_id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    @JsonProperty(value = "_rev")
	@XmlElement(name = "_rev")
	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	@Override
	public String toString() {
		return "VersionedResourceSet [id=" + id + ", rev=" + rev
				+ ", toString()=" + super.toString() + "]";
	}

}
