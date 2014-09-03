package org.xdi.oxauth.model.uma;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

/**
 * Requester permission token
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/16/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "rpt" })
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class RequesterPermissionTokenResponse {

	private String token;

	public RequesterPermissionTokenResponse() {
    }

	public RequesterPermissionTokenResponse(String token) {
		this.token = token;
	}

    @JsonProperty(value = "rpt")
	@XmlElement(name = "rpt")
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "RequesterPermissionTokenResponse [token=" + token + "]";
	}

}
