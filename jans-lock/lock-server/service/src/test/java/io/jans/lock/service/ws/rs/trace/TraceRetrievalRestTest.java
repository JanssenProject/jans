/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import io.jans.core.cedarling.model.AuditActionType;
import io.jans.core.cedarling.model.AuditLogEntry;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link TraceRestWebServiceImpl#getRecord(String, String)} and
 * {@link TraceRestWebServiceImpl#getExecution(String, String, int, int)} (TRACE MVP task 21
 * acceptance criteria): domain-scoped retrieval, the bare-identifier ambiguity cases, paging
 * validation, and the audit-logger call on every outcome. Backed by {@link InMemoryTraceStore}
 * pre-populated via {@link StoredTraceRecordFactory}; identity/domain services are mocked to
 * return fixed contexts.
 */
class TraceRetrievalRestTest extends BaseLockServiceTest {

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
	private ApplicationAuditLogger applicationAuditLogger;

	@Mock
	private HttpServletRequest httpServletRequest;

	private TraceConfiguration traceConfiguration;

	private InMemoryTraceStore traceStore;

	private TraceRestWebServiceImpl impl;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		traceStore = new InMemoryTraceStore();
		TraceRetrievalService retrievalService = new TraceRetrievalService();
		setField(retrievalService, "traceStore", traceStore);

		impl = new TraceRestWebServiceImpl();
		setField(impl, "log", LoggerFactory.getLogger(TraceRestWebServiceImpl.class));
		setField(impl, "appConfiguration", appConfiguration);
		setField(impl, "errorResponseFactory", errorResponseFactory);
		setField(impl, "submitterIdentityService", submitterIdentityService);
		setField(impl, "evidenceDomainResolver", evidenceDomainResolver);
		setField(impl, "retrievalService", retrievalService);
		setField(impl, "applicationAuditLogger", applicationAuditLogger);

		traceConfiguration = new TraceConfiguration();
		traceConfiguration.setEnabled(true);
		lenient().when(appConfiguration.getTraceConfiguration()).thenReturn(traceConfiguration);

		SubmitterIdentity identity = new SubmitterIdentity(CLIENT_ID, true);
		lenient().when(submitterIdentityService.resolve(any())).thenReturn(identity);

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

	private void boundToDomain(String domainId) {
		TraceRequestContext context = new TraceRequestContext(CLIENT_ID, domainId, List.of("*"), 5000L, "node-1");
		when(evidenceDomainResolver.resolve(any())).thenReturn(context);
	}

	// -- getRecord ----------------------------------------------------------------------------------

