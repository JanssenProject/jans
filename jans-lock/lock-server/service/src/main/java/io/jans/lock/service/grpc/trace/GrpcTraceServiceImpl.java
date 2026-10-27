/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import org.slf4j.Logger;

import io.grpc.stub.StreamObserver;
import io.jans.lock.model.trace.api.TraceAcceptanceResponse;
import io.jans.lock.model.trace.api.TraceBulkIngestionResponse;
import io.jans.lock.model.trace.api.TraceExecutionResponse;
import io.jans.lock.model.trace.api.TraceRecordResponse;
import io.jans.lock.model.trace.api.TraceRecordSearchResponse;
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
import io.jans.lock.model.trace.grpc.TraceServiceGrpc;
import io.jans.lock.service.ws.rs.trace.TraceRestWebService;

/**
 * gRPC service implementation for TRACE record ingestion and retrieval.
 * This service acts as a bridge between gRPC and the TRACE REST endpoints.
 *
 * Note: This class is NOT a CDI bean because the base class TraceServiceImplBase
 * contains final methods which are not proxyable by Weld CDI.
 * Instead, it's created by GrpcTraceServiceProvider.
 *
 * @author Yuriy Movchan
 */
public class GrpcTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

    static final int DEFAULT_START = 0;

    static final int DEFAULT_COUNT = 100;

    private final Logger log;
    private final TraceRestWebService traceRestWebService;
    private final GrpcTraceMapper mapper;

    /**
     * Constructor for manual instantiation (not CDI).
     *
     * @param traceRestWebService REST service to delegate to
     * @param mapper mapper between proto messages and REST DTOs
     * @param log logger instance
     */
    public GrpcTraceServiceImpl(TraceRestWebService traceRestWebService, GrpcTraceMapper mapper, Logger log) {
        this.traceRestWebService = traceRestWebService;
        this.mapper = mapper;
        this.log = log;
    }

    @Override
    public void submitRecord(SubmitRecordRequest request, StreamObserver<RecordAcceptance> responseObserver) {
        GrpcTraceCall.execute(log, "submitRecord", responseObserver,
                () -> traceRestWebService.submitRecord(request.getAssertion().newInput(), null, null),
                TraceAcceptanceResponse.class, mapper::toRecordAcceptance);
    }

    @Override
    public void submitBulkRecords(SubmitBulkRecordsRequest request, StreamObserver<BulkAcceptance> responseObserver) {
        GrpcTraceCall.execute(log, "submitBulkRecords", responseObserver,
                () -> traceRestWebService.submitBulkRecords(request.getRecords().newInput(), null, null),
                TraceBulkIngestionResponse.class, mapper::toBulkAcceptance);
    }

    @Override
    public void getRecord(GetRecordRequest request, StreamObserver<RecordEnvelope> responseObserver) {
        GrpcTraceCall.execute(log, "getRecord", responseObserver,
                () -> traceRestWebService.getRecord(request.getRecordId(),
                        mapper.optionalString(request.hasProducerId(), request.getProducerId())),
                TraceRecordResponse.class, mapper::toRecordEnvelope);
    }

    @Override
    public void searchRecords(SearchRecordsRequest request, StreamObserver<RecordSearchResult> responseObserver) {
        GrpcTraceCall.execute(log, "searchRecords", responseObserver,
                () -> traceRestWebService.searchRecords(
                        mapper.optionalString(request.hasPolicyStoreId(), request.getPolicyStoreId()),
                        mapper.optionalString(request.hasPolicyStoreVersion(), request.getPolicyStoreVersion()),
                        mapper.optionalString(request.hasBundleHash(), request.getBundleHash()),
                        mapper.optionalString(request.hasReceivedFrom(), request.getReceivedFrom()),
                        mapper.optionalString(request.hasReceivedTo(), request.getReceivedTo()),
                        request.hasStart() ? request.getStart() : DEFAULT_START,
                        request.hasCount() ? request.getCount() : DEFAULT_COUNT),
                TraceRecordSearchResponse.class, mapper::toRecordSearchResult);
    }

    @Override
    public void getExecution(GetExecutionRequest request, StreamObserver<ExecutionResult> responseObserver) {
        GrpcTraceCall.execute(log, "getExecution", responseObserver,
                () -> traceRestWebService.getExecution(request.getTraceExecutionId(),
                        mapper.optionalString(request.hasExecutionAuthority(), request.getExecutionAuthority()),
                        request.hasStart() ? request.getStart() : DEFAULT_START,
                        request.hasCount() ? request.getCount() : DEFAULT_COUNT),
                TraceExecutionResponse.class, mapper::toExecutionResult);
    }
}
