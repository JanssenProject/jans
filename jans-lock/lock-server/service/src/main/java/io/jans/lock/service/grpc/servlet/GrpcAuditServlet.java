/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.lock.service.grpc.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.servlet.jakarta.ServletAdapter;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.GrpcServerMode;
import io.jans.lock.model.config.grpc.GrpcConfiguration;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceProvider;
import io.jans.lock.service.grpc.security.GrpcAuthorizationInterceptor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * gRPC servlet for gRPC bridge
 *  
 * Added: Netty-based TLS/ALPN support when enabled.
 * 
 * @author Yuriy Movchan
 */
@WebServlet(
    name = "GrpcAuditServlet",
    urlPatterns = {"/"},               // if you want gRPC at the root — ok, but make sure REST endpoints use more specific paths
    asyncSupported = true,
    loadOnStartup = 10
)
public class GrpcAuditServlet extends HttpServlet {

    private static final long serialVersionUID = -5675524890589330190L;

	@Inject
	private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private GrpcAuditServiceProvider grpcAuditServiceProvider;

    @Inject
    private GrpcAuthorizationInterceptor authorizationInterceptor;

    private volatile ServletAdapter adapter = null;

    public GrpcAuditServlet() {
    }

    @PostConstruct
    public void initializeGrpc() {
    	log.info("gRPC adapter initializion");
    	GrpcConfiguration grpcConfiguration = appConfiguration.getGrpcConfiguration();
    	if (grpcConfiguration == null || grpcConfiguration.getServerMode() == null || GrpcServerMode.BRIDGE != grpcConfiguration.getServerMode()) {
			log.info("gRPC server bridge was disabled in configuration");
			return;
		}

        try {
            BindableService rawService = grpcAuditServiceProvider.getService();
            if (rawService == null) {
                throw new ServletException("GrpcAuditServiceProvider returned null service");
            }

			// Wrap the service with the authorization interceptor
            ServerServiceDefinition wrapped = ServerInterceptors.intercept(rawService, authorizationInterceptor);

            ServletServerBuilder builder = new ServletServerBuilder();
            builder.addService(wrapped);

            builder.maxInboundMessageSize(10 * 1024 * 1024);
            // builder.maxInboundMetadataSize(8192);                    // if you have a lot of metadata
            // builder.executor(Executors.newCachedThreadPool());      // custom executor if needed (default is ForkJoinPool)

            this.adapter = builder.buildServletAdapter();

            log.info("gRPC adapter initialized successfully with authorization enabled for service: " +
                        rawService.getClass().getSimpleName());
        } catch (Exception e) {
        	log.error("Failed to initialize gRPC servlet", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletAdapter localAdapter = this.adapter;
        if (localAdapter != null) {
        	HttpServletRequest wrappedRequest = new HeaderNormalizingRequestWrapper(req);
            localAdapter.doPost(wrappedRequest, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "gRPC not initialized yet");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletAdapter localAdapter = this.adapter;
        if (localAdapter != null) {
        	HttpServletRequest wrappedRequest = new HeaderNormalizingRequestWrapper(req);
            localAdapter.doGet(wrappedRequest, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "gRPC not initialized yet");
        }
    }

    @Override
    public void destroy() {
        ServletAdapter localAdapter = this.adapter;
        if (localAdapter != null) {
            try {
                localAdapter.destroy();
                log.info("gRPC adapter destroyed");
            } catch (Exception e) {
            	log.warn("Error during gRPC adapter destroy", e);
            }
        }
        super.destroy();
    }
    /**
     * Wrapper for headers normalization to lower case
     */
    private static class HeaderNormalizingRequestWrapper extends HttpServletRequestWrapper {
        
        private final Map<String, String> normalizedHeaders;
        
        public HeaderNormalizingRequestWrapper(HttpServletRequest request) {
            super(request);
            this.normalizedHeaders = new HashMap<>();
            
            // Collect all header names and normalize them
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String originalName = headerNames.nextElement();
                String normalizedName = originalName.toLowerCase();
                normalizedHeaders.put(normalizedName, originalName);
            }
        }
        
        @Override
        public String getHeader(String name) {
            String originalName = normalizedHeaders.get(name.toLowerCase());
            return super.getHeader(originalName != null ? originalName : name);
        }
        
        @Override
        public Enumeration<String> getHeaders(String name) {
            String originalName = normalizedHeaders.get(name.toLowerCase());
            return super.getHeaders(originalName != null ? originalName : name);
        }
        
        @Override
        public Enumeration<String> getHeaderNames() {
            // Возвращаем все имена в lowercase
            return Collections.enumeration(normalizedHeaders.keySet());
        }
        
        @Override
        public long getDateHeader(String name) {
            String originalName = normalizedHeaders.get(name.toLowerCase());
            return super.getDateHeader(originalName != null ? originalName : name);
        }
        
        @Override
        public int getIntHeader(String name) {
            String originalName = normalizedHeaders.get(name.toLowerCase());
            return super.getIntHeader(originalName != null ? originalName : name);
        }
    }
}