/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.audit;

import io.grpc.stub.StreamObserver;
import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import io.jans.lock.model.audit.grpc.*;
import io.jans.lock.service.ws.rs.audit.AuditRestWebService;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC service implementation for Audit operations.
 * This service acts as a bridge between gRPC and REST endpoints.
 * 
 * Note: This class is NOT a CDI bean because the base class AuditServiceImplBase
 * contains final methods which are not proxyable by Weld CDI.
 * Instead, it's created by GrpcAuditServiceProvider.
 * 
 * @author Yuriy Movchan
 */
public class GrpcAuditServiceImpl extends AuditServiceGrpc.AuditServiceImplBase {

    private final Logger log;
    private final AuditRestWebService auditRestWebService;
    private final GrpcToJavaMapper mapper;

    /**
     * Constructor for manual instantiation (not CDI).
     * 
     * @param auditRestWebService REST service to delegate to
     * @param mapper mapper for proto to Java conversion
     * @param log logger instance
     */
    public GrpcAuditServiceImpl(
            AuditRestWebService auditRestWebService,
            GrpcToJavaMapper mapper,
            Logger log) {
        this.auditRestWebService = auditRestWebService;
        this.mapper = mapper;
        this.log = log;
    }

    @Override
    public void processHealth(io.jans.lock.model.audit.grpc.HealthRequest request,
                               StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processHealth called");

        try {
            HealthEntry healthEntry = mapper.toHealthEntry(request.getEntry());
            Response restResponse = auditRestWebService.processHealthRequest(healthEntry, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing health request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void processBulkHealth(BulkHealthRequest request,
                                   StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processBulkHealth called with {} entries", request.getEntriesCount());

        try {
            List<HealthEntry> healthEntries = new ArrayList<>();
            for (io.jans.lock.model.audit.grpc.HealthEntry grpcEntry : request.getEntriesList()) {
                healthEntries.add(mapper.toHealthEntry(grpcEntry));
            }

            Response restResponse = auditRestWebService.processBulkHealthRequest(healthEntries, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing bulk health request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void processLog(io.jans.lock.model.audit.grpc.LogRequest request,
                           StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processLog called");

        try {
            LogEntry logEntry = mapper.toLogEntry(request.getEntry());
            Response restResponse = auditRestWebService.processLogRequest(logEntry, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing log request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void processBulkLog(BulkLogRequest request,
                                StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processBulkLog called with {} entries", request.getEntriesCount());

        try {
            List<LogEntry> logEntries = new ArrayList<>();
            for (io.jans.lock.model.audit.grpc.LogEntry grpcEntry : request.getEntriesList()) {
                logEntries.add(mapper.toLogEntry(grpcEntry));
            }

            Response restResponse = auditRestWebService.processBulkLogRequest(logEntries, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing bulk log request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void processTelemetry(io.jans.lock.model.audit.grpc.TelemetryRequest request,
                                  StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processTelemetry called");

        try {
            TelemetryEntry telemetryEntry = mapper.toTelemetryEntry(request.getEntry());
            Response restResponse = auditRestWebService.processTelemetryRequest(telemetryEntry, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing telemetry request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void processBulkTelemetry(BulkTelemetryRequest request,
                                      StreamObserver<AuditResponse> responseObserver) {
        log.info("gRPC processBulkTelemetry called with {} entries", request.getEntriesCount());

        try {
            List<TelemetryEntry> telemetryEntries = new ArrayList<>();
            for (io.jans.lock.model.audit.grpc.TelemetryEntry grpcEntry : request.getEntriesList()) {
                telemetryEntries.add(mapper.toTelemetryEntry(grpcEntry));
            }

            Response restResponse = auditRestWebService.processBulkTelemetryRequest(telemetryEntries, null, null);

            AuditResponse grpcResponse = buildAuditResponse(restResponse);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing bulk telemetry request", e);
            responseObserver.onError(e);
        }
    }

    /**
     * Build gRPC AuditResponse from JAX-RS Response.
     *
     * @param restResponse the JAX-RS response
     * @return the gRPC AuditResponse
     */
    private AuditResponse buildAuditResponse(Response restResponse) {
        boolean success = restResponse.getStatus() >= 200 && restResponse.getStatus() < 300;
        String message = restResponse.getEntity() != null ? restResponse.getEntity().toString() : "";

        return AuditResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();
    }
}