/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.security.service.ExternalInterceptionService;
import io.jans.configapi.security.service.AuthorizationService;
import io.jans.configapi.util.ApiConstants;

import java.util.Map;
import java.util.HashMap;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.json.JSONException;
import org.json.JSONObject;
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
    HttpServletResponse httpServletResponse;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    AuthorizationService authorizationService;
    
    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject 
    Jackson jackson;
    
    @Inject 
    ExternalInterceptionService externalInterceptionService;

    @SuppressWarnings({ "all" })
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
        boolean configOauthEnabled = authorizationService.isConfigOauthEnabled();
        log.info("\n\n\n AuthorizationFilter::filter() - authorizationHeader = " + authorizationHeader + " , issuer = "
                + issuer + " , configOauthEnabled = " + configOauthEnabled + "\n\n\n");

        if (!configOauthEnabled) {
            log.info("====== Authorization Granted...====== ");
            return;
        }

        log.info("\n\n\n AuthorizationFilter::filter() - Config Api OAuth Valdation Enabled");
        
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(context);
            log.info("======ONLY TOKEN BASED AUTHORIZATION IS SUPPORTED======================");
            return;
        }
        try {
            log.info("\n\n\n AuthorizationFilter::filter() - before - authorizationHeader = "+authorizationHeader+"\n");
            authorizationHeader = this.authorizationService.processAuthorization(authorizationHeader, issuer,
                    resourceInfo, context.getMethod(), request.getRequestURI());
            log.info("\n\n\n AuthorizationFilter::filter() - after - authorizationHeader = "+authorizationHeader+"\n");
            if (authorizationHeader != null && authorizationHeader.trim().length() > 0) {
                context.getHeaders().remove(HttpHeaders.AUTHORIZATION);
                context.getHeaders().add(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            
            //Custom authorization   
            log.info("\n\n\n AuthorizationFilter::filter() - Custom - authorizationHeader = "+authorizationHeader+"\n");
            boolean isAuthorized = isAuthorized(request, httpServletResponse, authorizationHeader, issuer,  context.getMethod(), info.getPath());
            log.error("\n\n\n Custom authorization - isAuthorized:{}",isAuthorized);
            if (!isAuthorized) {
                abortWithUnauthorized(context);
                log.info("======Custom authorization FAILED======================");
                return;
            }
            
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
    
     /*private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response, String token, String issuer, String method,
                String path) throws Exception {
            log.debug("Authorization script params -  request:{}, response:{}, token:{}, issuer:{}, method:{}, path:{} ", request, response, token, issuer, method, path);
        return externalInterceptionService.authorization(request, response, this.configurationFactory.getApiAppConfiguration(), token, issuer, method, path);
    }*/
     
     private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response, String token, String issuer, String method,
             String path) throws Exception {
         log.error("\n\n Authorization script params -  request:{}, response:{}, token:{}, issuer:{}, method:{}, path:{} ", request, response, token, issuer, method, path);
         Map<String, Object> requestParameters = new HashMap<>();
         requestParameters.put("ISSUER",issuer);
         requestParameters.put("TOKEN",token);
         requestParameters.put("METHOD",method);
         requestParameters.put("PATH",path);
         JSONObject responseAsJsonObject = jackson.createJSONObject(requestParameters);
         log.error("\n\n Authorization script params - responseAsJsonObject = "+responseAsJsonObject+"\n\n\n");
         log.error("Authorization script params -  request:{}, response:{}, requestParameters:{}, responseAsJsonObject:{} ", request, response, requestParameters, responseAsJsonObject);
     return externalInterceptionService.authorization(request, response, this.configurationFactory.getApiAppConfiguration(), requestParameters, responseAsJsonObject);
 }

}
