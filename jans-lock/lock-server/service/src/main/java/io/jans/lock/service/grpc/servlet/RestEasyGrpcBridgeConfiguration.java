/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */
package io.jans.lock.service.grpc.servlet;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import io.grpc.servlet.jakarta.GrpcServlet;
import io.jans.lock.service.grpc.audit.GrpcAuditServiceProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;

@ApplicationScoped
@WebListener
public class RestEasyGrpcBridgeConfiguration implements ServletContextListener {

    @Inject
    private Logger log;

    @Inject
    private GrpcAuditServiceProvider grpcAuditServiceProvider;

    /**
     * Initialize and register gRPC servlet for RESTEasy gRPC Bridge.
     *
     * @param servletContext the servlet context
     */
    public void initGrpcBridge(ServletContext servletContext) {
        log.info("Initializing RESTEasy gRPC Bridge");

        try {
            // If a servlet with this name is already registered, don't try to register again
            if (servletContext.getServletRegistration("grpcServlet") != null) {
                log.warn("gRPC servlet already registered; skipping registration");
                return;
            }

            GrpcServlet grpcServlet = new GrpcServlet(ImmutableList.of(grpcAuditServiceProvider.getService()));

            // Register as a servlet
            ServletRegistration.Dynamic registration = servletContext.addServlet("grpcServlet", grpcServlet);

            if (registration != null) {
                // Map to gRPC endpoint path
                registration.addMapping("/grpc/*");
                registration.setAsyncSupported(true);
                registration.setLoadOnStartup(1);

                // Set async timeout (optional, in milliseconds)
                registration.setInitParameter("async-timeout", "30000");

                // For better performance with HTTP/2 in some servlet containers (container must support it)
                registration.setInitParameter("h2c", "true");

                // Set max message size (optional)
                registration.setInitParameter("maxInboundMessageSize", "4194304"); // 4MB

                log.info("RESTEasy gRPC Bridge servlet registered successfully on /grpc/*");
            } else {
                log.warn("Failed to add gRPC servlet: addServlet returned null (might be already registered)");
            }
        } catch (Exception e) {
            log.error("Failed to initialize RESTEasy gRPC Bridge", e);
            throw new RuntimeException("Failed to initialize RESTEasy gRPC Bridge", e);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            initGrpcBridge(sce.getServletContext());
        } catch (Exception e) {
            log.error("Error during gRPC bridge initialization in contextInitialized", e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nothing to do on shutdown for now
    }

}