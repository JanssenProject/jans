/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.config;

import io.jans.lock.service.grpc.audit.GrpcAuditServiceImpl;
import io.grpc.servlet.GrpcServlet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;

/**
 * Configuration for RESTEasy gRPC Bridge.
 * This allows gRPC services to be exposed via HTTP/2 servlet endpoints.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class RestEasyGrpcBridgeConfiguration {

    @Inject
    private Logger log;

    @Inject
    private GrpcAuditServiceImpl grpcAuditService;

    /**
     * Initialize and register gRPC servlet for RESTEasy gRPC Bridge.
     *
     * @param servletContext the servlet context
     */
    public void initGrpcBridge(ServletContext servletContext) {
        log.info("Initializing RESTEasy gRPC Bridge");

        try {
            // Create GrpcServlet instance
            GrpcServlet grpcServlet = new GrpcServlet(null);
            
            // Register the servlet
            ServletRegistration.Dynamic registration = servletContext.addServlet("grpcServlet", grpcServlet);
            
            if (registration != null) {
                // Map to gRPC endpoint path
                registration.addMapping("/grpc/*");
                registration.setAsyncSupported(true);
                registration.setLoadOnStartup(1);
                
                // Set init parameters if needed
                registration.setInitParameter("grpc.service.package", "io.jans.lock.service.grpc");
                
                log.info("RESTEasy gRPC Bridge servlet registered successfully on /grpc/*");
            } else {
                log.warn("gRPC servlet was already registered");
            }
        } catch (Exception e) {
            log.error("Failed to initialize RESTEasy gRPC Bridge", e);
            throw new RuntimeException("Failed to initialize RESTEasy gRPC Bridge", e);
        }
    }
}