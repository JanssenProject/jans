/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.audit;

import org.slf4j.Logger;

import io.jans.lock.service.ws.rs.audit.AuditRestWebService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provider/Factory bean for GrpcAuditServiceImpl.
 * 
 * This is a workaround for Weld CDI proxy issue with gRPC generated classes
 * that contain final methods. Instead of making GrpcAuditServiceImpl a CDI bean,
 * we create it in this factory and expose it as ApplicationScoped singleton.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcAuditServiceProvider {

    @Inject
    private Logger log;

    @Inject
    private AuditRestWebService auditRestWebService;

    @Inject
    private GrpcToJavaMapper mapper;

    private GrpcAuditServiceImpl grpcAuditService;

    @PostConstruct
    public void init() {
        log.debug("Initializing GrpcAuditServiceProvider");
        
        // Create the gRPC service implementation
        // This avoids CDI trying to proxy the class with final methods
        grpcAuditService = new GrpcAuditServiceImpl(auditRestWebService, mapper, log);
        
        log.info("GrpcAuditServiceImpl created successfully");
    }

    /**
     * Get the gRPC audit service instance.
     * 
     * @return GrpcAuditServiceImpl instance
     */
    public GrpcAuditServiceImpl getService() {
        return grpcAuditService;
    }
}
