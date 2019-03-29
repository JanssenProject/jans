/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.model.fido.u2f.protocol;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.gluu.oxauth.model.util.Util;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * FIDO U2F authentication request message
 *
 * @author Yuriy Movchan Date: 05/15/2015
 */
public class AuthenticateRequestMessage implements Serializable {

	private static final long serialVersionUID = 5492097239884163697L;

	@JsonProperty
	private List<AuthenticateRequest> authenticateRequests;

	public AuthenticateRequestMessage() {}

	public AuthenticateRequestMessage(@JsonProperty("authenticateRequests") List<AuthenticateRequest> authenticateRequests) {
		this.authenticateRequests = authenticateRequests;
	}

	public List<AuthenticateRequest> getAuthenticateRequests() {
		return Collections.unmodifiableList(authenticateRequests);
	}

	public void setAuthenticateRequests(List<AuthenticateRequest> authenticateRequests) {
		this.authenticateRequests = authenticateRequests;
	}

	@JsonIgnore
	public String getRequestId() {
		return Util.firstItem(authenticateRequests).getChallenge();
	}

	@JsonIgnore
	public String getAppId() {
		return Util.firstItem(authenticateRequests).getAppId();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AuthenticateRequestMessage [authenticateRequests=").append(authenticateRequests).append("]");
		return builder.toString();
	}

}
