/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

import io.grpc.Status;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceBulkIngestionResponse;
import io.jans.lock.model.trace.api.TraceBulkResultEntry;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceIngestionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.api.TraceRecordSearchResponse;
import io.jans.lock.model.trace.api.TraceVerificationResponse;
import io.jans.lock.model.trace.grpc.BulkAcceptance;
import io.jans.lock.model.trace.grpc.ExecutionResult;
import io.jans.lock.model.trace.grpc.GetExecutionRequest;
import io.jans.lock.model.trace.grpc.GetRecordRequest;
import io.jans.lock.model.trace.grpc.RecordAcceptance;
import io.jans.lock.model.trace.grpc.RecordEnvelope;
import io.jans.lock.model.trace.grpc.RecordSearchResult;
import io.jans.lock.model.trace.grpc.SearchRecordsRequest;
import io.jans.lock.model.trace.grpc.SubmitBulkRecordsRequest;
import io.jans.lock.model.trace.grpc.SubmitRecordRequest;
import io.jans.lock.service.ws.rs.trace.TraceRestWebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

class GrpcTraceServiceImplTest {

	private static final String ERROR_JSON = "{\"error\":\"record_conflict\",\"reason\":\"digest_mismatch\"}";

	private TraceRestWebService rest;

	private GrpcTraceServiceImpl service;

	@BeforeEach
	void setUp() {
		rest = mock(TraceRestWebService.class);
		service = new GrpcTraceServiceImpl(rest, new GrpcTraceMapper(), LoggerFactory.getLogger(GrpcTraceServiceImpl.class));
	}

	private static TraceRecordResponse record(String recordId) throws IOException {
		TraceVerificationResponse verification = new TraceVerificationResponse();
		verification.setSignatureValid(true);
		verification.setKeyId("kid-1");
		verification.setAlgorithm("Ed25519");
		verification.setVerifiedAt("2026-10-27T10:00:00Z");

		TraceIngestionResponse ingestion = new TraceIngestionResponse();
		ingestion.setReceiptSequence(7L);
		ingestion.setReceivedAt("2026-10-27T10:00:00Z");
		ingestion.setReceiptHash("sha256:abc");
		ingestion.setReceiptState("settled");
		ingestion.setLateFlag(true);
		ingestion.setReservedMemberFlag(true);
		ingestion.setReservedMembers(List.of("x_ext"));

		TraceRecordResponse dto = new TraceRecordResponse();
		dto.setProducerId("producer-1");
		dto.setRecordId(recordId);
		dto.setAssertion(new ObjectMapper().readTree("{\"a\":1}"));
		dto.setAssertionRaw("{ \"a\": 1 }");
		dto.setContentDigest("sha256:def");
		dto.setVerification(verification);
		dto.setIngestion(ingestion);
		return dto;
	}

	@Test
	void testSubmitRecord_PassesExactBytesAndNoServletRequest() throws Exception {
		TraceAcceptanceResponse dto = new TraceAcceptanceResponse();
		dto.setAccepted(true);
		dto.setProducerId("producer-1");
		dto.setRecordId("record-1");
		dto.setReceiptSequence(42L);
		dto.setLateFlag(true);
		when(rest.submitRecord(any(), isNull(), isNull())).thenReturn(Response.accepted(dto).build());

		byte[] assertion = "{ \"b\" : 1,\n \"a\":\"é\" }".getBytes(StandardCharsets.UTF_8);
		RecordingObserver<RecordAcceptance> observer = new RecordingObserver<>();
		service.submitRecord(SubmitRecordRequest.newBuilder().setAssertion(ByteString.copyFrom(assertion)).build(), observer);

		ArgumentCaptor<InputStream> body = ArgumentCaptor.forClass(InputStream.class);
		verify(rest).submitRecord(body.capture(), isNull(), isNull());
		assertEquals(new String(assertion, StandardCharsets.UTF_8), new String(body.getValue().readAllBytes(), StandardCharsets.UTF_8));

		assertTrue(observer.completed);
		RecordAcceptance result = observer.values.get(0);
		assertTrue(result.getAccepted());
		assertEquals("record-1", result.getRecordId());
		assertEquals(42L, result.getReceiptSequence());
		assertTrue(result.getFlags().getLateFlag());
		assertFalse(result.getFlags().getEquivocationFlag());
	}

