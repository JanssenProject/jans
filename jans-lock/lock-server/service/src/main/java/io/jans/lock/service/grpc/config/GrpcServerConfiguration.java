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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Configuration and lifecycle management for gRPC server.
 * 
 * Added: Netty-based TLS/ALPN support when enabled.
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

    // TLS/ALPN support (Netty-based)
    private boolean useTls = false;
    private String tlsCertChainFilePath; // PEM cert chain file
    private String tlsPrivateKeyFilePath; // PEM private key file

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
     * Uses NettyServerBuilder with SslContext when TLS is enabled, otherwise falls back to plain ServerBuilder.
     *
     * @throws IOException if server fails to start
     */
    private void startGrpcServer() throws IOException {
        if (useTls) {
            // Use Netty-based server with TLS/ALPN
            try {
                // Use shaded Netty classes bundled with gRPC
                io.grpc.netty.shaded.io.netty.handler.ssl.SslContext sslContext =
                        buildSslContext(tlsCertChainFilePath, tlsPrivateKeyFilePath);

                io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder nettyBuilder =
                        io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.forPort(grpcPort)
                                .sslContext(sslContext)
                                .addService(grpcAuditService)
                                .intercept(authorizationInterceptor);

                grpcServer = nettyBuilder.build().start();

                log.info("gRPC (Netty) server started on port {} with TLS/ALPN and authorization enabled", grpcPort);
            } catch (Exception e) {
                log.error("Failed to start Netty-based gRPC server with TLS", e);
                throw new IOException("Failed to start Netty-based gRPC server with TLS", e);
            }
        } else {
            grpcServer = ServerBuilder.forPort(grpcPort)
                    .addService(grpcAuditService)
                    .intercept(authorizationInterceptor)  // Add authorization interceptor
                    .build()
                    .start();

            log.info("gRPC server started on port {} with authorization enabled", grpcPort);
        }

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
     * Build SslContext for server from PEM cert chain and private key files.
     * Uses gRPC-shaded Netty's SslContextBuilder so ALPN is properly configured for HTTP/2.
     */
    private io.grpc.netty.shaded.io.netty.handler.ssl.SslContext buildSslContext(String certChainPath, String privateKeyPath) throws Exception {
        if (certChainPath == null || privateKeyPath == null) {
            throw new IllegalArgumentException("TLS is enabled but cert chain or private key path is not set");
        }

        File certChainFile = new File(certChainPath);
        File privateKeyFile = new File(privateKeyPath);

        if (!certChainFile.exists() || !privateKeyFile.exists()) {
            throw new IllegalArgumentException("TLS certificate chain or private key file not found");
        }

        io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder sslCtxBuilder =
                io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder.forServer(certChainFile, privateKeyFile);

        // Let Netty choose optimal SslProvider (OpenSSL/JDK) and enable ALPN for HTTP/2 automatically.
        return sslCtxBuilder.build();
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

    // TLS configuration setters/getters
    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setTlsCertChainFilePath(String tlsCertChainFilePath) {
        this.tlsCertChainFilePath = tlsCertChainFilePath;
    }

    public void setTlsPrivateKeyFilePath(String tlsPrivateKeyFilePath) {
        this.tlsPrivateKeyFilePath = tlsPrivateKeyFilePath;
    }

    public String getTlsCertChainFilePath() {
        return tlsCertChainFilePath;
    }

    public String getTlsPrivateKeyFilePath() {
        return tlsPrivateKeyFilePath;
    }
}
