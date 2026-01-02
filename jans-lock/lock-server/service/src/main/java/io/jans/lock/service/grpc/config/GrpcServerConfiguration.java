/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceImpl;
import io.jans.lock.service.grpc.security.GrpcAuthorizationInterceptor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Configuration and lifecycle management for gRPC server.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcServerConfiguration {

    @Inject
    private Logger log;

    @Inject
    private GrpcAuditServiceImpl grpcAuditService;

    @Inject
    private GrpcAuthorizationInterceptor authorizationInterceptor;

    private Server grpcServer;
    private int grpcPort = 50051; // Default gRPC port

    @PostConstruct
    public void init() {
        try {
            startGrpcServer();
        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
            throw new RuntimeException("Failed to start gRPC server", e);
        }
    }

    /**
     * Start the gRPC server with authorization interceptor.
     *
     * @throws IOException if server fails to start
     */
    private void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(grpcPort)
                .addService(grpcAuditService)
                .intercept(authorizationInterceptor)  // Add authorization interceptor
                .build()
                .start();

        log.info("gRPC server started on port {} with authorization enabled", grpcPort);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server due to JVM shutdown");
            try {
                stopGrpcServer();
            } catch (InterruptedException e) {
                log.error("Error during gRPC server shutdown", e);
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * Stop the gRPC server.
     *
     * @throws InterruptedException if shutdown is interrupted
     */
    @PreDestroy
    public void stopGrpcServer() throws InterruptedException {
        if (grpcServer != null) {
            log.info("Stopping gRPC server...");
            grpcServer.shutdown();
            
            if (!grpcServer.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("gRPC server did not terminate gracefully, forcing shutdown");
                grpcServer.shutdownNow();
                
                if (!grpcServer.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("gRPC server did not terminate");
                }
            }
            
            log.info("gRPC server stopped");
        }
    }

    /**
     * Block until the server shuts down.
     *
     * @throws InterruptedException if waiting is interrupted
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }

    public void setGrpcPort(int port) {
        this.grpcPort = port;
    }

    public int getGrpcPort() {
        return grpcPort;
    }
}