	@Test
	void testSubmitBulkRecords_PassesBodyUnchangedAndMapsResults() throws Exception {
		TraceBulkResultEntry accepted = new TraceBulkResultEntry();
		accepted.setIndex(0);
		accepted.setAccepted(true);
		accepted.setRecordId("r0");
		accepted.setReceiptSequence(5L);
		accepted.setIdempotentReplay(false);
		accepted.setLateFlag(true);
		accepted.setCoverageGapFlag(false);
		accepted.setChainLinkFailureFlag(false);
		accepted.setEquivocationFlag(false);
		accepted.setPolicyMismatchFlag(false);
		accepted.setReservedMemberFlag(false);

		io.jans.lock.model.trace.api.TraceErrorResponse error = new io.jans.lock.model.trace.api.TraceErrorResponse();
		error.setError("invalid_signature");
		error.setReason("bad");
		TraceBulkResultEntry rejected = new TraceBulkResultEntry();
		rejected.setIndex(1);
		rejected.setAccepted(false);
		rejected.setRecordId("r1");
		rejected.setError(error);

		TraceBulkIngestionResponse dto = new TraceBulkIngestionResponse();
		dto.setResults(List.of(accepted, rejected));
		when(rest.submitBulkRecords(any(), isNull(), isNull())).thenReturn(Response.status(207).entity(dto).build());

		String array = "[{\"a\":1},\n{\"b\":2}]";
		RecordingObserver<BulkAcceptance> observer = new RecordingObserver<>();
		service.submitBulkRecords(SubmitBulkRecordsRequest.newBuilder()
				.setRecords(ByteString.copyFromUtf8(array)).build(), observer);

		ArgumentCaptor<InputStream> body = ArgumentCaptor.forClass(InputStream.class);
		verify(rest).submitBulkRecords(body.capture(), isNull(), isNull());
		assertEquals(array, new String(body.getValue().readAllBytes(), StandardCharsets.UTF_8));

		BulkAcceptance result = observer.values.get(0);
		assertEquals(2, result.getResultsCount());
		assertTrue(result.getResults(0).getAccepted());
		assertTrue(result.getResults(0).hasFlags());
		assertTrue(result.getResults(0).getFlags().getLateFlag());
		assertEquals(5L, result.getResults(0).getReceiptSequence());
		assertFalse(result.getResults(1).getAccepted());
		assertFalse(result.getResults(1).hasFlags());
		assertFalse(result.getResults(1).hasReceiptSequence());
		assertEquals("invalid_signature", result.getResults(1).getError().getError());
	}

	@Test
	void testGetRecord_MapsEnvelopeAndTreatsUnsetProducerAsNull() throws Exception {
		when(rest.getRecord(eq("record-1"), isNull())).thenReturn(Response.ok(record("record-1")).build());

		RecordingObserver<RecordEnvelope> observer = new RecordingObserver<>();
		service.getRecord(GetRecordRequest.newBuilder().setRecordId("record-1").build(), observer);

		RecordEnvelope result = observer.values.get(0);
		assertEquals("record-1", result.getRecordId());
		assertEquals("{\"a\":1}", result.getAssertion());
		assertEquals("{ \"a\": 1 }", result.getAssertionRaw());
		assertTrue(result.getVerification().getSignatureValid());
		assertFalse(result.getVerification().hasKeyAuthorized());
		assertEquals(7L, result.getIngestion().getReceiptSequence());
		assertTrue(result.getIngestion().getFlags().getLateFlag());
		assertEquals(List.of("x_ext"), result.getIngestion().getFlags().getReservedMembersList());
	}

	@Test
	void testGetRecord_BlankProducerIsNull() {
		when(rest.getRecord(eq("record-1"), isNull())).thenReturn(Response.ok(new TraceRecordResponse()).build());

		RecordingObserver<RecordEnvelope> observer = new RecordingObserver<>();
		service.getRecord(GetRecordRequest.newBuilder().setRecordId("record-1").setProducerId(" ").build(), observer);

		verify(rest).getRecord("record-1", null);
		assertTrue(observer.completed);
	}

	@Test
	void testSearchRecords_DefaultsStartAndCountLikeRest() throws Exception {
		TraceRecordSearchResponse dto = new TraceRecordSearchResponse();
		dto.setRecords(List.of(record("r1"), record("r2")));
		when(rest.searchRecords(any(), any(), any(), any(), any(), eq(0), eq(100))).thenReturn(Response.ok(dto).build());

		RecordingObserver<RecordSearchResult> observer = new RecordingObserver<>();
		service.searchRecords(SearchRecordsRequest.newBuilder().setPolicyStoreId("store-1").setBundleHash("").build(), observer);

		verify(rest).searchRecords("store-1", null, null, null, null, 0, 100);
		assertEquals(2, observer.values.get(0).getRecordsCount());
	}

