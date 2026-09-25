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

import io.grpc.Context;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.security.AuthenticatedClient;
import io.jans.lock.service.security.AuthenticatedClientContext;
import io.jans.lock.service.trace.error.TraceValidationException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Tests for {@link SubmitterIdentityService}: it reads the {@link AuthenticatedClient} the
 * protection filter recorded and maps every gap to {@code client_not_bound} (403), never a 500
 * (design decision D-2).
 */
class SubmitterIdentityServiceTest {

	@Mock
	private HttpServletRequest request;

	private SubmitterIdentityService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		service = new SubmitterIdentityService();
		service.setLog(LoggerFactory.getLogger(SubmitterIdentityService.class));
	}

	private void recorded(AuthenticatedClient client) {
		when(request.getAttribute(AuthenticatedClientContext.REQUEST_ATTRIBUTE)).thenReturn(client);
	}

	@Test
	void testResolve_JwtClientRecorded_Resolved() {
		recorded(new AuthenticatedClient("2200.aaaa", true));

		SubmitterIdentity identity = service.resolve(request);

		assertEquals("2200.aaaa", identity.getClientId());
		assertTrue(identity.isFromJwt());
	}

	@Test
	void testResolve_IntrospectedClientRecorded_Resolved() {
		recorded(new AuthenticatedClient("2200.cccc", false));

		SubmitterIdentity identity = service.resolve(request);

		assertEquals("2200.cccc", identity.getClientId());
		assertFalse(identity.isFromJwt());
	}

	@Test
	void testResolve_NothingRecorded_Rejected() {
		recorded(null);

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(request));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_AUTHENTICATED_CLIENT, ex.getReason());
	}

	@Test
	void testResolve_NullRequestAndNoGrpcContext_Rejected() {
		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(null));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_AUTHENTICATED_CLIENT, ex.getReason());
	}

	@Test
	void testResolve_ForeignAttributeType_Rejected() {
		when(request.getAttribute(AuthenticatedClientContext.REQUEST_ATTRIBUTE)).thenReturn("not-a-client");

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(request));

		assertEquals(SubmitterIdentityService.REASON_NO_AUTHENTICATED_CLIENT, ex.getReason());
	}

	@Test
	void testResolve_JwtWithoutClientClaims_Rejected() {
		recorded(new AuthenticatedClient(null, true));

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(request));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_CLIENT_CLAIM, ex.getReason());
	}

	@Test
	void testResolve_IntrospectionWithoutClientId_Rejected() {
		recorded(new AuthenticatedClient(" ", false));

		TraceValidationException ex = assertThrows(TraceValidationException.class, () -> service.resolve(request));

		assertEquals(TraceErrorResponseType.CLIENT_NOT_BOUND, ex.getErrorId());
		assertEquals(SubmitterIdentityService.REASON_NO_CLIENT_ID, ex.getReason());
	}

	@Test
	void testResolve_GrpcContextFallback_Resolved() throws Exception {
		recorded(null);
		Context context = AuthenticatedClientContext.withClient(Context.current(),
				new AuthenticatedClient("2200.grpc", true));

		SubmitterIdentity identity = context.call(() -> service.resolve(request));

		assertEquals("2200.grpc", identity.getClientId());
		assertTrue(identity.isFromJwt());
	}

}
