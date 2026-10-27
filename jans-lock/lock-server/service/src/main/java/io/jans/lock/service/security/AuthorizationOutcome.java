/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.security;

import jakarta.ws.rs.core.Response;

/**
 * Result of {@link AuthenticatingProtection#authorize}: either a denial carrying the error
 * {@link Response} the filter must abort with, or an allow carrying the
 * {@link AuthenticatedClient}. Exactly one of the two is present.
 *
 * @author Yuriy Movchan
 */
public final class AuthorizationOutcome {

	private final Response errorResponse;

	private final AuthenticatedClient client;

	private AuthorizationOutcome(Response errorResponse, AuthenticatedClient client) {
		this.errorResponse = errorResponse;
		this.client = client;
	}

	/**
	 * @param client the authenticated client, never {@code null} (its client id may be)
	 */
	public static AuthorizationOutcome allowed(AuthenticatedClient client) {
		if (client == null) {
			throw new IllegalArgumentException("An allowed outcome must carry the authenticated client");
		}
		return new AuthorizationOutcome(null, client);
	}

	/**
	 * @param errorResponse the response the caller must abort the request with, never {@code null}
	 */
	public static AuthorizationOutcome denied(Response errorResponse) {
		if (errorResponse == null) {
			throw new IllegalArgumentException("A denied outcome must carry the error response");
		}
		return new AuthorizationOutcome(errorResponse, null);
	}

	public boolean isAllowed() {
		return errorResponse == null;
	}

	/**
	 * @return the error response, or {@code null} when allowed -- the exact contract the legacy
	 *         {@code processAuthorization(...)} methods expose
	 */
	public Response getErrorResponse() {
		return errorResponse;
	}

	/**
	 * @return the authenticated client, or {@code null} when denied
	 */
	public AuthenticatedClient getClient() {
		return client;
	}

}
