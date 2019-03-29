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
 * UMA error response
 * 
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * Date: 10/24/2012
 */
@IgnoreMediaTypes("application/*+json") // try to ignore jettison as it's recommended here: http://docs.jboss.org/resteasy/docs/2.3.4.Final/userguide/html/json.html
@JsonPropertyOrder({ "status", "error" })
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmaErrorResponse {

    private String status;
	private String error;
	private String errorDescription;
	private String errorUri;

	public UmaErrorResponse() {
    }

    @JsonProperty(value = "status")
	@XmlElement(name = "status")
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    @JsonProperty(value = "error")
	@XmlElement(name = "error")
	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

    @JsonProperty(value = "error_description")
	@XmlElement(name = "error_description")
	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

    @JsonProperty(value = "error_uri")
	@XmlElement(name = "error_uri")
	public String getErrorUri() {
		return errorUri;
	}

	public void setErrorUri(String errorUri) {
		this.errorUri = errorUri;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UmaErrorResponse [status=");
		builder.append(status);
		builder.append(", error=");
		builder.append(error);
		builder.append(", errorDescription=");
		builder.append(errorDescription);
		builder.append(", errorUri=");
		builder.append(errorUri);
		builder.append("]");
		return builder.toString();
	}

}
