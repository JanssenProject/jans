/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.core.cedarling.service.security.api.ProtectedCedarlingApi;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.ingest.AcceptanceResult;
import io.jans.lock.service.trace.ingest.TraceIngestionService;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.util.ApiAccessConstants;
import io.jans.service.security.api.ProtectedApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link TraceRestWebServiceImpl} (TRACE MVP task 20 acceptance criteria): the 202 happy
 * path and idempotent-replay path, the {@code record_conflict}/{@code client_not_bound}/validation/
 * {@code storage_failure}/disabled-feature error paths, the audit-logger call on every outcome, and
 * a fixture check of the interface's protection annotations.
 */
class TraceRestWebServiceImplTest extends BaseLockServiceTest {

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
	private TraceIngestionService ingestionService;

	@Mock
	private ApplicationAuditLogger applicationAuditLogger;

	@Mock
	private HttpServletRequest httpServletRequest;

	private TraceConfiguration traceConfiguration;

	private TraceRequestContext context;

	private TraceRestWebServiceImpl impl;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		impl = new TraceRestWebServiceImpl();
		setField(impl, "log", LoggerFactory.getLogger(TraceRestWebServiceImpl.class));
		setField(impl, "appConfiguration", appConfiguration);
		setField(impl, "errorResponseFactory", errorResponseFactory);
		setField(impl, "submitterIdentityService", submitterIdentityService);
		setField(impl, "evidenceDomainResolver", evidenceDomainResolver);
		setField(impl, "ingestionService", ingestionService);
		setField(impl, "applicationAuditLogger", applicationAuditLogger);

		traceConfiguration = new TraceConfiguration();
		traceConfiguration.setEnabled(true);
		lenient().when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		SubmitterIdentity identity = new SubmitterIdentity(CLIENT_ID, true);
		lenient().when(submitterIdentityService.resolve(any())).thenReturn(identity);

		context = new TraceRequestContext(CLIENT_ID, DOMAIN, List.of("*"), 5000L, "node-1");
		lenient().when(evidenceDomainResolver.resolve(identity)).thenReturn(context);

