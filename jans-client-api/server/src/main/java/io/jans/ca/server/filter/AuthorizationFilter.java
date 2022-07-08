/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.filter;

import io.jans.ca.server.security.service.AuthorizationService;
import io.jans.ca.common.rest.ProtectedApi;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private static final String AUTHORIZATION_RP_ID = "AuthorizationRpId";

    private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Inject
    AuthorizationService authorizationService;

    @SuppressWarnings({"all"})
    public void filter(ContainerRequestContext context) {
        log.info("=======================================================================");
        log.info("====== context = " + context + " , info.getAbsolutePath() = " + info.getAbsolutePath()
                + " , info.getRequestUri() = " + info.getRequestUri() + "\n\n");
        log.info("====== info.getBaseUri()=" + info.getBaseUri() + " info.getPath()=" + info.getPath()
                + " info.toString()=" + info.toString());
        log.info("====== request.getContextPath()=" + request.getContextPath() + " request.getRequestURI()="
                + request.getRequestURI() + " request.toString() " + request.toString());

        log.info("======" + context.getMethod() + " " + info.getPath() + " FROM IP " + request.getRemoteAddr());
        log.info("======PERFORMING AUTHORIZATION=========================================");
        String authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        String authorizationRpIdHeader = context.getHeaderString(AUTHORIZATION_RP_ID);

        log.info("\n\n\n AuthorizationFilter::filter() - authorizationHeader = " + authorizationHeader + " , authorizationRpIdHeader = "
                + authorizationRpIdHeader + " \n\n\n");
        try {
            authorizationService.processAuthorization(info.getPath(), context.getMethod(), request.getRemoteAddr(), authorizationHeader, authorizationRpIdHeader);
            log.info("======AUTHORIZATION  GRANTED===========================================");
        } catch (Exception ex) {
            log.error("======AUTHORIZATION  FAILED ===========================================", ex);
            abortWithUnauthorized(context, ex.getMessage());
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String errMsg) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(errMsg)
                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
    }

}
