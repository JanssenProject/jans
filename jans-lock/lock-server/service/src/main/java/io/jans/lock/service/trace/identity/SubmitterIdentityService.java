/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Resolves the submitting OAuth client behind a TRACE request from the bearer token already
 * validated by the existing protection filters (design decision D-2). This is defense in depth:
 * the filter should already have rejected an unauthenticated request, so every failure here is a
 * belt-and-suspenders 403, never a 500.
 *
 * <p>Never logs the token itself; only the resolved {@code client_id} is logged, at DEBUG.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class SubmitterIdentityService {

	/** Case-insensitive per task 11: {@code Bearer}/{@code bearer}/{@code BEARER} all match. */
	private static final Pattern BEARER_PATTERN = Pattern.compile("^Bearer\\s+(.+)$", Pattern.CASE_INSENSITIVE);

	/** Not a standard {@link JwtClaimName} constant; OAuth's {@code client_id} claim. */
	private static final String CLIENT_ID_CLAIM = "client_id";

	static final String REASON_NO_BEARER = "no_bearer";

	static final String REASON_NO_CLIENT_CLAIM = "no_client_claim";

	static final String REASON_INTROSPECTION_INACTIVE = "introspection_inactive";

	/** Introspection call itself failed (network, endpoint, etc.) rather than returning inactive. */
	static final String REASON_INTROSPECTION_ERROR = "introspection_error";

	@Inject
	private Logger log;

	@Inject
	private IntrospectionClientProvider introspectionClientProvider;

	/**
	 * @param headers the current request's headers ({@link jakarta.ws.rs.core.Context @Context}
	 *                {@code HttpHeaders} on a {@code BaseResource})
	 * @return the resolved submitter identity
	 * @throws TraceValidationException {@code client_not_bound} (403) for every failure mode
	 */
	public SubmitterIdentity resolve(HttpHeaders headers) {
		String authorization = headers == null ? null : headers.getHeaderString(HttpHeaders.AUTHORIZATION);
		String token = extractBearerToken(authorization);
		if (token == null) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_NO_BEARER);
		}

		Jwt jwt = tryParseJwt(token);
		SubmitterIdentity identity = jwt != null ? fromJwt(jwt) : fromIntrospection(token);

		log.debug("Resolved TRACE submitter client_id={}", identity.getClientId());
		return identity;
	}

	private String extractBearerToken(String authorization) {
		if (StringUtils.isBlank(authorization)) {
			return null;
		}

		Matcher matcher = BEARER_PATTERN.matcher(authorization.trim());
		if (!matcher.matches()) {
			return null;
		}

		String token = matcher.group(1).trim();
		return StringUtils.isBlank(token) ? null : token;
	}

	private Jwt tryParseJwt(String token) {
		try {
			return Jwt.parse(token);
		} catch (Exception e) {
			// Not a JWT (or malformed) -- treat as an opaque token. Log a fixed message, never the
			// token or the parse exception's message (which could conceivably echo input).
			log.trace("TRACE bearer token is not a JWT; treating it as opaque");
			return null;
		}
	}

	private SubmitterIdentity fromJwt(Jwt jwt) {
		JwtClaims claims = jwt.getClaims();
		String clientId = claims == null ? null : claims.getClaimAsString(CLIENT_ID_CLAIM);
		if (StringUtils.isBlank(clientId)) {
			clientId = claims == null ? null : claims.getClaimAsString(JwtClaimName.AUTHORIZED_PARTY);
		}

		if (StringUtils.isBlank(clientId)) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_NO_CLIENT_CLAIM);
		}

		return new SubmitterIdentity(clientId, true);
	}

	private SubmitterIdentity fromIntrospection(String token) {
		IntrospectionResponse response;
		try {
			response = introspectionClientProvider.introspectToken("Bearer " + token, token);
		} catch (Exception e) {
			// Introspection endpoint failure must not become a 500 (defense in depth).
			log.debug("TRACE opaque-token introspection failed: {}", e.getMessage());
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_INTROSPECTION_ERROR, e);
		}

		if (response == null || !response.isActive() || StringUtils.isBlank(response.getClientId())) {
			throw new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, REASON_INTROSPECTION_INACTIVE);
		}

		return new SubmitterIdentity(response.getClientId(), false);
	}

	/**
	 * Test seam: bypasses CDI injection of {@link IntrospectionClientProvider}.
	 */
	void setIntrospectionClientProvider(IntrospectionClientProvider introspectionClientProvider) {
		this.introspectionClientProvider = introspectionClientProvider;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

}