	@Test
	void testSearchRecords_PassesExplicitValues() {
		when(rest.searchRecords(any(), any(), any(), any(), any(), eq(20), eq(5)))
				.thenReturn(Response.ok(new TraceRecordSearchResponse()).build());

		RecordingObserver<RecordSearchResult> observer = new RecordingObserver<>();
		service.searchRecords(SearchRecordsRequest.newBuilder().setPolicyStoreId("s").setPolicyStoreVersion("1")
				.setBundleHash("h").setReceivedFrom("2026-01-01T00:00:00Z").setReceivedTo("2026-02-01T00:00:00Z")
				.setStart(20).setCount(5).build(), observer);

		verify(rest).searchRecords("s", "1", "h", "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z", 20, 5);
		assertTrue(observer.completed);
	}

	@Test
	void testSearchRecords_ExplicitZeroCountIsNotReplacedByDefault() {
		when(rest.searchRecords(any(), any(), any(), any(), any(), eq(0), eq(0)))
				.thenReturn(Response.ok(new TraceRecordSearchResponse()).build());

		service.searchRecords(SearchRecordsRequest.newBuilder().setPolicyStoreId("s").setCount(0).build(),
				new RecordingObserver<>());

		verify(rest).searchRecords("s", null, null, null, null, 0, 0);
	}

	@Test
	void testGetExecution_MapsResultAndDefaults() throws Exception {
		TraceExecutionResponse dto = new TraceExecutionResponse();
		dto.setExecutionAuthority("spiffe://example.org/agent");
		dto.setTraceExecutionId("exec-1");
		dto.setRecords(List.of(record("r1")));
		when(rest.getExecution(eq("exec-1"), isNull(), eq(0), eq(100))).thenReturn(Response.ok(dto).build());

		RecordingObserver<ExecutionResult> observer = new RecordingObserver<>();
		service.getExecution(GetExecutionRequest.newBuilder().setTraceExecutionId("exec-1").build(), observer);

		ExecutionResult result = observer.values.get(0);
		assertEquals("exec-1", result.getTraceExecutionId());
		assertEquals("receipt_sequence", result.getOrdering());
		assertEquals("not_assessed", result.getCompleteness());
		assertEquals(1, result.getRecordsCount());
	}

	@Test
	void testRestError_IsTranslatedToGrpcStatusWithErrorBody() {
		when(rest.submitRecord(any(), isNull(), isNull())).thenThrow(new WebApplicationException(
				Response.status(Response.Status.CONFLICT).entity(ERROR_JSON).type(MediaType.APPLICATION_JSON_TYPE).build()));

		RecordingObserver<RecordAcceptance> observer = new RecordingObserver<>();
		service.submitRecord(SubmitRecordRequest.getDefaultInstance(), observer);

		assertFalse(observer.completed);
		assertTrue(observer.values.isEmpty());
		assertEquals(Status.Code.ALREADY_EXISTS, observer.errorStatus().getCode());
		assertEquals(ERROR_JSON, observer.errorStatus().getDescription());
	}

	@Test
	void testRestErrorStatuses_AreMapped() {
		assertEquals(Status.Code.NOT_FOUND, errorCodeOf(Response.Status.NOT_FOUND));
		assertEquals(Status.Code.INVALID_ARGUMENT, errorCodeOf(Response.Status.BAD_REQUEST));
		assertEquals(Status.Code.PERMISSION_DENIED, errorCodeOf(Response.Status.FORBIDDEN));
		assertEquals(Status.Code.INTERNAL, errorCodeOf(Response.Status.INTERNAL_SERVER_ERROR));
	}

	private Status.Code errorCodeOf(Response.Status status) {
		doThrow(new WebApplicationException(Response.status(status).entity(ERROR_JSON).build())).when(rest).getRecord(any(), any());
		RecordingObserver<RecordEnvelope> observer = new RecordingObserver<>();
		service.getRecord(GetRecordRequest.newBuilder().setRecordId("r").build(), observer);
		return observer.errorStatus().getCode();
	}

	@Test
	void testUnexpectedException_IsInternalWithoutLeakingDetails() {
		when(rest.getRecord(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));

		RecordingObserver<RecordEnvelope> observer = new RecordingObserver<>();
		service.getRecord(GetRecordRequest.newBuilder().setRecordId("r").build(), observer);

		assertEquals(Status.Code.INTERNAL, observer.errorStatus().getCode());
		assertEquals("Internal error", observer.errorStatus().getDescription());
		assertNull(observer.errorStatus().getCause());
	}

	@Test
	void testNonSuccessResponse_IsTranslatedToGrpcStatus() {
		when(rest.getRecord(any(), any())).thenReturn(Response.status(Response.Status.NOT_FOUND).entity(ERROR_JSON).build());

		RecordingObserver<RecordEnvelope> observer = new RecordingObserver<>();
		service.getRecord(GetRecordRequest.newBuilder().setRecordId("r").build(), observer);

		assertEquals(Status.Code.NOT_FOUND, observer.errorStatus().getCode());
		assertEquals(ERROR_JSON, observer.errorStatus().getDescription());
	}
}