	@Test
	void testGetRecord_SameRecordIdInTwoDomains_ReturnsDomainScopedRecord() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").recordId("record-1")
				.contentDigest("sha256:" + "a".repeat(64)).build());
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-b").recordId("record-1")
				.contentDigest("sha256:" + "b".repeat(64)).build());

		boundToDomain("domain-a");
		Response responseA = impl.getRecord("record-1", null);
		assertEquals(200, responseA.getStatus());
		TraceRecordResponse dtoA = (TraceRecordResponse) responseA.getEntity();
		assertEquals("sha256:" + "a".repeat(64), dtoA.getContentDigest());

		boundToDomain("domain-b");
		Response responseB = impl.getRecord("record-1", null);
		assertEquals(200, responseB.getStatus());
		TraceRecordResponse dtoB = (TraceRecordResponse) responseB.getEntity();
		assertEquals("sha256:" + "b".repeat(64), dtoB.getContentDigest());

		boundToDomain("domain-c");
		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> impl.getRecord("record-1", null));
		assertEquals(404, ex.getResponse().getStatus());
	}

	@Test
	void testGetRecord_BareRecordIdTwoProducers_Returns409() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).build());
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-2")
				.recordId("record-1").contentDigest("sha256:" + "2".repeat(64)).build());
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> impl.getRecord("record-1", null));
		assertEquals(409, ex.getResponse().getStatus());

		verify(applicationAuditLogger).log(any(), org.mockito.ArgumentMatchers.eq(false));
	}

	@Test
	void testGetRecord_BareRecordIdWithProducerId_Returns200() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).build());
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-2")
				.recordId("record-1").contentDigest("sha256:" + "2".repeat(64)).build());
		boundToDomain("domain-a");

		Response response = impl.getRecord("record-1", "producer-2");

		assertEquals(200, response.getStatus());
		TraceRecordResponse dto = (TraceRecordResponse) response.getEntity();
		assertEquals("producer-2", dto.getProducerId());
		assertEquals("sha256:" + "2".repeat(64), dto.getContentDigest());
		assertNotNull(dto.getAssertion());
		assertNotNull(dto.getVerification());
		assertNotNull(dto.getIngestion());

		ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
		verify(applicationAuditLogger).log(captor.capture(), org.mockito.ArgumentMatchers.eq(true));
		assertEquals(AuditActionType.TRACE_RECORD_READ, captor.getValue().getAction());
	}

	@Test
	void testGetRecord_NotFound_Returns404() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> impl.getRecord("missing", null));
		assertEquals(404, ex.getResponse().getStatus());
	}

	@Test
	void testGetRecord_RecordIdTooLong_Returns400() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getRecord("x".repeat(129), null));
		assertEquals(400, ex.getResponse().getStatus());
		verify(submitterIdentityService, org.mockito.Mockito.never()).resolve(any());
	}

	@Test
	void testGetRecord_DisabledFeature_Returns400InvalidRequest() {
		traceConfiguration.setEnabled(false);

		WebApplicationException ex = assertThrows(WebApplicationException.class, () -> impl.getRecord("record-1", null));

		assertEquals(400, ex.getResponse().getStatus());
		@SuppressWarnings("unchecked")
		Map<String, String> entity = (Map<String, String>) ex.getResponse().getEntity();
		assertEquals("invalid_request", entity.get("error"));
	}

	// -- getExecution ---------------------------------------------------------------------------------

	@Test
	void testGetExecution_DecisionAndInvocationRecords_ReturnsBothOrderedByReceiptSequence() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a")
				.executionAuthority("spiffe://example.org/agent/planner").traceExecutionId("exec-1")
				.recordId("record-2").receiptSequence(2L).contentDigest("sha256:" + "2".repeat(64))
				.eventKind("CAPABILITY_INVOKED").build());
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a")
				.executionAuthority("spiffe://example.org/agent/planner").traceExecutionId("exec-1")
				.recordId("record-1").receiptSequence(1L).contentDigest("sha256:" + "1".repeat(64))
				.eventKind("AUTHORIZATION_DECISION").build());
		boundToDomain("domain-a");

		Response response = impl.getExecution("exec-1", "spiffe://example.org/agent/planner", 0, 100);

		assertEquals(200, response.getStatus());
		TraceExecutionResponse dto = (TraceExecutionResponse) response.getEntity();
		assertEquals("receipt_sequence", dto.getOrdering());
		assertEquals("not_assessed", dto.getCompleteness());
		assertEquals(2, dto.getRecords().size());
		assertEquals("record-1", dto.getRecords().get(0).getRecordId());
		assertEquals("record-2", dto.getRecords().get(1).getRecordId());

		ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
		verify(applicationAuditLogger).log(captor.capture(), org.mockito.ArgumentMatchers.eq(true));
		assertEquals(AuditActionType.TRACE_EXECUTION_READ, captor.getValue().getAction());
	}

	@Test
	void testGetExecution_BareExecutionIdTwoAuthorities_Returns409() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a")
				.executionAuthority("spiffe://example.org/agent/a").traceExecutionId("exec-1").recordId("record-1")
				.contentDigest("sha256:" + "1".repeat(64)).build());
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a")
				.executionAuthority("spiffe://example.org/agent/b").traceExecutionId("exec-1").recordId("record-2")
				.contentDigest("sha256:" + "2".repeat(64)).build());
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getExecution("exec-1", null, 0, 100));
		assertEquals(409, ex.getResponse().getStatus());
	}

	@Test
	void testGetExecution_NotFound_Returns404() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getExecution("missing-exec", null, 0, 100));
		assertEquals(404, ex.getResponse().getStatus());
	}

	@Test
	void testGetExecution_CountZero_Returns400() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getExecution("exec-1", "authority", 0, 0));
		assertEquals(400, ex.getResponse().getStatus());
	}

	@Test
	void testGetExecution_CountTooLarge_Returns400() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getExecution("exec-1", "authority", 0, 1001));
		assertEquals(400, ex.getResponse().getStatus());
	}

	@Test
	void testGetExecution_DomainIsolation_ForeignDomainReturns404() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a")
				.executionAuthority("spiffe://example.org/agent/planner").traceExecutionId("exec-1").recordId("record-1")
				.contentDigest("sha256:" + "1".repeat(64)).build());
		boundToDomain("domain-c");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getExecution("exec-1", "spiffe://example.org/agent/planner", 0, 100));
		assertEquals(404, ex.getResponse().getStatus());
	}

}
