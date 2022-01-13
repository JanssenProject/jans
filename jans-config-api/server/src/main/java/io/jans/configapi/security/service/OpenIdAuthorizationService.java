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

    public String processAuthorization(String token, String issuer, ResourceInfo resourceInfo, String method,
            String path) throws Exception {
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
                return this.validateScope(tokenScopes, resourceInfo, issuer);
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
        return this.validateScope(tokenScopes, resourceInfo, issuer);
    }

    private String validateScope(List<String> tokenScopes, ResourceInfo resourceInfo, String issuer) throws Exception {
        log.debug("Validate scope, tokenScopes:{}, resourceInfo: {}, issuer: {}", tokenScopes, resourceInfo, issuer);
        String accessToken = null;
        // Get resource scope
        List<String> resourceScopes = getRequestedScopes(resourceInfo);

        // Check if resource requires auth server specific scope
        List<String> authSpecificScope = getAuthSpecificScopeRequired(resourceInfo);
        log.debug(" resourceScopes = " + resourceScopes + " ,authSpecificScope = " + authSpecificScope);

        // If No auth scope required OR if token contains the authSpecificScope
        if ((authSpecificScope == null || authSpecificScope.size() == 0)) {
            log.debug("Validating token scopes as no authSpecificScope required");
            if (!validateScope(tokenScopes, resourceScopes)) {
                log.error("Insufficient scopes! Required scope: " + resourceScopes + ", however token scopes: "
                        + tokenScopes);
                throw new WebApplicationException("Insufficient scopes! , Required scope: " + resourceScopes
                        + ", however token scopes: " + tokenScopes,
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
            return null;
        }

        // find missing scopes
        List<String> missingScopes = findMissingElements(resourceScopes, tokenScopes);
        log.debug("missingScopes = " + missingScopes);

        // If only authSpecificScope missing then proceed with token creation else throw
        // error
        if (missingScopes != null && missingScopes.size() > 0 && !isEqualCollection(missingScopes, authSpecificScope)) {
            log.error("Insufficient scopes!! Required scope: " + resourceScopes + ", however token scopes: "
                    + tokenScopes);
            throw new WebApplicationException("Insufficient scopes!! , Required scope: " + resourceScopes
                    + ", however token scopes: " + tokenScopes, Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Generate token with required resourceScopes
        resourceScopes.addAll(authSpecificScope);
        accessToken = openIdService.requestAccessToken(authUtil.getClientId(), resourceScopes);
        log.debug("Introspecting new accessToken = " + accessToken);

        // Introspect
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse("Bearer " + accessToken,
                accessToken, authUtil.getIssuer());

        // Validate Token Scope
        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            log.error("Insufficient scopes!!! for new token as well - Required scope: " + resourceScopes
                    + ", token scopes: " + introspectionResponse.getScope());
            throw new WebApplicationException("Insufficient scopes!!! Required scope: " + resourceScopes
                    + ", token scopes: " + introspectionResponse.getScope(),
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        log.info("Token scopes Valid");
        return "Bearer " + accessToken;
    }

}