		lenient().when(errorResponseFactory.traceException(any(TraceErrorResponseType.class), org.mockito.ArgumentMatchers.anyString()))
				.thenAnswer(inv -> toWebApplicationException(inv.getArgument(0), inv.getArgument(1)));
		lenient().when(errorResponseFactory.traceException(any(TraceErrorResponseType.class), org.mockito.ArgumentMatchers.anyString(), any()))
				.thenAnswer(inv -> toWebApplicationException(inv.getArgument(0), inv.getArgument(1)));
	}

	private static WebApplicationException toWebApplicationException(TraceErrorResponseType type, String reason) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("error", type.getParameter());
		body.put("reason", reason);
		return new WebApplicationException(Response.status(type.httpStatus()).entity(body).build());
	}

	private static InputStream body() {
		return new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
	}

	private static StoredTraceRecord storedRecord(boolean late) {
		RecordIdentity identity = new RecordIdentity(DOMAIN, "cedarling-fleet-1/1.0.0", "record-1");
		VerificationResult verification = new VerificationResult(true, "kid-1", 5000L, "Ed25519");
		ReceiptEntry receipt = new ReceiptEntry(108422L, 5000L, "sha256:" + "0".repeat(64), "sha256:" + "1".repeat(64));
		IngestionFlags flags = new IngestionFlags(false, false, false, late);
		ExecutionIdentity execution = new ExecutionIdentity(DOMAIN, "spiffe://example.org/agent/planner", "exec-1");
		ChainIdentity chainIdentity = new ChainIdentity(DOMAIN, "cedarling-fleet-1/1.0.0", "instance-1", "chain-1");
		ChainPosition chainPosition = new ChainPosition(chainIdentity, 1L);
		return new StoredTraceRecord(identity, "{}", "sha256:" + "2".repeat(64), verification, receipt, flags,
				execution, chainPosition, "sha256:" + "0".repeat(64), List.of(), List.of(), "AUTHORIZATION_DECISION",
				4000L, "node-1", 5000L);
	}

	// -- happy path -------------------------------------------------------------------------------

	@Test
	void testSubmitRecord_HappyPath_Returns202() {
		StoredTraceRecord stored = storedRecord(false);
		when(ingestionService.ingest(any(), org.mockito.ArgumentMatchers.eq(context)))
				.thenReturn(AcceptanceResult.fromStored(stored, false));

		Response response = impl.submitRecord(body(), httpServletRequest, null);

		assertEquals(202, response.getStatus());
		Object cacheControl = response.getMetadata().getFirst("Cache-Control");
		assertNotNull(cacheControl);
		assertTrue(((CacheControl) cacheControl).isNoStore());
		assertEquals("no-cache", response.getHeaderString("Pragma"));

		TraceAcceptanceResponse dto = (TraceAcceptanceResponse) response.getEntity();
		assertTrue(dto.isAccepted());
		assertEquals("cedarling-fleet-1/1.0.0", dto.getProducerId());
		assertEquals("record-1", dto.getRecordId());
		assertEquals(108422L, dto.getReceiptSequence());
		assertFalse(dto.isIdempotentReplay());
		assertFalse(dto.isLateFlag());

		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(true));
	}

	@Test
	void testSubmitRecord_IdempotentReplay_Returns202WithReplayFlag() {
		StoredTraceRecord stored = storedRecord(false);
		when(ingestionService.ingest(any(), org.mockito.ArgumentMatchers.eq(context)))
				.thenReturn(AcceptanceResult.fromStored(stored, true));

		Response response = impl.submitRecord(body(), httpServletRequest, null);

		assertEquals(202, response.getStatus());
		TraceAcceptanceResponse dto = (TraceAcceptanceResponse) response.getEntity();
		assertTrue(dto.isIdempotentReplay());
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(true));
	}

	// -- error paths --------------------------------------------------------------------------------

	@Test
	void testSubmitRecord_RecordConflict_Returns409() {
		when(ingestionService.ingest(any(), any()))
				.thenThrow(new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, "digest_mismatch"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.submitRecord(body(), httpServletRequest, null));

		assertEquals(409, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
	}

	@Test
	void testSubmitRecord_ClientNotBound_Returns403() {
		when(evidenceDomainResolver.resolve(any()))
				.thenThrow(new TraceValidationException(TraceErrorResponseType.CLIENT_NOT_BOUND, "no_domain_binding"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.submitRecord(body(), httpServletRequest, null));

		assertEquals(403, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
		verify(ingestionService, never()).ingest(any(), any());
	}

	@Test
	void testSubmitRecord_ValidationFailure_Returns400() {
		when(ingestionService.ingest(any(), any()))
				.thenThrow(new TraceValidationException(TraceErrorResponseType.INVALID_ASSERTION, "missing:trace.signed_at"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.submitRecord(body(), httpServletRequest, null));

		assertEquals(400, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
	}

	@Test
	void testSubmitRecord_StorageFailure_Returns500() {
		when(ingestionService.ingest(any(), any()))
				.thenThrow(new TraceStorageException("receipt_allocation_retry_exhausted", "retries exhausted"));

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.submitRecord(body(), httpServletRequest, null));

		assertEquals(500, ex.getResponse().getStatus());
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
	}

	@Test
	void testSubmitRecord_DisabledFeature_Returns400InvalidRequest() {
		traceConfiguration.setEnabled(false);

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.submitRecord(body(), httpServletRequest, null));

		assertEquals(400, ex.getResponse().getStatus());
		@SuppressWarnings("unchecked")
		Map<String, String> entity = (Map<String, String>) ex.getResponse().getEntity();
		assertEquals("invalid_request", entity.get("error"));
		assertEquals("trace_disabled", entity.get("reason"));
		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
		verify(submitterIdentityService, never()).resolve(any());
	}

	// -- annotation fixture check (task 20 "Tests" section) ------------------------------------------

	@Test
	void testInterfaceMethod_CarriesBothProtectionAnnotationsWithMatchingIdAndPath() throws Exception {
		java.lang.reflect.Method method = TraceRestWebService.class.getDeclaredMethod("submitRecord",
				InputStream.class, HttpServletRequest.class, jakarta.ws.rs.core.SecurityContext.class);

		ProtectedApi protectedApi = method.getAnnotation(ProtectedApi.class);
		ProtectedCedarlingApi cedarlingApi = method.getAnnotation(ProtectedCedarlingApi.class);

		assertNotNull(protectedApi, "@ProtectedApi missing on submitRecord");
		assertNotNull(cedarlingApi, "@ProtectedCedarlingApi missing on submitRecord");

		assertEquals(1, protectedApi.scopes().length);
		assertEquals(ApiAccessConstants.LOCK_TRACE_WRITE_ACCESS, protectedApi.scopes()[0]);
		assertEquals("", protectedApi.grpcMethodName(), "TRACE write has no gRPC transport (D-15)");

		assertEquals("Jans::Action::\"POST\"", cedarlingApi.action());
		assertEquals("Jans::HTTP_Request", cedarlingApi.resource());
		assertEquals("lock_audit_trace_write", cedarlingApi.id());
		assertEquals("/audit/trace", cedarlingApi.path());
	}

}
