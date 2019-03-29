/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.model.fido.u2f.protocol;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxauth.model.util.Util;

/**
 * FIDO U2F registration request message
 *
 * @author Yuriy Movchan Date: 05/15/2015
 */
public class RegisterRequestMessage implements Serializable {

	private static final long serialVersionUID = -5554834606247337007L;

	@JsonProperty
	private final List<AuthenticateRequest> authenticateRequests;

	@JsonProperty
	private final List<RegisterRequest> registerRequests;

	public RegisterRequestMessage(@JsonProperty("authenticateRequests") List<AuthenticateRequest> authenticateRequests,
			@JsonProperty("registerRequests") List<RegisterRequest> registerRequests) {
		this.authenticateRequests = authenticateRequests;
		this.registerRequests = registerRequests;
	}

	public List<AuthenticateRequest> getAuthenticateRequests() {
		return Collections.unmodifiableList(authenticateRequests);
	}

	public List<RegisterRequest> getRegisterRequests() {
		return Collections.unmodifiableList(registerRequests);
	}

	@JsonIgnore
	public RegisterRequest getRegisterRequest() {
		return Util.firstItem(registerRequests);
	}

	@JsonIgnore
	public String getRequestId() {
		return Util.firstItem(registerRequests).getChallenge();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RegisterRequestMessage [authenticateRequests=").append(authenticateRequests).append(", registerRequests=").append(registerRequests)
				.append("]");
		return builder.toString();
	}

}
