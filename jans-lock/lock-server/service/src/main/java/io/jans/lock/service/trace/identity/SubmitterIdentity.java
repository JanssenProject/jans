/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

/**
 * The submitting OAuth client identity resolved by {@link SubmitterIdentityService} from the
 * request's bearer token (design decision D-2). This is a plain value object: it never carries
 * the token itself.
 *
 * @author Yuriy Movchan
 */
public class SubmitterIdentity {

	private final String clientId;

	private final boolean fromJwt;

	public SubmitterIdentity(String clientId, boolean fromJwt) {
		this.clientId = clientId;
		this.fromJwt = fromJwt;
	}

	/**
	 * @return the resolved OAuth {@code client_id}, never blank
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return {@code true} when the identity was read from a JWT claim ({@code client_id} or
	 *         {@code azp}); {@code false} when it came from token introspection
	 */
	public boolean isFromJwt() {
		return fromJwt;
	}

	@Override
	public String toString() {
		return "SubmitterIdentity [clientId=" + clientId + ", fromJwt=" + fromJwt + "]";
	}

}
