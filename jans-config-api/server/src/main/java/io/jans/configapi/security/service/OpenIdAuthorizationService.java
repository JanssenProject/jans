/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.util.*;
import io.jans.as.model.common.IntrospectionResponse;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

@ApplicationScoped
@Named("openIdAuthorizationService")
@Alternative
@Priority(1)
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String AUTHENTICATION_SCHEME = "Bearer ";

    @Inject
    transient Logger logger;

    @Context
    transient HttpServletRequest request;

    @Context
    transient HttpServletResponse response;

    @Inject
    transient JwtUtil jwtUtil;

    @Inject
    OpenIdService openIdService;

    @Inject
    ExternalInterceptionService externalInterceptionService;

    public String processAuthorization(String token, String issuer, ResourceInfo resourceInfo, String method,
            String path) throws Exception {
        logger.debug("oAuth  Authorization parameters , token:{}, issuer:{}, resourceInfo:{}, method: {}, path: {} ",
                token, issuer, resourceInfo, method, path);

        if (StringUtils.isBlank(token)) {
            logger.error("Token is blank !!!");
            throw new WebApplicationException("Token is blank.", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Validate issuer
        logger.info("Validate issuer");
        if (StringUtils.isNotBlank(issuer) && !authUtil.isValidIssuer(issuer)) {
            throw new WebApplicationException("Header Issuer is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // Check the type of token simple, jwt, reference
        logger.info("Verify if JWT");
        String acccessToken = token.substring("Bearer".length()).trim();
        boolean isJwtToken = jwtUtil.isJwt(acccessToken);

        if (isJwtToken) {
            try {
                logger.info("Since token is JWT Validate it");
                jwtUtil.parse(acccessToken);
                List<String> tokenScopes = jwtUtil.validateToken(acccessToken);
                logger.debug(" tokenScopes:{} ", tokenScopes);
                // Validate Scopes
                return this.validateScope(acccessToken, tokenScopes, resourceInfo, issuer);
            } catch (InvalidJwtException exp) {
                logger.error("oAuth Invalid Jwt token:{}, exception:{} ", token, exp);
                throw new WebApplicationException("Jwt Token is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        logger.info("Token is NOT JWT hence introspecting it as Reference token ");
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse(token,
                token.substring("Bearer".length()).trim(), issuer);

        logger.trace("oAuth  Authorization introspectionResponse:{}", introspectionResponse);
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            logger.error("Token is Invalid.");
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<String> tokenScopes = introspectionResponse.getScope();
        // Validate Scopes
        acccessToken = validateScope(acccessToken, tokenScopes, resourceInfo, issuer);

        boolean isAuthorized = externalAuthorization(token, issuer, method, path);
        logger.debug("Custom authorization - isAuthorized:{}", isAuthorized);

        return acccessToken;
    }

    private String validateScope(String accessToken, List<String> tokenScopes, ResourceInfo resourceInfo, String issuer)
            throws WebApplicationException {
        logger.debug("Validate scope, accessToken:{}, tokenScopes:{}, resourceInfo: {}, issuer: {}", accessToken,
                tokenScopes, resourceInfo, issuer);
        try {
            // Get resource scope
            List<String> resourceScopes = getRequestedScopes(resourceInfo);

            // Check if resource requires auth server specific scope
            List<String> authSpecificScope = getAuthSpecificScopeRequired(resourceInfo);
            logger.debug(" resourceScopes:{}, authSpecificScope:{} ", resourceScopes, authSpecificScope);

            // If No auth scope required OR if token contains the authSpecificScope
            if ((authSpecificScope == null || authSpecificScope.isEmpty())) {
                logger.debug("Validating token scopes as no authSpecificScope required");
                if (!validateScope(tokenScopes, resourceScopes)) {
                    logger.error("Insufficient scopes! Required scope:{} -  however token scopes:{}", resourceScopes,
                            tokenScopes);
                    throw new WebApplicationException("Insufficient scopes! , Required scope: " + resourceScopes
                            + ", however token scopes: " + tokenScopes,
                            Response.status(Response.Status.UNAUTHORIZED).build());
                }
                return AUTHENTICATION_SCHEME + accessToken;
            }

            // find missing scopes
            List<String> missingScopes = findMissingElements(resourceScopes, tokenScopes);
            logger.debug("missingScopes:{}", missingScopes);

            // If only authSpecificScope missing then proceed with token creation else throw
            // error
            if (missingScopes != null && !missingScopes.isEmpty()
                    && !isEqualCollection(missingScopes, authSpecificScope)) {
                logger.error("Insufficient scopes!! Required scope:{}, , however token scopes:{} ", resourceScopes,
                        tokenScopes);
                throw new WebApplicationException("Insufficient scopes!! , Required scope: " + resourceScopes
                        + ", however token scopes: " + tokenScopes,
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            // Generate token with required resourceScopes
            resourceScopes.addAll(authSpecificScope);
            accessToken = openIdService.requestAccessToken(authUtil.getClientId(), resourceScopes);
            logger.debug("Introspecting new accessToken:{}", accessToken);

            // Introspect
            IntrospectionResponse introspectionResponse = openIdService
                    .getIntrospectionResponse(AUTHENTICATION_SCHEME + accessToken, accessToken, authUtil.getIssuer());

            // Validate Token Scope
            if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
                logger.error("Insufficient scopes!!! for new token as well - Required scope:{}, token scopes:{}",
                        resourceScopes, introspectionResponse.getScope());
                throw new WebApplicationException(
                        "Insufficient scopes!!! Required scope: " + resourceScopes + ", token scopes: "
                                + introspectionResponse.getScope(),
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }

            logger.info("Token scopes Valid Returning accessToken:{}", accessToken);
            return AUTHENTICATION_SCHEME + accessToken;
        } catch (Exception ex) {
            if (log.isErrorEnabled()) {
                log.error("oAuth authorization error:{} ", ex.getMessage());
            }
            throw new WebApplicationException("oAuth authorization error " + ex.getMessage(),
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
    }

    private boolean externalAuthorization(String token, String issuer, String method, String path) {
        logger.debug(
                "External Authorization script params -  request:{}, response:{}, token:{}, issuer:{}, method:{}, path:{} ",
                request, response, token, issuer, method, path);
        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("ISSUER", issuer);
        requestParameters.put("TOKEN", token);
        requestParameters.put("METHOD", method);
        requestParameters.put("PATH", path);
        JSONObject responseAsJsonObject = Jackson.createJSONObject(requestParameters);
        return externalInterceptionService.authorization(request, response,
                this.configurationFactory.getApiAppConfiguration(), requestParameters, responseAsJsonObject);
    }

}