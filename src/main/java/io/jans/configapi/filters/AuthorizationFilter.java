/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.auth.AuthorizationService;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * @author Mougang T.Gasmyr
 */
@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {
    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    Logger logger;

    @Inject
    AuthorizationService authorizationService;

    public void filter(ContainerRequestContext context) {
        logger.info("=======================================================================");
        logger.debug("====== info.getAbsolutePath() = " +info.getAbsolutePath()+" , info.getRequestUri() = "+info.getRequestUri()+"\n\n");
        logger.debug("====== resourceInfo.getClass().getName().toString() = " +resourceInfo.getClass().getName().toString()+" resourceInfo.getResourceClass().getName() = "+ resourceInfo.getResourceClass().getName()+" , resourceInfo.getClass().getAnnotations().toString() = "+resourceInfo.getClass().getAnnotations().toString());
        logger.debug("======" + context.getMethod() + " " + info.getPath() + " FROM IP " + request.getRemoteAddr());
        logger.info("======PERFORMING AUTHORIZATION=========================================");
        String authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);

        logger.debug("\n\n\n filter - authorizationHeader = " + authorizationHeader + "\n\n\n");

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(context);
            logger.info("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
            return;
        }
        try {
            String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
            this.authorizationService.validateAuthorization(token, resourceInfo,context.getMethod(), info.getPath());
            logger.info("======AUTHORIZATION  GRANTED===========================================");
        } catch (Exception ex) {
            logger.error("======AUTHORIZATION  FAILED ===========================================", ex);
            abortWithUnauthorized(context);
        }

    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME).build());
    }

}
