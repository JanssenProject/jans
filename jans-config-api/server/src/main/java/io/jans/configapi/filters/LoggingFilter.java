/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Provider
public class LoggingFilter implements ContainerRequestFilter {
    
    private static Logger logger = LoggerFactory.getLogger("audit");

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    public void filter(ContainerRequestContext context) {
        logger.info("***********************************************************************");
        logger.info(
                "****Request " + context.getMethod() + " " + info.getPath() + " from IP " + request.getRemoteAddr());

    }

}
