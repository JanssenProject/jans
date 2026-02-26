/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.servlet.http.HttpServletRequest;

/**
 * gRPC bridge for HttpServletRequest exclusion from CDI processing
 * 
 * @author Yuriy Movchan
 */
public class GrpcBridgeExclusionExtension implements Extension {
	
    private static final Logger log = LoggerFactory.getLogger(GrpcBridgeExclusionExtension.class.getName());

    public <T extends HttpServletRequest> void excludeGrpcBridge(@Observes ProcessAnnotatedType<T> pat) {
        
        String className = pat.getAnnotatedType().getJavaClass().getName();
        
        if (className.contains("grpc") && className.contains("HttpServletRequest")) {
            log.info("Vetoing gRPC HttpServletRequest: {}", className);
            pat.veto();
        }
    }
}