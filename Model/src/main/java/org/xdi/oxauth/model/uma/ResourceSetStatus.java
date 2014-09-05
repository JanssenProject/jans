/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * Resource set status description.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/03/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "status", "_id", "_rev", "policy_uri" })
//@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonRootName(value = "resourceSetStatus")
@XmlRootElement
public class ResourceSetStatus {

	private String status;
	private String id;
	private String rev;
	private String policyUri;

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

    @JsonProperty(value = "status")
	@XmlElement
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    @JsonProperty(value = "policy_uri")
	@XmlElement(name = "policy_uri")
	public String getPolicyUri() {
		return policyUri;
	}

	public void setPolicyUri(String policyUri) {
		this.policyUri = policyUri;
	}

	@Override
	public String toString() {
		return "ResourceSetStatus [status=" + status + ", id=" + id
				+ ", rev=" + rev + ", policyUri=" + policyUri + "]";
	}

}
