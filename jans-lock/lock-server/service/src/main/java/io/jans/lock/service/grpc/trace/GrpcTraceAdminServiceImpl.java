/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import org.slf4j.Logger;

import io.grpc.stub.StreamObserver;
import io.jans.lock.model.trace.api.ProducerChainListResponse;
import io.jans.lock.model.trace.api.ProducerChainResponse;
import io.jans.lock.model.trace.api.ProducerKeyListResponse;
import io.jans.lock.model.trace.api.ProducerKeyResponse;
import io.jans.lock.model.trace.grpc.ListProducerChainsRequest;
import io.jans.lock.model.trace.grpc.ListProducerKeysRequest;
import io.jans.lock.model.trace.grpc.ProducerChain;
import io.jans.lock.model.trace.grpc.ProducerChainList;
import io.jans.lock.model.trace.grpc.ProducerKey;
import io.jans.lock.model.trace.grpc.ProducerKeyList;
import io.jans.lock.model.trace.grpc.RegisterProducerChainRequest;
import io.jans.lock.model.trace.grpc.RegisterProducerKeyRequest;
import io.jans.lock.model.trace.grpc.RevokeProducerKeyRequest;
import io.jans.lock.model.trace.grpc.TraceAdminServiceGrpc;
import io.jans.lock.service.ws.rs.trace.TraceAdminRestWebService;

/**
 * gRPC service implementation for TRACE producer-key and producer-chain administration.
 * This service acts as a bridge between gRPC and the TRACE admin REST endpoints.
 *
 * Note: This class is NOT a CDI bean because the base class TraceAdminServiceImplBase
 * contains final methods which are not proxyable by Weld CDI.
 * Instead, it's created by GrpcTraceServiceProvider.
 *
 * @author Yuriy Movchan
 */
public class GrpcTraceAdminServiceImpl extends TraceAdminServiceGrpc.TraceAdminServiceImplBase {

    private final Logger log;
    private final TraceAdminRestWebService traceAdminRestWebService;
    private final GrpcTraceMapper mapper;

    /**
     * Constructor for manual instantiation (not CDI).
     *
     * @param traceAdminRestWebService REST service to delegate to
     * @param mapper mapper between proto messages and REST DTOs
     * @param log logger instance
     */
    public GrpcTraceAdminServiceImpl(TraceAdminRestWebService traceAdminRestWebService, GrpcTraceMapper mapper,
            Logger log) {
        this.traceAdminRestWebService = traceAdminRestWebService;
        this.mapper = mapper;
        this.log = log;
    }

    @Override
    public void registerProducerKey(RegisterProducerKeyRequest request, StreamObserver<ProducerKey> responseObserver) {
        GrpcTraceCall.execute(log, "registerProducerKey", responseObserver,
                () -> traceAdminRestWebService.registerProducerKey(mapper.toKeyRegistrationRequest(request)),
                ProducerKeyResponse.class, mapper::toProducerKey);
    }

    @Override
    public void listProducerKeys(ListProducerKeysRequest request, StreamObserver<ProducerKeyList> responseObserver) {
        GrpcTraceCall.execute(log, "listProducerKeys", responseObserver,
                () -> traceAdminRestWebService.listProducerKeys(
                        mapper.optionalString(request.hasProducerId(), request.getProducerId())),
                ProducerKeyListResponse.class, mapper::toProducerKeyList);
    }

    @Override
    public void revokeProducerKey(RevokeProducerKeyRequest request, StreamObserver<ProducerKey> responseObserver) {
        GrpcTraceCall.execute(log, "revokeProducerKey", responseObserver,
                () -> traceAdminRestWebService.revokeProducerKey(mapper.toKeyRevokeRequest(request)),
                ProducerKeyResponse.class, mapper::toProducerKey);
    }

    @Override
    public void registerProducerChain(RegisterProducerChainRequest request,
            StreamObserver<ProducerChain> responseObserver) {
        GrpcTraceCall.execute(log, "registerProducerChain", responseObserver,
                () -> traceAdminRestWebService.registerProducerChain(mapper.toChainRegistrationRequest(request)),
                ProducerChainResponse.class, mapper::toProducerChain);
    }

    @Override
    public void listProducerChains(ListProducerChainsRequest request,
            StreamObserver<ProducerChainList> responseObserver) {
        GrpcTraceCall.execute(log, "listProducerChains", responseObserver,
                () -> traceAdminRestWebService.listProducerChains(
                        mapper.optionalString(request.hasProducerId(), request.getProducerId())),
                ProducerChainListResponse.class, mapper::toProducerChainList);
    }
}
