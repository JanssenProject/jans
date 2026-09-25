/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.security;

import io.grpc.Context;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Per-request carrier for the {@link AuthenticatedClient} recorded by the protection layer.
 *
 * <ul>
 * <li><strong>REST</strong>: a servlet request attribute, written by the JAX-RS filters and read
 * by resources via their {@code @Context HttpServletRequest}. The servlet attribute is used
 * directly (not {@code ContainerRequestContext.setProperty}) so nothing depends on how the JAX-RS
 * runtime maps properties to attributes.</li>
 * <li><strong>gRPC</strong>: an {@link io.grpc.Context} key, written by the authorization
 * interceptor next to the client-IP key in {@code ServerUtil} and read with {@link #current()}
 * from the handling thread.</li>
 * </ul>
 *
 * A CDI {@code @RequestScoped} bean was deliberately not used: the protection services are shared
 * with the gRPC interceptor, where no CDI request context is active.
 *
 * @author Yuriy Movchan
 */
public final class AuthenticatedClientContext {

	/** Servlet request attribute name. */
	public static final String REQUEST_ATTRIBUTE = AuthenticatedClient.class.getName();

	/** gRPC context key. */
	public static final Context.Key<AuthenticatedClient> GRPC_CONTEXT_KEY = Context.key("authenticated-client");

	private AuthenticatedClientContext() {
	}

	/**
	 * Records the client on the current REST request. A {@code null} request (non-servlet
	 * deployment) is ignored.
	 */
	public static void store(HttpServletRequest request, AuthenticatedClient client) {
		if (request != null && client != null) {
			request.setAttribute(REQUEST_ATTRIBUTE, client);
		}
	}

	/**
	 * @return the client recorded on this REST request, or {@code null} when none was recorded
	 *         (unprotected resource, or the filter did not run)
	 */
	public static AuthenticatedClient get(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		Object value = request.getAttribute(REQUEST_ATTRIBUTE);
		return value instanceof AuthenticatedClient ? (AuthenticatedClient) value : null;
	}

	/**
	 * @return {@code context} extended with the client, for {@code Contexts.interceptCall}
	 */
	public static Context withClient(Context context, AuthenticatedClient client) {
		return context.withValue(GRPC_CONTEXT_KEY, client);
	}

	/**
	 * @return the client recorded on the current gRPC {@link Context}, or {@code null}
	 */
	public static AuthenticatedClient current() {
		return GRPC_CONTEXT_KEY.get(Context.current());
	}

}
