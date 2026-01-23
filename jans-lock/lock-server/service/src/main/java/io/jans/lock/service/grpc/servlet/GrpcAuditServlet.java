/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */
package io.jans.lock.service.grpc.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    // Make it static final and use volatile for safe publication
    private static volatile ServletAdapter adapter = null;

    public GrpcAuditServlet() {
    }

    @PostConstruct
    public void initializeGrpc() {
        log.info("gRPC adapter initialization");
        GrpcConfiguration grpcConfiguration = appConfiguration.getGrpcConfiguration();
        if (grpcConfiguration == null || grpcConfiguration.getServerMode() == null || 
            GrpcServerMode.BRIDGE != grpcConfiguration.getServerMode()) {
            log.info("gRPC server bridge was disabled in configuration");
            return;
        }

        // Use double-checked locking for thread-safety
        if (adapter == null) {
            synchronized (GrpcAuditServlet.class) {
                if (adapter == null) {
                    try {
                        BindableService rawService = grpcAuditServiceProvider.getService();
                        if (rawService == null) {
                            log.error("GrpcAuditServiceProvider returned null service");
                            return;
                        }

                        ServerServiceDefinition wrapped = ServerInterceptors.intercept(rawService, authorizationInterceptor);

                        ServletServerBuilder builder = new ServletServerBuilder();
                        builder.addService(wrapped);
                        builder.maxInboundMessageSize(10 * 1024 * 1024);

                        adapter = builder.buildServletAdapter();

                        log.info("gRPC adapter initialized successfully with authorization enabled for service: " +
                                rawService.getClass().getSimpleName());
                    } catch (Exception e) {
                        log.error("Failed to initialize gRPC servlet", e);
                    }
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletAdapter localAdapter = adapter;
        if (localAdapter != null) {
            HttpServletRequest wrappedRequest = new GrpcRequestWrapper(req);
            localAdapter.doPost(wrappedRequest, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "gRPC not initialized yet");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletAdapter localAdapter = adapter;
        if (localAdapter != null) {
            HttpServletRequest wrappedRequest = new GrpcRequestWrapper(req);
            localAdapter.doGet(wrappedRequest, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "gRPC not initialized yet");
        }
    }

    @Override
    public void destroy() {
        ServletAdapter localAdapter = adapter;
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
	 * /** Wrapper for gRPC requests: 1. Normalizes headers to lowercase 2. Removes
	 * context path from URI for gRPC bridge
	 */
	private static class GrpcRequestWrapper extends HttpServletRequestWrapper {

		private final Map<String, List<String>> headers;
		private final String requestURI;
		private final String servletPath;

		public GrpcRequestWrapper(HttpServletRequest request) {
			super(request);

			// 1. Normalize headers
			this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String name = headerNames.nextElement();
				List<String> values = Collections.list(request.getHeaders(name));
				headers.put(name.toLowerCase(), values);
			}

			// 2. Remove context path from URI
			// Was: /jans-lock/io.jans.lock.audit.AuditService/ProcessLog
			// Becomes: /io.jans.lock.audit.AuditService/ProcessLog
			String originalURI = request.getRequestURI();
			String contextPath = request.getContextPath();

			if (contextPath != null && !contextPath.isEmpty() && originalURI.startsWith(contextPath)) {
				this.requestURI = originalURI.substring(contextPath.length());
				this.servletPath = this.requestURI;
			} else {
				this.requestURI = originalURI;
				this.servletPath = request.getServletPath();
			}
		}

		// === Override methods for paths ===

		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public String getServletPath() {
			return servletPath;
		}

		@Override
		public String getPathInfo() {
			// gRPC usually uses requestURI, but just in case
			return null;
		}

		@Override
		public String getContextPath() {
			// Return empty context path for gRPC
			return "";
		}

		@Override
		public StringBuffer getRequestURL() {
			StringBuffer url = new StringBuffer();
			url.append(getScheme()).append("://").append(getServerName());

			int port = getServerPort();
			if ((getScheme().equals("http") && port != 80) || (getScheme().equals("https") && port != 443)) {
				url.append(':').append(port);
			}

			url.append(getRequestURI());
			return url;
		}

		// === Override methods for headers ===

		@Override
		public String getHeader(String name) {
			List<String> values = headers.get(name.toLowerCase());
			return values != null && !values.isEmpty() ? values.get(0) : null;
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> values = headers.get(name.toLowerCase());
			return values != null ? Collections.enumeration(values) : Collections.enumeration(Collections.emptyList());
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(headers.keySet());
		}

		@Override
		public long getDateHeader(String name) {
			String value = getHeader(name);
			if (value == null) {
				return -1L;
			}
			try {
				return super.getDateHeader(name);
			} catch (IllegalArgumentException e) {
				return -1L;
			}
		}

		@Override
		public int getIntHeader(String name) {
			String value = getHeader(name);
			if (value == null) {
				return -1;
			}
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
	}
}