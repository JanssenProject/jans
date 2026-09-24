/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Tests for {@link SubmitterIdentityService}: JWT claim resolution, opaque-token introspection,
 * and that every failure mode maps to {@code client_not_bound} (403), never a 500 (design decision
 * D-2, task 11 acceptance criteria).
 */
class SubmitterIdentityServiceTest {

	@Mock
	private IntrospectionService introspectionService;

	@Mock
	private HttpHeaders httpHeaders;

	private SubmitterIdentityService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		IntrospectionClientProvider provider = new IntrospectionClientProvider();
		provider.setIntrospectionService(introspectionService);

		service = new SubmitterIdentityService();
		service.setLog(LoggerFactory.getLogger(SubmitterIdentityService.class));
		service.setIntrospectionClientProvider(provider);
	}

	/** Builds an unsigned ("alg: NONE" shaped) JWT string carrying only the given claims. */
	private static String jwtWithClaims(String clientId, String azp) throws Exception {
		JwtHeader header = new JwtHeader();
		JwtClaims claims = new JwtClaims();
		if (clientId != null) {
			claims.setClaim("client_id", clientId);
		}
		if (azp != null) {
			claims.setClaim(JwtClaimName.AUTHORIZED_PARTY, azp);
		}
		return header.toBase64JsonObject() + "." + claims.toBase64JsonObject();
	}

	@Test
	void testResolve_JwtWithClientIdClaim_Resolved() throws Exception {
		String token = jwtWithClaims("2200.aaaa", null);
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

		SubmitterIdentity identity = service.resolve(httpHeaders);

		assertEquals("2200.aaaa", identity.getClientId());
		assertTrue(identity.isFromJwt());
	}

	@Test
	void testResolve_JwtWithOnlyAzpClaim_Resolved() throws Exception {
		String token = jwtWithClaims(null, "2200.bbbb");
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("bearer " + token);

		SubmitterIdentity identity = service.resolve(httpHeaders);

		assertEquals("2200.bbbb", identity.getClientId());
		assertTrue(identity.isFromJwt());
	}

	@Test
	void testResolve_JwtWithNeitherClaim_Rejected() throws Exception {
		String token = jwtWithClaims(null, null);
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_CLIENT_CLAIM, ex.getReason());
	}

	@Test
	void testResolve_NoAuthorizationHeader_Rejected() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_BEARER, ex.getReason());
	}

	@Test
	void testResolve_NonBearerScheme_Rejected() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_BEARER, ex.getReason());
	}

	@Test
	void testResolve_OpaqueTokenActiveWithClientId_Resolved() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer opaque-abc123");
		IntrospectionResponse response = new IntrospectionResponse(true);
		response.setClientId("2200.cccc");
		when(introspectionService.introspectToken("Bearer opaque-abc123", "opaque-abc123")).thenReturn(response);

		SubmitterIdentity identity = service.resolve(httpHeaders);

		assertEquals("2200.cccc", identity.getClientId());
		assertFalse(identity.isFromJwt());
	}

	@Test
	void testResolve_OpaqueTokenInactive_Rejected() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer opaque-abc123");
		IntrospectionResponse response = new IntrospectionResponse(false);
		when(introspectionService.introspectToken("Bearer opaque-abc123", "opaque-abc123")).thenReturn(response);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_INTROSPECTION_INACTIVE, ex.getReason());
	}

	@Test
	void testResolve_OpaqueTokenActiveWithoutClientId_Rejected() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer opaque-abc123");
		IntrospectionResponse response = new IntrospectionResponse(true);
		when(introspectionService.introspectToken("Bearer opaque-abc123", "opaque-abc123")).thenReturn(response);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_INTROSPECTION_INACTIVE, ex.getReason());
	}

	@Test
	void testResolve_IntrospectionThrows_RejectedNotServerError() {
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer opaque-abc123");
		when(introspectionService.introspectToken("Bearer opaque-abc123", "opaque-abc123"))
				.thenThrow(new RuntimeException("introspection endpoint unreachable"));

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(httpHeaders));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_INTROSPECTION_ERROR, ex.getReason());
	}

}
