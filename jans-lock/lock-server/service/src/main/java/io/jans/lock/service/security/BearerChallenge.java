/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.security;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Builds the bearer-token error responses of RFC 6750 section 3.1: {@code 401} with
 * {@code error="invalid_token"} when a new token could help, {@code 403} with
 * {@code error="insufficient_scope"} when it could not, and a {@code Bearer} challenge without
 * {@code error} when no credentials were sent. The {@code detail} stays the response entity, as before; the
 * {@code error_description} is a fixed string so token data never reaches the header.
 */
public final class BearerChallenge {

	private static final String SCHEME = "Bearer";

	/** Fixed realm, so the challenge without {@code error} still carries an auth-param (RFC 6750 section 3). */
	static final String REALM = "jans-lock";

	private BearerChallenge() {
	}

	/**
	 * @return {@code 401} carrying a {@code Bearer} challenge with only the {@code realm}, as no credentials were sent
	 */
	public static Response missingCredentials(String detail) {
		return Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, SCHEME + " realm=\"" + REALM + "\"").entity(detail).build();
	}

	/**
	 * @param description fixed ASCII text without quotes or backslashes
	 * @return {@code 401} for an expired, revoked, malformed or wrongly signed token
	 */
	public static Response invalidToken(String detail, String description) {
		return Response.status(Response.Status.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, SCHEME + " error=\"invalid_token\", error_description=\"" + description + "\"")
				.entity(detail).build();
	}

	/**
	 * @param description fixed ASCII text without quotes or backslashes
	 * @param scopes the scopes the resource requires, listed in the {@code scope} attribute
	 * @return {@code 403} for a valid token that lacks a required scope
	 */
	public static Response insufficientScope(String detail, String description, List<String> scopes) {
		String challenge = SCHEME + " error=\"insufficient_scope\", error_description=\"" + description + "\"";
		if ((scopes != null) && !scopes.isEmpty()) {
			challenge += ", scope=\"" + String.join(" ", scopes) + "\"";
		}

		return Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.WWW_AUTHENTICATE, challenge).entity(detail).build();
	}

}
