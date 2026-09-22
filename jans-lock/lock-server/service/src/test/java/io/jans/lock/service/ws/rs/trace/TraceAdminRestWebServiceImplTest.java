/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyRegistrationRequest;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.api.ProducerKeyRevokeRequest;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainRegistration;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.parse.TraceValidationException;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link TraceAdminRestWebServiceImpl}: the happy paths for all five admin operations,
 * the disabled-feature/unbound-client/duplicate/not-found error paths (task 15 acceptance
 * criteria), and a fixture check that every {@link TraceAdminRestWebService} method carries both
 * protection annotations with matching {@code id}/{@code path} (task 15's "Tests" section).
 */
class TraceAdminRestWebServiceImplTest {

	private static final String DOMAIN = "domain-1";

	private static final String CLIENT_ID = "2200.abcd";

	@Mock
	private AppConfiguration appConfiguration;

	@Mock
	private ErrorResponseFactory errorResponseFactory;

	@Mock
	private SubmitterIdentityService submitterIdentityService;

	@Mock
	private EvidenceDomainResolver evidenceDomainResolver;

	@Mock
	private ProducerKeyRegistry producerKeyRegistry;

	@Mock
	private ProducerChainRegistry producerChainRegistry;

	@Mock
	private ApplicationAuditLogger applicationAuditLogger;

	private TraceConfiguration traceConfiguration;

	private TraceAdminRestWebServiceImpl impl;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		impl = new TraceAdminRestWebServiceImpl();
		impl.setLog(LoggerFactory.getLogger(TraceAdminRestWebServiceImpl.class));
		impl.setAppConfiguration(appConfiguration);
		impl.setErrorResponseFactory(errorResponseFactory);
		impl.setSubmitterIdentityService(submitterIdentityService);
		impl.setEvidenceDomainResolver(evidenceDomainResolver);
		impl.setProducerKeyRegistry(producerKeyRegistry);
		impl.setProducerChainRegistry(producerChainRegistry);
		impl.setApplicationAuditLogger(applicationAuditLogger);

		traceConfiguration = new TraceConfiguration();
		traceConfiguration.setEnabled(true);
		lenient().when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		SubmitterIdentity identity = new SubmitterIdentity(CLIENT_ID, true);
		lenient().when(submitterIdentityService.resolve(any())).thenReturn(identity);

		TraceRequestContext context = new TraceRequestContext(CLIENT_ID, DOMAIN, List.of("*"), 5000L, "node-1");
		lenient().when(evidenceDomainResolver.resolve(identity)).thenReturn(context);

		lenient().when(errorResponseFactory.traceException(any(TraceErrorResponseType.class), anyString()))
				.thenAnswer(inv -> toWebApplicationException(inv.getArgument(0), inv.getArgument(1)));
		lenient()
				.when(errorResponseFactory.traceException(any(TraceErrorResponseType.class), anyString(), any()))
				.thenAnswer(inv -> toWebApplicationException(inv.getArgument(0), inv.getArgument(1)));
	}

	private static WebApplicationException toWebApplicationException(TraceErrorResponseType type, String reason) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("error", type.getParameter());
		body.put("reason", reason);
		return new WebApplicationException(Response.status(type.httpStatus()).entity(body).build());
	}

	private static Map<String, String> jwk() {
		Map<String, String> jwk = new LinkedHashMap<>();
		jwk.put("kty", "OKP");
		jwk.put("crv", "Ed25519");
		jwk.put("x", "abc");
		jwk.put("d", "should-never-be-echoed-back");
		return jwk;
	}

	private static ProducerKey producerKey() {
		return new ProducerKey(DOMAIN, "cedarling-fleet-1/1.0.0", "kid-1", jwk(), 1000L, null, null, CLIENT_ID,
				1000L);
	}

	// -- registerProducerKey -----------------------------------------------------------------------

	@Test
	void testRegisterProducerKey_HappyPath_Returns201WithFilteredJwk() {
		ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setKid("kid-1");
		request.setPublicKeyJwk(jwk());
		request.setValidFrom("1970-01-01T00:00:01Z");

		when(producerKeyRegistry.register(eq(DOMAIN), eq("cedarling-fleet-1/1.0.0"), eq("kid-1"), any(), eq(1000L),
				isNull(), eq(CLIENT_ID), eq(5000L))).thenReturn(producerKey());

		Response response = impl.registerProducerKey(request);

		assertEquals(201, response.getStatus());
		Object cacheControl = response.getMetadata().getFirst("Cache-Control");
		assertNotNull(cacheControl);
		assertTrue(((jakarta.ws.rs.core.CacheControl) cacheControl).isNoStore());
		ProducerKeyResponse body = (ProducerKeyResponse) response.getEntity();
		assertEquals(DOMAIN, body.getEvidenceDomainId());
		assertEquals("kid-1", body.getKid());
		assertEquals(Map.of("kty", "OKP", "crv", "Ed25519", "x", "abc"), body.getPublicKeyJwk());
		assertEquals(null, body.getRevokedAt());
		verify(applicationAuditLogger).log(any(), eq(true));
	}

	@Test
	void testRegisterProducerKey_Duplicate_Returns409() {
		ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setKid("kid-1");
		request.setPublicKeyJwk(jwk());
		request.setValidFrom("1970-01-01T00:00:01Z");

		when(producerKeyRegistry.register(anyString(), anyString(), anyString(), anyMap(), anyLong(), any(),
				anyString(), anyLong()))
				.thenThrow(new TraceConflictException(TraceErrorResponseType.KEY_ALREADY_EXISTS, "duplicate_key"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.registerProducerKey(request));

		assertEquals(409, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), eq(false));
	}

	@Test
	void testRegisterProducerKey_BadTimestamp_Returns400InvalidKey() {
		ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setKid("kid-1");
		request.setPublicKeyJwk(jwk());
		request.setValidFrom("not-a-timestamp");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.registerProducerKey(request));

		assertEquals(400, ex.getResponse().getStatus());
	}

	@Test
	void testRegisterProducerKey_DisabledFeature_Returns400InvalidRequest() {
		traceConfiguration.setEnabled(false);

		ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.registerProducerKey(request));

		assertEquals(400, ex.getResponse().getStatus());
		@SuppressWarnings("unchecked")
		Map<String, String> body = (Map<String, String>) ex.getResponse().getEntity();
		assertEquals("invalid_request", body.get("error"));
		assertEquals("trace_disabled", body.get("reason"));
		verify(applicationAuditLogger).log(any(), eq(false));
	}

	@Test
	void testRegisterProducerKey_UnboundClient_Returns403() {
		SubmitterIdentity identity = new SubmitterIdentity(CLIENT_ID, true);
		when(submitterIdentityService.resolve(any())).thenReturn(identity);
		when(evidenceDomainResolver.resolve(identity))
				.thenThrow(new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, "no_domain_binding"));

		ProducerKeyRegistrationRequest request = new ProducerKeyRegistrationRequest();

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.registerProducerKey(request));

		assertEquals(403, ex.getResponse().getStatus());
	}

	// -- listProducerKeys ---------------------------------------------------------------------------

	@Test
	void testListProducerKeys_HappyPath_Returns200() {
		when(producerKeyRegistry.list(DOMAIN, "cedarling-fleet-1/1.0.0")).thenReturn(List.of(producerKey()));

		Response response = impl.listProducerKeys("cedarling-fleet-1/1.0.0");

		assertEquals(200, response.getStatus());
		ProducerKeyListResponse body = (ProducerKeyListResponse) response.getEntity();
		assertEquals(1, body.getKeys().size());
		assertEquals("kid-1", body.getKeys().get(0).getKid());
	}

	// -- revokeProducerKey --------------------------------------------------------------------------

	@Test
	void testRevokeProducerKey_KnownKey_Returns200() {
		ProducerKeyRevokeRequest request = new ProducerKeyRevokeRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setKid("kid-1");

		when(producerKeyRegistry.revoke(DOMAIN, "cedarling-fleet-1/1.0.0", "kid-1", 5000L))
				.thenReturn(Optional.of(producerKey().withRevokedAt(5000L)));

		Response response = impl.revokeProducerKey(request);

		assertEquals(200, response.getStatus());
		ProducerKeyResponse body = (ProducerKeyResponse) response.getEntity();
		assertNotNull(body.getRevokedAt());
	}

	@Test
	void testRevokeProducerKey_UnknownKey_Returns404() {
		ProducerKeyRevokeRequest request = new ProducerKeyRevokeRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setKid("kid-1");

		when(producerKeyRegistry.revoke(DOMAIN, "cedarling-fleet-1/1.0.0", "kid-1", 5000L))
				.thenReturn(Optional.empty());

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.revokeProducerKey(request));

		assertEquals(404, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), eq(false));
	}

	@Test
	void testRevokeProducerKey_MissingKid_Returns400() {
		ProducerKeyRevokeRequest request = new ProducerKeyRevokeRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.revokeProducerKey(request));

		assertEquals(400, ex.getResponse().getStatus());
	}

	// -- registerProducerChain -----------------------------------------------------------------------

	@Test
	void testRegisterProducerChain_HappyPath_Returns201() {
		ProducerChainRegistrationRequest request = new ProducerChainRegistrationRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setProducerInstanceId("instance-1");
		request.setProducerChainId("chain-1");

		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "cedarling-fleet-1/1.0.0", "instance-1", "chain-1");
		ChainRegistration registration = new ChainRegistration(chainIdentity, 5000L, CLIENT_ID);
		when(producerChainRegistry.register(chainIdentity, CLIENT_ID, 5000L)).thenReturn(registration);

		Response response = impl.registerProducerChain(request);

		assertEquals(201, response.getStatus());
		ProducerChainResponse body = (ProducerChainResponse) response.getEntity();
		assertEquals("chain-1", body.getProducerChainId());
		assertEquals(DOMAIN, body.getEvidenceDomainId());
	}

	@Test
	void testRegisterProducerChain_Duplicate_Returns409() {
		ProducerChainRegistrationRequest request = new ProducerChainRegistrationRequest();
		request.setProducerId("cedarling-fleet-1/1.0.0");
		request.setProducerInstanceId("instance-1");
		request.setProducerChainId("chain-1");

		when(producerChainRegistry.register(any(), anyString(), anyLong())).thenThrow(
				new TraceConflictException(TraceErrorResponseType.CHAIN_ALREADY_EXISTS, "duplicate_chain"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.registerProducerChain(request));

		assertEquals(409, ex.getResponse().getStatus());
	}

	// -- listProducerChains -------------------------------------------------------------------------

	@Test
	void testListProducerChains_HappyPath_Returns200() {
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "cedarling-fleet-1/1.0.0", "instance-1", "chain-1");
		ChainRegistration registration = new ChainRegistration(chainIdentity, 5000L, CLIENT_ID);
		when(producerChainRegistry.list(DOMAIN, null)).thenReturn(List.of(registration));

		Response response = impl.listProducerChains(null);

		assertEquals(200, response.getStatus());
		ProducerChainListResponse body = (ProducerChainListResponse) response.getEntity();
		assertEquals(1, body.getChains().size());
		assertEquals("chain-1", body.getChains().get(0).getProducerChainId());
	}

	// -- annotation fixture check (task 15 "Tests" section) ------------------------------------------

	@Test
	void testInterfaceMethods_CarryBothProtectionAnnotationsWithMatchingIdAndPath() throws Exception {
		Map<String, String[]> expected = new LinkedHashMap<>();
		expected.put("registerProducerKey",
				new String[] { "POST", "lock_audit_trace_admin_key_write", "/audit/trace/admin/producer-keys" });
		expected.put("listProducerKeys",
				new String[] { "GET", "lock_audit_trace_admin_key_read", "/audit/trace/admin/producer-keys" });
		expected.put("revokeProducerKey", new String[] { "POST", "lock_audit_trace_admin_key_revoke",
				"/audit/trace/admin/producer-keys/revoke" });
		expected.put("registerProducerChain",
				new String[] { "POST", "lock_audit_trace_admin_chain_write", "/audit/trace/admin/producer-chains" });
		expected.put("listProducerChains",
				new String[] { "GET", "lock_audit_trace_admin_chain_read", "/audit/trace/admin/producer-chains" });

		assertEquals(expected.size(), TraceAdminRestWebService.class.getDeclaredMethods().length,
				"unexpected number of methods on the admin interface");

		for (Method method : TraceAdminRestWebService.class.getDeclaredMethods()) {
			String[] spec = expected.get(method.getName());
			assertNotNull(spec, "no expectation registered for method " + method.getName());

			ProtectedApi protectedApi = method.getAnnotation(ProtectedApi.class);
			ProtectedCedarlingApi cedarlingApi = method.getAnnotation(ProtectedCedarlingApi.class);

			assertNotNull(protectedApi, "@ProtectedApi missing on " + method.getName());
			assertNotNull(cedarlingApi, "@ProtectedCedarlingApi missing on " + method.getName());

			assertEquals(1, protectedApi.scopes().length);
			assertEquals(ApiAccessConstants.LOCK_TRACE_ADMIN_ACCESS, protectedApi.scopes()[0]);
			assertEquals("", protectedApi.grpcMethodName(), "TRACE admin has no gRPC transport");

			assertEquals("Jans::Action::\"" + spec[0] + "\"", cedarlingApi.action());
			assertEquals("Jans::HTTP_Request", cedarlingApi.resource());
			assertEquals(spec[1], cedarlingApi.id());
			assertEquals(spec[2], cedarlingApi.path());
		}
	}

}
