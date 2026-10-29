/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.server;

import java.util.List;

import io.grpc.BindableService;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceProvider;
import io.jans.lock.service.grpc.trace.GrpcTraceServiceProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Single list of the gRPC services Lock exposes, shared by the plain, TLS and servlet bridge
 * servers so that a service cannot be registered on one of them only.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcServiceRegistry {

    @Inject
    private GrpcAuditServiceProvider grpcAuditServiceProvider;

    @Inject
    private GrpcTraceServiceProvider grpcTraceServiceProvider;

    /**
     * @return the audit, TRACE and TRACE admin gRPC services
     */
    public List<BindableService> getServices() {
        return List.of(grpcAuditServiceProvider.getService(), grpcTraceServiceProvider.getTraceService(),
                grpcTraceServiceProvider.getTraceAdminService());
    }
}
