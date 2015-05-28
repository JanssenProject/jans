/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * FIDO U2F metadata configuration
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
@IgnoreMediaTypes("application/*+json")
@JsonPropertyOrder({ "version", "issuer", "registration_start", "authentication_start" })
@ApiModel(value = "FIDO U2F Configuration")
public class U2fConfiguration {

	@ApiModelProperty(value = "The version of the FIDO U2F core protocol to which this server conforms. The value MUST be the string \"1.0\".", required = true)
	@JsonProperty(value = "version")
	private String version;

	@ApiModelProperty(value = "A URI indicating the party operating the FIDO U2F server.", required = true)
	@JsonProperty(value = "issuer")
	private String issuer;

	@JsonProperty(value = "registration_endpoint")
	private String registrationEndpoint;

	@JsonProperty(value = "authentication_endpoint")
	private String authenticationEndpoint;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getRegistrationEndpoint() {
		return registrationEndpoint;
	}

	public void setRegistrationEndpoint(String registrationEndpoint) {
		this.registrationEndpoint = registrationEndpoint;
	}

	public String getAuthenticationEndpoint() {
		return authenticationEndpoint;
	}

	public void setAuthenticationEndpoint(String authenticationEndpoint) {
		this.authenticationEndpoint = authenticationEndpoint;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("U2fConfiguration [version=").append(version).append(", issuer=").append(issuer).append(", registrationEndpoint=")
				.append(registrationEndpoint).append(", authenticationEndpoint=").append(authenticationEndpoint).append("]");
		return builder.toString();
	}


}
