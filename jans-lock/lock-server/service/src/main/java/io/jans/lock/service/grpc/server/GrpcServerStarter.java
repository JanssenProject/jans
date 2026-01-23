/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */
package io.jans.lock.service.grpc.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.GrpcServerMode;
import io.jans.lock.model.config.grpc.GrpcConfiguration;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceProvider;
import io.jans.lock.service.grpc.security.GrpcAuthorizationInterceptor;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Configuration and lifecycle management for gRPC server.
 * 
 * Added: Netty-based TLS/ALPN support when enabled.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcServerStarter {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private GrpcAuditServiceProvider grpcAuditServiceProvider;

    @Inject
    private GrpcAuthorizationInterceptor authorizationInterceptor;

    private Server grpcServer;

    public void initGrpcServer() {
        try {
        	if (grpcServer == null) {
        		startGrpcServer();
        	} else {
				log.warn("gRPC server is already started");
			}
        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
        }
    }

    /**
     * Start the gRPC server with authorization interceptor.
     * Uses NettyServerBuilder with SslContext when TLS is enabled, otherwise falls back to plain ServerBuilder.
     *
     * @throws IOException if server fails to start
     */
    private void startGrpcServer() throws IOException {
    	GrpcConfiguration grpcConfiguration = appConfiguration.getGrpcConfiguration();
    	if (grpcConfiguration == null || grpcConfiguration.getServerMode() == null ||
    			!(GrpcServerMode.PLAIN_SERVER == grpcConfiguration.getServerMode() || GrpcServerMode.TLS_SERVER == grpcConfiguration.getServerMode())) {
			log.info("gRPC inproc server was disabled in configuration");
			return;
		}

    	if (GrpcServerMode.TLS_SERVER == grpcConfiguration.getServerMode()) {
            // Use Netty-based server with TLS/ALPN
            try {
                // Use shaded Netty classes bundled with gRPC
                io.grpc.netty.shaded.io.netty.handler.ssl.SslContext sslContext =
                        buildSslContext(grpcConfiguration.getTlsCertChainFilePath(), grpcConfiguration.getTlsPrivateKeyFilePath());

                io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder nettyBuilder =
                        io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.forPort(grpcConfiguration.getGrpcPort())
                                .sslContext(sslContext)
                                .addService(grpcAuditServiceProvider.getService())  // Get service from provider
                                .intercept(authorizationInterceptor);  // Add authorization interceptor

                grpcServer = nettyBuilder.build().start();

                log.info("gRPC (Netty) server started on port {} with TLS/ALPN and authorization enabled", grpcConfiguration.getGrpcPort());
            } catch (SSLException e) {
                log.error("Failed to start Netty-based gRPC server with TLS", e);
                throw new IOException("Failed to start Netty-based gRPC server with TLS", e);
            }
        } else {
            grpcServer = ServerBuilder.forPort(grpcConfiguration.getGrpcPort())
                    .addService(grpcAuditServiceProvider.getService())  // Get service from provider
                    .intercept(authorizationInterceptor)  // Add authorization interceptor
                    .build()
                    .start();

            log.info("gRPC server started on port {} with authorization enabled", grpcConfiguration.getGrpcPort());
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server due to JVM shutdown");
            stopGrpcServer();
		}));
    }

    /**
     * Build SslContext for server from PEM cert chain and private key files.
     * Uses gRPC-shaded Netty's SslContextBuilder so ALPN is properly configured for HTTP/2.
     * @throws SSLException 
     */
    private io.grpc.netty.shaded.io.netty.handler.ssl.SslContext buildSslContext(String certChainPath, String privateKeyPath) throws SSLException {
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
    public void stopGrpcServer() {
        if (grpcServer != null) {
            log.info("Stopping gRPC server...");
            grpcServer.shutdown();
            
            try {
                if (!grpcServer.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("gRPC server did not terminate gracefully, forcing shutdown");
                    grpcServer.shutdownNow();
                    
                    if (!grpcServer.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.error("gRPC server did not terminate");
                    }
                }
                
                log.info("gRPC server stopped");
            } catch (InterruptedException e) {
                log.error("gRPC server shutdown was interrupted", e);
                // Restore interrupt status
                Thread.currentThread().interrupt();
                // Force shutdown
                grpcServer.shutdownNow();
            }
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

}
