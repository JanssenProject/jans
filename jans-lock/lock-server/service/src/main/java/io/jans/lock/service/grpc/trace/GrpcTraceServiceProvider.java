/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.grpc.trace;

import org.slf4j.Logger;

import io.jans.lock.service.ws.rs.trace.TraceAdminRestWebService;
import io.jans.lock.service.ws.rs.trace.TraceRestWebService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provider/Factory bean for GrpcTraceServiceImpl and GrpcTraceAdminServiceImpl.
 *
 * This is a workaround for Weld CDI proxy issue with gRPC generated classes
 * that contain final methods. Instead of making the gRPC services CDI beans,
 * we create them in this factory and expose them as ApplicationScoped singletons.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcTraceServiceProvider {

    @Inject
    private Logger log;

    @Inject
    private TraceRestWebService traceRestWebService;

    @Inject
    private TraceAdminRestWebService traceAdminRestWebService;

    @Inject
    private GrpcTraceMapper mapper;

    private GrpcTraceServiceImpl grpcTraceService;

    private GrpcTraceAdminServiceImpl grpcTraceAdminService;

    @PostConstruct
    public void init() {
        log.debug("Initializing GrpcTraceServiceProvider");

        grpcTraceService = new GrpcTraceServiceImpl(traceRestWebService, mapper, log);
        grpcTraceAdminService = new GrpcTraceAdminServiceImpl(traceAdminRestWebService, mapper, log);

        log.info("GrpcTraceServiceImpl and GrpcTraceAdminServiceImpl created successfully");
    }

    /**
     * Get the gRPC TRACE service instance.
     *
     * @return GrpcTraceServiceImpl instance
     */
    public GrpcTraceServiceImpl getTraceService() {
        return grpcTraceService;
    }

    /**
     * Get the gRPC TRACE admin service instance.
     *
     * @return GrpcTraceAdminServiceImpl instance
     */
    public GrpcTraceAdminServiceImpl getTraceAdminService() {
        return grpcTraceAdminService;
    }
}
