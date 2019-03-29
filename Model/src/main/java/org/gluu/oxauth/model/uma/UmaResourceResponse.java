/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Resource description.
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/03/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "_id", "user_access_policy_uri" })
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class UmaResourceResponse {

	private String id;
	private String userAccessPolicyUri;

	@JsonProperty(value = "_id")
	@XmlElement(name = "_id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    @JsonProperty(value = "user_access_policy_uri")
	@XmlElement(name = "user_access_policy_uri")
	public String getUserAccessPolicyUri() {
		return userAccessPolicyUri;
	}

	public void setUserAccessPolicyUri(String userAccessPolicyUri) {
		this.userAccessPolicyUri = userAccessPolicyUri;
	}

	@Override
	public String toString() {
		return "UmaResourceResponse [id=" + id + ", user_access_policy_uri=" + userAccessPolicyUri + "]";
	}

}
