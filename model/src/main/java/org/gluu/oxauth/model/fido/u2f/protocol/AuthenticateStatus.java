/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.model.fido.u2f.protocol;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.gluu.oxauth.model.fido.u2f.exception.BadInputException;

/**
 * FIDO U2F device authentication status response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
public class AuthenticateStatus implements Serializable {

	private static final long serialVersionUID = -8287836230637556749L;

	@JsonProperty
	private final String status;

	@JsonProperty
	private final String challenge;

	public AuthenticateStatus(@JsonProperty("status") String status, @JsonProperty("challenge") String challenge) throws BadInputException {
		this.status = status;
		this.challenge = challenge;
	}

	public String getStatus() {
		return status;
	}

	public String getChallenge() {
		return challenge;
	}

	@JsonIgnore
	public String getRequestId() {
		return challenge;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RegisterStatus [status=").append(status).append(", challenge=").append(challenge).append("]");
		return builder.toString();
	}

}
