/*
 /*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.protocol;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxauth.model.fido.u2f.U2fConstants;

/**
 * FIDO U2F device authentication request
 *
 * @author Yuriy Movchan Date: 05/13/2015
 */
public class AuthenticateRequest implements Serializable {

	private static final long serialVersionUID = 8479635006453668632L;

	/**
	 * Version of the protocol that the to-be-registered U2F token must speak.
	 * For the version of the protocol described herein, must be "U2F_V2"
	 */
	@JsonProperty
	private final String version = U2fConstants.U2F_PROTOCOL_VERSION;

	/**
	 * The websafe-base64-encoded challenge.
	 */
	@JsonProperty
	private final String challenge;

	/**
	 * The application id that the RP would like to assert. The U2F token will
	 * enforce that the key handle provided above is associated with this
	 * application id. The browser enforces that the calling origin belongs to
	 * the application identified by the application id.
	 */
	@JsonProperty
	private final String appId;

	/**
	 * websafe-base64 encoding of the key handle obtained from the U2F token
	 * during registration.
	 */
	@JsonProperty
	private final String keyHandle;

	public AuthenticateRequest(@JsonProperty("challenge") String challenge, @JsonProperty("appId") String appId, @JsonProperty("keyHandle") String keyHandle) {
		this.challenge = challenge;
		this.appId = appId;
		this.keyHandle = keyHandle;
	}

	public String getKeyHandle() {
		return keyHandle;
	}

	public String getChallenge() {
		return challenge;
	}

	public String getAppId() {
		return appId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AuthenticateRequest [version=").append(version).append(", challenge=").append(challenge).append(", appId=").append(appId)
				.append(", keyHandle=").append(keyHandle).append("]");
		return builder.toString();
	}

}
