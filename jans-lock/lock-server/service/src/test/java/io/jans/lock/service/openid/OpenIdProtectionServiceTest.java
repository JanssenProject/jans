/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.openid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.as.client.service.IntrospectionService;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.as.model.crypto.AuthCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.lock.service.OpenIdService;
import io.jans.lock.service.security.AuthorizationOutcome;
import io.jans.service.security.api.ProtectedApi;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link OpenIdProtectionService#authorize}: the allow/deny decision is unchanged from
 * {@code processAuthorization}, and an allow now reports the authenticated client (from JWT
 * claims, or from the introspection response for opaque tokens).
 */
class OpenIdProtectionServiceTest {

	private static final String ISSUER = "https://idp.example.org";

	private static final String SCOPE = "https://jans.io/oauth/lock/trace.write";

	@Mock
	private Logger log;

	@Mock
	private OpenIdService openIdService;

	@Mock
	private IntrospectionService introspectionService;

	@Mock
	private OpenIdConfigurationResponse oidcConfig;

	@Mock
	private ObjectMapper mapper;

	@InjectMocks
	private OpenIdProtectionService service;

	/** Fixture resource protected by a single scope. */
	@ProtectedApi(scopes = { SCOPE })
	interface SecuredApi {
		void write();
	}

	static class SecuredResource implements SecuredApi {
		@Override
		public void write() {
		}
	}

	private ResourceInfo resourceInfo;

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		lenient().when(oidcConfig.getIssuer()).thenReturn(ISSUER);
		lenient().when(oidcConfig.getJwksUri()).thenReturn(ISSUER + "/jwks");

