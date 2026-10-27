/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.security;

import jakarta.ws.rs.container.ResourceInfo;

/**
 * A protection service that, besides allowing or denying a call, reports <em>which</em> OAuth
 * client it authenticated. Implemented by both Lock protection services (OAuth scopes and
 * Cedarling), so the REST filters and the gRPC interceptor can record the client once per request
 * for downstream services.
 *
 * <p>Do not inject this interface by type: two beans implement it. Inject the concrete service
 * (or {@code OpenIdProtection}, which has a single implementation).
 *
 * @author Yuriy Movchan
 */
public interface AuthenticatingProtection {

	/**
	 * Same decision as {@code processAuthorization(bearerToken, resourceInfo)} -- which is defined
	 * as {@code authorize(...).getErrorResponse()} -- plus the authenticated client on allow.
	 *
	 * @param bearerToken  the raw bearer token (with or without the {@code Bearer } prefix)
	 * @param resourceInfo the target resource, used to determine the required scopes/permissions
	 * @return never {@code null}
	 */
	AuthorizationOutcome authorize(String bearerToken, ResourceInfo resourceInfo);

}
