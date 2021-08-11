/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.configapi.util.*;
import io.jans.as.model.common.IntrospectionResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Named("openIdAuthorizationService")
@Alternative
@Priority(1)
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger log;

    @Inject
    AuthUtil authUtil;

    @Inject
    JwtUtil jwtUtil;

    @Inject
    OpenIdService openIdService;

    public void processAuthorization(String token, String issuer, ResourceInfo resourceInfo, String method, String path)
            throws Exception {
        log.debug("oAuth  Authorization parameters , token:{}, issuer:{}, resourceInfo:{}, method: {}, path: {} ",
                token, issuer, resourceInfo, method, path);

        if (StringUtils.isBlank(token)) {
            log.error("Token is blank !!!");
            throw new WebApplicationException("Token is blank.", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Validate issuer
        log.info("Validate issuer");
        if (StringUtils.isNotBlank(issuer) && !authUtil.isValidIssuer(issuer)) {
            throw new WebApplicationException("Header Issuer is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Check the type of token simple, jwt, reference
        log.info("Verify if JWT");
        String acccessToken = token.substring("Bearer".length()).trim();
        boolean isJwtToken = jwtUtil.isJwt(acccessToken);
        List<String> tokenScopes = new ArrayList<String>();
        log.debug(" Is Jwt Token isJwtToken = " + isJwtToken);

        if (isJwtToken) {
            try {
                log.info("Since token is JWT Validate it");
                Jwt jwt = jwtUtil.parse(acccessToken);
                tokenScopes = jwtUtil.validateToken(acccessToken);

                // Validate Scopes
                this.validateScope(tokenScopes, resourceInfo, issuer);
                return;
            } catch (InvalidJwtException exp) {
                log.error("oAuth Invalid Jwt " + token + " - Exception is " + exp);
                throw new WebApplicationException("Jwt Token is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        log.info("\n Token is NOT JWT hence introspecting it as Reference token \n");
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse(token,
                token.substring("Bearer".length()).trim(), issuer);

        log.trace("oAuth  Authorization introspectionResponse = " + introspectionResponse);
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            log.error("Token is Invalid.");
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        tokenScopes = introspectionResponse.getScope();
        // Validate Scopes
        this.validateScope(tokenScopes, resourceInfo, issuer);
    }

    private void validateScope(List<String> tokenScopes, ResourceInfo resourceInfo, String issuer) throws Exception {

        log.info("Get requested scopes");
        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        log.trace("oAuth  Authorization Resource details, resourceInfo: {}, resourceScopes: {}, issuer: {} ",
                resourceInfo, resourceScopes, issuer);

        // Check if resource requires auth server specific scope exists
        List<String> authSpecificScope = getAuthSpecificScopeRequired(resourceInfo);
        log.info("\n oAuth authSpecificScope = " + authSpecificScope + "\n");

        if (authSpecificScope == null || authSpecificScope.size() == 0) {
            log.info("Validate token scopes as no authSpecificScope required");
            if (!validateScope(tokenScopes, resourceScopes)) {
                log.error("Insufficient scopes. Required scope: " + resourceScopes + ", however token scopes: "
                        + tokenScopes);
                throw new WebApplicationException("Insufficient scopes. Required scope: " + resourceScopes
                        + ", however token scopes: " + tokenScopes,
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return;
        }

        // Check if missing scopes are authSpecificStore then generate Token for them
        List<String> missingAuthScopeList = findMissingElements(resourceScopes, authSpecificScope);
        log.info("\n oAuth missingAuthScopeList = " + missingAuthScopeList + "\n");
        if (!isEqualCollection(authSpecificScope, missingAuthScopeList)) {
            log.error("Insufficient scopes. Required scope: " + resourceScopes + ", and token scopes: " + tokenScopes);
            throw new WebApplicationException("Insufficient scopes. Required scope",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
        if (authSpecificScope != null && authSpecificScope.size() > 0) {
            // get accessToken for all required scope for an endpoint including the
            // authScope
            resourceScopes.addAll(authSpecificScope);
            String accessToken = openIdService.requestAccessToken(authUtil.getClientId(), resourceScopes);
            log.info("\n Introspecting new accessToken = " + accessToken);
            IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse(accessToken,
                    accessToken.substring("Bearer".length()).trim(), issuer);

            log.info("Validate token scopes");
            if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
                log.error("Insufficient scopes for new token as well - Required scope: " + resourceScopes
                        + ", token scopes: " + introspectionResponse.getScope());
                throw new WebApplicationException("Insufficient scopes. Required scope",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        log.info("Token scopes Valid");

    }

}