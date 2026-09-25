/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.security.AuthenticatedClient;
import io.jans.lock.service.security.AuthenticatedClientContext;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the submitting OAuth client behind a TRACE request (design decision D-2) by reading
 * the {@link AuthenticatedClient} the protection layer recorded when it authorized the call
 * (see {@link AuthenticatedClientContext}). The token is never parsed or introspected here: that
 * happened exactly once, in the filter, whose signature/expiry/scope verification this service
 * therefore inherits.
 *
 * <p>Fails closed: no recorded client means the resource was reached without the protection
 * filter (misregistration or bypass) and is rejected with 403, never a 500.
 *
 * <p>Never logs the token (it is not even reachable from here); only the resolved
 * {@code client_id} is logged, at DEBUG.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class SubmitterIdentityService {

	/** No client was recorded for this request: the protection filter did not run. */
	static final String REASON_NO_AUTHENTICATED_CLIENT = "no_authenticated_client";

	/** The JWT was accepted by the filter but carried neither {@code client_id} nor {@code azp}. */
	static final String REASON_NO_CLIENT_CLAIM = "no_client_claim";

	/** The opaque token was accepted by the filter but its introspection had no {@code client_id}. */
	static final String REASON_NO_CLIENT_ID = "no_client_id";

	@Inject
	private Logger log;

	/**
	 * @param request the current REST request ({@code @Context HttpServletRequest} on a
	 *                {@code BaseResource}); {@code null} when the caller is not a servlet request,
	 *                in which case only the gRPC {@link io.grpc.Context} is consulted
	 * @return the resolved submitter identity
	 * @throws TraceValidationException {@code client_not_bound} (403) for every failure mode
	 */
	public SubmitterIdentity resolve(HttpServletRequest request) {
		AuthenticatedClient client = AuthenticatedClientContext.get(request);
		if (client == null) {
			client = AuthenticatedClientContext.current();
		}
		if (client == null) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_NO_AUTHENTICATED_CLIENT);
		}

		if (StringUtils.isBlank(client.getClientId())) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND,
					client.isFromJwt() ? REASON_NO_CLIENT_CLAIM : REASON_NO_CLIENT_ID);
		}

		SubmitterIdentity identity = new SubmitterIdentity(client.getClientId(), client.isFromJwt());
		log.debug("Resolved TRACE submitter client_id={}", identity.getClientId());
		return identity;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

}
