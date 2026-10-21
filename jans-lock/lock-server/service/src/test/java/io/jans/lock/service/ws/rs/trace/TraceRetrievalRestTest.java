/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.ws.rs.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.jans.lock.model.trace.api.TraceRecordSearchResponse;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.BaseLockServiceTest;
import io.jans.lock.service.app.audit.ApplicationAuditLogger;
import io.jans.lock.service.trace.identity.EvidenceDomainResolver;
import io.jans.lock.service.trace.identity.SubmitterIdentity;
import io.jans.lock.service.trace.identity.SubmitterIdentityService;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.retrieve.TraceRetrievalService;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.store.InMemoryTraceStore;
import io.jans.lock.service.trace.testkit.StoredTraceRecordFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link TraceRestWebServiceImpl#getRecord(String, String)} and
 * {@link TraceRestWebServiceImpl#getExecution(String, String, int, int)} domain-scoped retrieval, the bare-identifier ambiguity cases, paging
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
	void testGetRecord_AssertionRaw_ReturnedByteForByte() throws Exception {
		String raw = "{ \"z\" : 1.0,\n  \"a\":1e2, \"trace\" : {} }";
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).assertionRaw(raw).build());
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", "producer-1").getEntity();

		assertEquals(raw, dto.getAssertionRaw());
		assertNotNull(dto.getAssertion());
		assertEquals("z", dto.getAssertion().fieldNames().next());
	}

	@Test
	void testGetRecord_ReceiptEntryCommitted_IngestionCarriesReceiptState() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).receiptSequence(7L).build());
		traceStore.insertReceipt(receiptRow("domain-a", 7L, TraceReceiptState.COMMITTED));
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", "producer-1").getEntity();

		assertEquals("COMMITTED", dto.getIngestion().getReceiptState());
		assertEquals("COMMITTED",
				new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(dto.getIngestion()).get("receipt_state").asText());
	}

	@Test
	void testGetRecord_ReceiptEntryPending_IngestionCarriesPendingState() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).receiptSequence(7L).build());
		traceStore.insertReceipt(receiptRow("domain-a", 7L, TraceReceiptState.PENDING));
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", "producer-1").getEntity();

		assertEquals("PENDING", dto.getIngestion().getReceiptState());
	}

	@Test
	void testGetRecord_NoReceiptRow_ReceiptStateAbsent() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest("sha256:" + "1".repeat(64)).receiptSequence(7L).build());
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", "producer-1").getEntity();

		assertNull(dto.getIngestion().getReceiptState());
	}

	private static TraceReceiptEntry receiptRow(String domainId, long seq, TraceReceiptState state) {
		TraceReceiptEntry row = new TraceReceiptEntry();
		row.setDomainId(domainId);
		row.setReceiptSeq(seq);
		row.setProducerId("producer-1");
		row.setRecordId("record-1");
		row.setReceiptStateEnum(state);
		return row;
	}

	@Test
	void testGetRecord_VerificationBlockWithNewMembers_returnedAndSerialized() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").recordId("record-1")
				.contentDigest("sha256:" + "1".repeat(64)).verification(new VerificationResult(true, "kid-1", 1000L, "Ed25519", "sha256:" + "a".repeat(64),
						Boolean.TRUE))
				.build());
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", null).getEntity();

		assertEquals("sha256:" + "a".repeat(64), dto.getVerification().getKeyThumbprint());
		assertEquals(Boolean.TRUE, dto.getVerification().getKeyAuthorized());
		com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
				.valueToTree(dto.getVerification());
		assertEquals("sha256:" + "a".repeat(64), json.get("key_thumbprint").textValue());
		assertTrue(json.get("key_authorized").booleanValue());
		assertEquals("kid-1", json.get("key_id").textValue());
	}

	@Test
	void testGetRecord_LegacyVerificationBlock_omitsNewMembers() throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").recordId("record-1")
				.contentDigest("sha256:" + "1".repeat(64)).build());
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", null).getEntity();

		com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
				.valueToTree(dto.getVerification());
		assertFalse(json.has("key_thumbprint"));
		assertFalse(json.has("key_authorized"));
		assertTrue(json.has("signature_valid"));
	}

	@Test
	void testGetRecord_ReceivedAtAndVerifiedAtZeroMillis_ThreeFractionDigitsAndHashReproducible() throws Exception {
		long receivedAtMs = 1_800_000_000_000L;
		String contentDigest = "sha256:" + "a".repeat(64);
		String zero = "sha256:" + "0".repeat(64);
		String canonical = "{\"content_digest\":\"" + contentDigest + "\",\"evidence_domain_id\":\"domain-a\""
				+ ",\"prev_receipt_hash\":\"" + zero + "\",\"producer_id\":\"producer-1\",\"receipt_profile\":\"tag:jans.io,2026:lock-trace-receipt-v1\",\"receipt_sequence\":1"
				+ ",\"received_at\":\"2027-01-15T08:00:00.000Z\",\"record_id\":\"record-1\"}";
		String receiptHash = "sha256:" + String.format("%064x", new java.math.BigInteger(1,
				java.security.MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").producerId("producer-1")
				.recordId("record-1").contentDigest(contentDigest).receiptSequence(1L).receivedAtMs(receivedAtMs)
				.prevReceiptHash(zero).receiptHash(receiptHash)
				.verification(new VerificationResult(true, "kid-1", receivedAtMs, "Ed25519")).build());
		boundToDomain("domain-a");

		TraceRecordResponse dto = (TraceRecordResponse) impl.getRecord("record-1", "producer-1").getEntity();

		assertEquals("2027-01-15T08:00:00.000Z", dto.getIngestion().getReceivedAt());
		assertEquals("2027-01-15T08:00:00.000Z", dto.getVerification().getVerifiedAt());
		// recompute from API output alone
		String fromApi = "{\"content_digest\":\"" + dto.getContentDigest() + "\",\"evidence_domain_id\":\"domain-a\""
				+ ",\"prev_receipt_hash\":\"" + dto.getIngestion().getPrevReceiptHash() + "\",\"producer_id\":\""
				+ dto.getProducerId() + "\",\"receipt_profile\":\"tag:jans.io,2026:lock-trace-receipt-v1\",\"receipt_sequence\":" + dto.getIngestion().getReceiptSequence()
				+ ",\"received_at\":\"" + dto.getIngestion().getReceivedAt() + "\",\"record_id\":\"" + dto.getRecordId()
				+ "\"}";
		String recomputed = "sha256:" + String.format("%064x", new java.math.BigInteger(1,
				java.security.MessageDigest.getInstance("SHA-256").digest(fromApi.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
		assertEquals(dto.getIngestion().getReceiptHash(), recomputed);
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

		String recordId = "x".repeat(129);
		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.getRecord(recordId, null));
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

	// -- searchRecords ------------------------------------------------------------------------------

	private static final String HASH_A = "sha256:" + "a".repeat(64);

	private static final String HASH_B = "sha256:" + "b".repeat(64);

	private void insertPolicyRecord(String domainId, String recordId, long receiptSequence, long receivedAtMs,
			String storeId, String version, String bundleHash) throws Exception {
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId(domainId).recordId(recordId)
				.receiptSequence(receiptSequence).receivedAtMs(receivedAtMs)
				.contentDigest("sha256:" + String.format("%064d", receiptSequence))
				.policy(storeId, version, bundleHash).policyMismatch(HASH_B.equals(bundleHash)).build());
	}

	@Test
	void testSearchRecords_ByStoreAndVersion_ReturnsMatchesInReceiptOrder() throws Exception {
		insertPolicyRecord("domain-a", "record-2", 2, 2000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-3", 3, 3000L, "store-1", "2.0.0", HASH_B);
		insertPolicyRecord("domain-a", "record-4", 4, 4000L, "store-2", "1.0.0", HASH_A);
		traceStore.insertRecord(StoredTraceRecordFactory.builder().domainId("domain-a").recordId("record-5")
				.receiptSequence(5).contentDigest("sha256:" + "5".repeat(64)).build());
		boundToDomain("domain-a");

		Response response = impl.searchRecords("store-1", "1.0.0", null, null, null, 0, 100);

		assertEquals(200, response.getStatus());
		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) response.getEntity();
		assertEquals("receipt_sequence", dto.getOrdering());
		assertEquals(List.of("record-1", "record-2"),
				dto.getRecords().stream().map(TraceRecordResponse::getRecordId).collect(java.util.stream.Collectors.toList()));
		ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
		verify(applicationAuditLogger).log(captor.capture(), org.mockito.ArgumentMatchers.eq(true));
		assertEquals(AuditActionType.TRACE_RECORD_READ, captor.getValue().getAction());
	}

	@Test
	void testSearchRecords_ByBundleHash_SurfacesPolicyMismatchFlag() throws Exception {
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-3", 3, 3000L, "store-1", "2.0.0", HASH_B);
		boundToDomain("domain-a");

		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) impl
				.searchRecords(null, null, HASH_B, null, null, 0, 100).getEntity();

		assertEquals(1, dto.getRecords().size());
		assertEquals("record-3", dto.getRecords().get(0).getRecordId());
		assertTrue(dto.getRecords().get(0).getIngestion().isPolicyMismatchFlag());
	}

	@Test
	void testSearchRecords_PaddedValue_MatchedExactly() throws Exception {
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, " store-1 ", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-2", 2, 2000L, "store-1", "1.0.0", HASH_A);
		boundToDomain("domain-a");

		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) impl
				.searchRecords(" store-1 ", null, null, null, null, 0, 100).getEntity();

		assertEquals(List.of("record-1"),
				dto.getRecords().stream().map(TraceRecordResponse::getRecordId).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	void testSearchRecords_ReceivedRange_IsInclusive() throws Exception {
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-2", 2, 2000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-3", 3, 3000L, "store-1", "1.0.0", HASH_A);
		boundToDomain("domain-a");

		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) impl.searchRecords("store-1", null, null,
				"1970-01-01T00:00:02.000Z", "1970-01-01T00:00:03Z", 0, 100).getEntity();

		assertEquals(List.of("record-2", "record-3"),
				dto.getRecords().stream().map(TraceRecordResponse::getRecordId).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	void testSearchRecords_Paging_SkipsAndLimits() throws Exception {
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-2", 2, 2000L, "store-1", "1.0.0", HASH_A);
		insertPolicyRecord("domain-a", "record-3", 3, 3000L, "store-1", "1.0.0", HASH_A);
		boundToDomain("domain-a");

		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) impl
				.searchRecords("store-1", null, null, null, null, 1, 1).getEntity();

		assertEquals(1, dto.getRecords().size());
		assertEquals("record-2", dto.getRecords().get(0).getRecordId());
	}

	@Test
	void testSearchRecords_ForeignDomain_ReturnsEmpty() throws Exception {
		insertPolicyRecord("domain-a", "record-1", 1, 1000L, "store-1", "1.0.0", HASH_A);
		boundToDomain("domain-b");

		TraceRecordSearchResponse dto = (TraceRecordSearchResponse) impl
				.searchRecords("store-1", null, null, null, null, 0, 100).getEntity();

		assertTrue(dto.getRecords().isEmpty());
	}

	@Test
	void testSearchRecords_NoStoreIdOrBundleHash_Returns400() {
		boundToDomain("domain-a");

		WebApplicationException ex = assertThrows(WebApplicationException.class,
				() -> impl.searchRecords(null, "1.0.0", " ", null, null, 0, 100));
		assertEquals(400, ex.getResponse().getStatus());
	}

	@Test
	void testSearchRecords_InvalidTimestampOrRange_Returns400() {
		boundToDomain("domain-a");

		assertEquals(400, assertThrows(WebApplicationException.class,
				() -> impl.searchRecords("store-1", null, null, "yesterday", null, 0, 100)).getResponse().getStatus());
		assertEquals(400, assertThrows(WebApplicationException.class,
				() -> impl.searchRecords("store-1", null, null, "2026-01-02T00:00:00Z", "2026-01-01T00:00:00Z", 0,
						100)).getResponse().getStatus());
		assertEquals(400, assertThrows(WebApplicationException.class,
				() -> impl.searchRecords("store-1", null, null, null, null, 0, 1001)).getResponse().getStatus());
	}

}
