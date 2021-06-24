/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.security.service.AuthorizationService;
import io.jans.configapi.util.ApiConstants;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 */
@Provider
@ProtectedApi
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Inject
    Logger log;

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    AuthorizationService authorizationService;

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
        String issuer = context.getHeaderString(ApiConstants.ISSUER);

        log.info("\n\n\n AuthorizationFilter::filter() - authorizationHeader = " + authorizationHeader + " , issuer = "
                + issuer + "\n\n\n");

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(context);
            log.info("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
            return;
        }
        try {
            /* To test - Start */
            if (context.getMethod().equals("PATCH")) {
                MediaType requestMediaType = context.getMediaType();
                log.info("====== PATCH Method ======" + isJsonPatch(requestMediaType));
                /*
                 * if(isJsonPatch(requestMediaType) && requestMediaType.getParameters() != null
                 * && !requestMediaType.getParameters().isEmpty()) {
                 */
                if (isJsonPatch(requestMediaType)) {
                    log.info("====== PATCH Method  content-type ======");
                    // context.getHeaders().putSingle("content-type",MediaType.APPLICATION_JSON_PATCH_JSON);
                }
            }
            /* To test - End */

            this.authorizationService.processAuthorization(authorizationHeader, issuer, resourceInfo,
                    context.getMethod(), request.getRequestURI());
            log.info("======AUTHORIZATION  GRANTED===========================================");
        } catch (Exception ex) {
            log.error("======AUTHORIZATION  FAILED ===========================================", ex);
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

    protected boolean isJsonPatch(MediaType mediaType) {
        log.info("====== PATCH Method mediaType.getType() = " + mediaType.getType() + " , mediaType.getSubtype() = "
                + mediaType.getSubtype() + "\n\n");
        if (mediaType != null && StringUtils.equalsIgnoreCase(mediaType.getType(), "application")
                && StringUtils.equalsIgnoreCase(mediaType.getSubtype(), "json-patch+json")) {
            return true;
        }
        return false;
    }

}
