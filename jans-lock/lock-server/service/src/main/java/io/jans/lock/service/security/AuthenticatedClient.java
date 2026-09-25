/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.security;

/**
 * The OAuth client the protection layer authenticated for the current request. Produced by the
 * protection services inside their authorization decision and recorded by the REST filters / gRPC
 * interceptor (see {@link AuthenticatedClientContext}) so downstream services never have to parse
 * or introspect the bearer token a second time.
 *
 * <p>{@link #getClientId()} may be {@code null}: the token was valid for the endpoint but carried
 * neither a {@code client_id} nor an {@code azp} claim (JWT) or the introspection response had no
 * {@code client_id}. Consumers that need a client id must treat that as their own failure; the
 * protection layer itself does not require one. The token is never carried here.
 *
 * @author Yuriy Movchan
 */
public final class AuthenticatedClient {

	private final String clientId;

	private final boolean fromJwt;

	public AuthenticatedClient(String clientId, boolean fromJwt) {
		this.clientId = clientId;
		this.fromJwt = fromJwt;
	}

	/**
	 * @return the OAuth {@code client_id}, or {@code null} when the token did not identify one
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return {@code true} when the id was read from JWT claims ({@code client_id}, then
	 *         {@code azp}); {@code false} when it came from token introspection
	 */
	public boolean isFromJwt() {
		return fromJwt;
	}

	@Override
	public String toString() {
		return "AuthenticatedClient [clientId=" + clientId + ", fromJwt=" + fromJwt + "]";
	}

}
