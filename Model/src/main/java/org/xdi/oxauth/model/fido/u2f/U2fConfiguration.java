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
@JsonPropertyOrder({ "version", "issuer", "registration_start_endpoint", "registration_end_endpoint", "authentication_start_endpoint",
		"authentication_end_endpoint" })
@ApiModel(value = "FIDO U2F Configuration")
public class U2fConfiguration {

	@ApiModelProperty(value = "The version of the FIDO U2F core protocol to which this server conforms. The value MUST be the string \"1.0\".", required = true)
	@JsonProperty(value = "version")
	private String version;

	@ApiModelProperty(value = "A URI indicating the party operating the FIDO U2F server.", required = true)
	@JsonProperty(value = "issuer")
	private String issuer;

	@JsonProperty(value = "registration_start_endpoint")
	private String registrationStartEndpoint;

	@JsonProperty(value = "registration_finish_endpoint")
	private String registrationFinishEndpoint;

	@JsonProperty(value = "registration_status_endpoint")
	private String registrationStatusEndpoint;

	@JsonProperty(value = "authentication_start_endpoint")
	private String authenticationStartEndpoint;

	@JsonProperty(value = "authentication_finish_endpoint")
	private String authenticationFinishEndpoint;

	@JsonProperty(value = "authentication_status_endpoint")
	private String authenticationStatusEndpoint;

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

	public String getRegistrationStartEndpoint() {
		return registrationStartEndpoint;
	}

	public void setRegistrationStartEndpoint(String registrationStartEndpoint) {
		this.registrationStartEndpoint = registrationStartEndpoint;
	}

	public String getRegistrationFinishEndpoint() {
		return registrationFinishEndpoint;
	}

	public void setRegistrationFinishEndpoint(String registrationFinishEndpoint) {
		this.registrationFinishEndpoint = registrationFinishEndpoint;
	}

	public String getRegistrationStatusEndpoint() {
		return registrationStatusEndpoint;
	}

	public void setRegistrationStatusEndpoint(String registrationStatusEndpoint) {
		this.registrationStatusEndpoint = registrationStatusEndpoint;
	}

	public String getAuthenticationStartEndpoint() {
		return authenticationStartEndpoint;
	}

	public void setAuthenticationStartEndpoint(String authenticationStartEndpoint) {
		this.authenticationStartEndpoint = authenticationStartEndpoint;
	}

	public String getAuthenticationFinishEndpoint() {
		return authenticationFinishEndpoint;
	}

	public void setAuthenticationFinishEndpoint(String authenticationFinishEndpoint) {
		this.authenticationFinishEndpoint = authenticationFinishEndpoint;
	}

	public String getAuthenticationStatusEndpoint() {
		return authenticationStatusEndpoint;
	}

	public void setAuthenticationStatusEndpoint(String authenticationStatusEndpoint) {
		this.authenticationStatusEndpoint = authenticationStatusEndpoint;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("U2fConfiguration [version=").append(version).append(", issuer=").append(issuer)
				.append(", registrationStartEndpoint=").append(registrationStartEndpoint).append(", registrationFinishEndpoint=")
				.append(registrationFinishEndpoint).append(", authenticationStartEndpoint=").append(authenticationStartEndpoint)
				.append(", authenticationFinishEndpoint=").append(authenticationFinishEndpoint).append("]");
		return builder.toString();
	}

}
