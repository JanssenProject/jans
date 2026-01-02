/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.config;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * ServletContainerInitializer to bootstrap gRPC bridge integration.
 * This initializer is automatically discovered and invoked by the servlet container.
 * 
 * @author Yuriy Movchan
 */
@HandlesTypes({})
public class GrpcServletContextInitializer implements ServletContainerInitializer {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GrpcServletContextInitializer.class);

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        log.info("Initializing gRPC Bridge via ServletContainerInitializer");

        try {
            // Get RestEasyGrpcBridgeConfiguration bean from CDI container
            RestEasyGrpcBridgeConfiguration bridgeConfig = CDI.current()
                    .select(RestEasyGrpcBridgeConfiguration.class)
                    .get();

            // Initialize gRPC bridge
            bridgeConfig.initGrpcBridge(ctx);

            log.info("gRPC Bridge initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize gRPC Bridge", e);
            throw new ServletException("Failed to initialize gRPC Bridge", e);
        }
    }
}