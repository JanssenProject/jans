/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.LockProtectionMode;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.openid.OpenIdProtection;
import io.jans.lock.service.security.AuthenticatedClient;
import io.jans.lock.service.security.AuthenticatedClientContext;
import io.jans.lock.service.security.AuthorizationOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Tests for {@link AuthorizationProcessingFilter}: an allowed call records the authenticated
 * client on the servlet request; a denied call aborts and records nothing.
 */
class AuthorizationProcessingFilterTest {

	@Mock
	private Logger log;

	@Mock
	private AppConfiguration appConfiguration;

	@Mock
	private OpenIdProtection protectionService;

	@Mock
	private ApplicationAuditLogger applicationAuditLogger;

	@Mock
	private HttpHeaders httpHeaders;

	@Mock
	private ResourceInfo resourceInfo;

	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private ContainerRequestContext requestContext;

	@Mock
	private UriInfo uriInfo;

	@InjectMocks
	private AuthorizationProcessingFilter filter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		when(requestContext.getUriInfo()).thenReturn(uriInfo);
		when(uriInfo.getPath()).thenReturn("/api/v1/audit/trace");
		when(appConfiguration.getProtectionMode()).thenReturn(LockProtectionMode.OAUTH);
		when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer opaque-token");
	}

	@Test
	void testFilter_Allowed_RecordsAuthenticatedClient() throws Exception {
		AuthenticatedClient client = new AuthenticatedClient("2200.aaaa", false);
		when(protectionService.authorize(eq("opaque-token"), eq(resourceInfo)))
				.thenReturn(AuthorizationOutcome.allowed(client));

		filter.filter(requestContext);

		verify(httpRequest).setAttribute(AuthenticatedClientContext.REQUEST_ATTRIBUTE, client);
		verify(requestContext, never()).abortWith(any());
		verify(applicationAuditLogger).log(any(), eq(true));
	}

	@Test
	void testFilter_Denied_AbortsAndRecordsNothing() throws Exception {
		Response denied = Response.status(Response.Status.FORBIDDEN).entity("Expired token").build();
		when(protectionService.authorize(eq("opaque-token"), eq(resourceInfo)))
				.thenReturn(AuthorizationOutcome.denied(denied));

		filter.filter(requestContext);

		verify(requestContext).abortWith(denied);
		verify(httpRequest, never()).setAttribute(any(), any());
		verify(applicationAuditLogger).log(any(), eq(false));
	}

	@Test
	void testFilter_CedarlingMode_DoesNothing() throws Exception {
		when(appConfiguration.getProtectionMode()).thenReturn(LockProtectionMode.CEDARLING);

		filter.filter(requestContext);

		verify(protectionService, never()).authorize(any(), any());
		verify(httpRequest, never()).setAttribute(any(), any());
		verify(applicationAuditLogger, never()).log(any(), anyBoolean());
	}

}
