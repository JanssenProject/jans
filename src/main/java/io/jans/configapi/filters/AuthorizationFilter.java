/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.auth.AuthorizationService;
import io.jans.configapi.auth.util.AuthUtil;
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
    
    @Inject
    AuthUtil authUtil;

    public void filter(ContainerRequestContext context) {
        System.out.println("\n\n\n\n AuthorizationFilter::filter() - Entry - authUtil.isTestMode() = " + authUtil.isTestMode() + "\n\n\n");
        log.info("=======================================================================");
        log.info("====== info.getAbsolutePath() = " +info.getAbsolutePath()+" , info.getRequestUri() = "+info.getRequestUri()+"\n\n");
        log.info("====== info.getBaseUri()=" + info.getBaseUri() + " info.getPath()=" + info.getPath() + " info.toString()=" + info.toString());
        log.info("====== request.getContextPath()=" + request.getContextPath() + " request.getRequestURI()=" + request.getRequestURI()+ " request.toString() " + request.toString());
        log.info("======" + context.getMethod() + " " + info.getPath() + " FROM IP " + request.getRemoteAddr());
        log.info("======PERFORMING AUTHORIZATION=========================================");
        String authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);

        log.info("\n\n\n AuthorizationFilter::filter() - authorizationHeader = " + authorizationHeader+" , AuthUtil.isTestMode() = "+authUtil.isTestMode()+"\n\n\n");
        
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(context);
            log.info("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
            return;
        }
        try {
            if(authUtil.isTestMode())  {
                authorizationHeader = this.testAuthenticationPrep(resourceInfo, context.getMethod(), request.getRequestURI());
            }
            log.info("\n\n\n AuthorizationFilter::filter() - after testAuthenticationPrep() -  authorizationHeader = " + authorizationHeader+" , AuthUtil.isTestMode() = "+authUtil.isTestMode()+"\n\n\n");
            
            //Api protection validation
            this.authorizationService.processAuthorization(authorizationHeader, resourceInfo, context.getMethod(), request.getRequestURI());
            log.info("======AUTHORIZATION  GRANTED===========================================");
        } catch (Exception ex) {
            log.error("======AUTHORIZATION  FAILED ===========================================", ex);
            abortWithUnauthorized(context);
        }
        finally {
            //TODO::if test mode delete tokens that were created for test client --- ???
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
    
    private String testAuthenticationPrep(ResourceInfo resourceInfo, String method, String path) throws Exception {
        log.trace("testAuthenticationPrep() - resourceInfo = "+ resourceInfo +" , method = "+method+" , path = "+path+"\n\n");
        System.out.println("testAuthenticationPrep() - resourceInfo = "+ resourceInfo +" , method = "+method+" , path = "+path+"\n\n");
        String token = AUTHENTICATION_SCHEME +" "+ authUtil.testPrep(resourceInfo,  method,  path);
       return token;
    }


}