		resourceInfo = mock(ResourceInfo.class);
		when(resourceInfo.getResourceClass()).thenReturn((Class) SecuredResource.class);
		when(resourceInfo.getResourceMethod()).thenReturn(SecuredResource.class.getMethod("write"));
	}

	// -- opaque tokens --------------------------------------------------------------------------

	@Test
	void testAuthorize_OpaqueActiveWithClientId_AllowedWithIntrospectedClient() {
		IntrospectionResponse active = new IntrospectionResponse(true);
		active.setClientId("2200.cccc");
		active.setScope(Collections.singletonList(SCOPE));
		when(introspectionService.introspectToken("Bearer opaque-1", "opaque-1")).thenReturn(active);

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-1", resourceInfo);

		assertTrue(outcome.isAllowed());
		assertEquals("2200.cccc", outcome.getClient().getClientId());
		assertFalse(outcome.getClient().isFromJwt());
		assertNull(service.processAuthorization("Bearer opaque-1", resourceInfo));
	}

	@Test
	void testAuthorize_OpaqueInactive_Denied401() {
		IntrospectionResponse inactive = new IntrospectionResponse(false);
		inactive.setScope(Collections.singletonList(SCOPE));
		when(introspectionService.introspectToken("Bearer opaque-2", "opaque-2")).thenReturn(inactive);

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-2", resourceInfo);

		assertFalse(outcome.isAllowed());
		assertNull(outcome.getClient());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	@Test
	void testAuthorize_OpaqueInactiveWithoutScope_Denied401() {
		when(introspectionService.introspectToken("Bearer opaque-4", "opaque-4")).thenReturn(new IntrospectionResponse(false));

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-4", resourceInfo);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	@Test
	void testAuthorize_OpaqueActiveInsufficientScope_Denied403() {
		IntrospectionResponse active = new IntrospectionResponse(true);
		active.setScope(Collections.singletonList("other.scope"));
		when(introspectionService.introspectToken("Bearer opaque-5", "opaque-5")).thenReturn(active);

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-5", resourceInfo);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInsufficientScopeChallenge(outcome);
	}

	@Test
	void testAuthorize_IntrospectionThrows_Denied403NotServerError() {
		when(introspectionService.introspectToken(anyString(), anyString())).thenThrow(new RuntimeException("down"));

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-3", resourceInfo);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), outcome.getErrorResponse().getStatus());
	}

	@Test
	void testAuthorize_IntrospectionAnswers401_Denied401InvalidToken() {
		when(introspectionService.introspectToken("Bearer opaque-6", "opaque-6"))
				.thenThrow(new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build()));

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-6", resourceInfo);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	@Test
	void testAuthorize_IntrospectionAnswers401ForMissingIntrospectionScope_Denied403() {
		// Verbatim from jans-auth-server IntrospectionService#validateIntrospectionScopePresence, kept literal so a change to the classifier marker fails this test
		String reason = "{\"error\":\"access_denied\",\"error_description\":\"access_token used to access introspection endpoint does not have 'introspection' scope, "
				+ "however in AS configuration 'introspectionAccessTokenMustHaveIntrospectionScope' is true\"}";
		when(introspectionService.introspectToken("Bearer opaque-8", "opaque-8"))
				.thenThrow(new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(reason).build()));

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-8", resourceInfo);

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertNull(outcome.getErrorResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	void testAuthorize_IntrospectionAnswers500_Denied403() {
		when(introspectionService.introspectToken("Bearer opaque-7", "opaque-7"))
				.thenThrow(new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()));

		AuthorizationOutcome outcome = service.authorize("Bearer opaque-7", resourceInfo);

		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertNull(outcome.getErrorResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
	}

	@Test
	void testAuthorize_NoToken_Denied401() {
		AuthorizationOutcome outcome = service.authorize(null, resourceInfo);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertEquals("Bearer realm=\"jans-lock\"", outcome.getErrorResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
	}

	// -- JWT tokens -----------------------------------------------------------------------------

	@Test
	void testAuthorize_JwtWithClientId_AllowedWithJwtClient() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList(SCOPE));

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertTrue(outcome.isAllowed());
		assertEquals("2200.aaaa", outcome.getClient().getClientId());
		assertTrue(outcome.getClient().isFromJwt());
	}

	@Test
	void testAuthorize_JwtWithOnlyAzp_AllowedWithAzpClient() throws Exception {
		Jwt jwt = mockJwt(null, "2200.bbbb", Collections.singletonList(SCOPE));

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertTrue(outcome.isAllowed());
		assertEquals("2200.bbbb", outcome.getClient().getClientId());
	}

	@Test
	void testAuthorize_JwtWithNeitherClaim_AllowedWithNullClientId() throws Exception {
		Jwt jwt = mockJwt(null, null, Collections.singletonList(SCOPE));

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertTrue(outcome.isAllowed(), "missing client claims are not a protection-layer denial");
		assertNotNull(outcome.getClient());
		assertNull(outcome.getClient().getClientId());
	}

	@Test
	void testAuthorize_JwtInsufficientScope_Denied403() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList("other.scope"));

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.FORBIDDEN.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInsufficientScopeChallenge(outcome);
	}

	@Test
	void testAuthorize_JwtBadSignature_Denied401() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList(SCOPE));

		AuthorizationOutcome outcome = authorizeJwt(jwt, false);

		assertFalse(outcome.isAllowed());
		assertNull(outcome.getClient());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	@Test
	void testAuthorize_JwtBadSignatureAndInsufficientScope_Denied401() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList("other.scope"));

		AuthorizationOutcome outcome = authorizeJwt(jwt, false);

		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
	}

	@Test
	void testAuthorize_JwtExpired_Denied401() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList(SCOPE));
		when(jwt.getClaims().getClaimAsInteger(JwtClaimName.EXPIRATION_TIME)).thenReturn((int) (System.currentTimeMillis() / 1000L) - 60);

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	@Test
	void testAuthorize_JwtWrongIssuer_Denied401() throws Exception {
		Jwt jwt = mockJwt("2200.aaaa", null, Collections.singletonList(SCOPE));
		when(jwt.getClaims().getClaimAsString(JwtClaimName.ISSUER)).thenReturn("https://other.example.org");

		AuthorizationOutcome outcome = authorizeJwt(jwt, true);

		assertFalse(outcome.isAllowed());
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), outcome.getErrorResponse().getStatus());
		assertInvalidTokenChallenge(outcome);
	}

	// -- helpers --------------------------------------------------------------------------------

	private static void assertInvalidTokenChallenge(AuthorizationOutcome outcome) {
		String challenge = outcome.getErrorResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
		assertNotNull(challenge);
		assertTrue(challenge.startsWith("Bearer error=\"invalid_token\""), challenge);
	}

	private static void assertInsufficientScopeChallenge(AuthorizationOutcome outcome) {
		String challenge = outcome.getErrorResponse().getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
		assertNotNull(challenge);
		assertTrue(challenge.startsWith("Bearer error=\"insufficient_scope\""), challenge);
		assertTrue(challenge.contains("scope=\"" + SCOPE + "\""), challenge);
	}

	private AuthorizationOutcome authorizeJwt(Jwt jwt, boolean signatureValid) throws Exception {
		try (MockedStatic<Jwt> jwtStatic = mockStatic(Jwt.class);
				MockedConstruction<AuthCryptoProvider> crypto = mockConstruction(AuthCryptoProvider.class,
						(m, ctx) -> when(m.verifySignature(any(), any(), any(), any(), any(), any()))
								.thenReturn(signatureValid))) {
			jwtStatic.when(() -> Jwt.parse(anyString())).thenReturn(jwt);
			when(mapper.readValue(any(URL.class), eq(Map.class))).thenReturn(Map.of());

			return service.authorize("Bearer h.p.s", resourceInfo);
		}
	}

	private static Jwt mockJwt(String clientId, String azp, List<String> scopes) throws Exception {
		JwtHeader header = mock(JwtHeader.class);
		lenient().when(header.getSignatureAlgorithm()).thenReturn(SignatureAlgorithm.RS256);
		lenient().when(header.getKeyId()).thenReturn("kid-1");

		JwtClaims claims = mock(JwtClaims.class);
		lenient().when(claims.getClaimAsString(JwtClaimName.ISSUER)).thenReturn(ISSUER);
		lenient().when(claims.getClaimAsInteger(JwtClaimName.EXPIRATION_TIME))
				.thenReturn((int) (System.currentTimeMillis() / 1000L) + 3600);
		lenient().when(claims.getClaimAsStringList("scope")).thenReturn(scopes);
		lenient().when(claims.getClaimAsString(OpenIdProtectionService.CLIENT_ID_CLAIM)).thenReturn(clientId);
		lenient().when(claims.getClaimAsString(JwtClaimName.AUTHORIZED_PARTY)).thenReturn(azp);

		Jwt jwt = mock(Jwt.class);
		lenient().when(jwt.getHeader()).thenReturn(header);
		lenient().when(jwt.getClaims()).thenReturn(claims);
		lenient().when(jwt.getSigningInput()).thenReturn("h.p");
		lenient().when(jwt.getEncodedSignature()).thenReturn("s");
		return jwt;
	}

}
