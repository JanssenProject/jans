/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.auth.service.OpenIdService;
import io.jans.configapi.auth.util.AuthUtil;
import io.jans.configapi.auth.util.JwtUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;

@ApplicationScoped
@Named("openIdAuthorizationService")
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

        log.info("Get requested scopes");
        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        log.trace("oAuth  Authorization Resource details, resourceInfo: {}, resourceScopes: {} ", resourceInfo,
                resourceScopes);

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
        log.trace(" Is Jwt Token isJwtToken = " + isJwtToken);

        if (isJwtToken) {
            try {
                log.info("Validate JWT");
                Jwt jwt = jwtUtil.parse(acccessToken);
                jwtUtil.validateToken(acccessToken, resourceScopes);
                return;
            } catch (InvalidJwtException exp) {
                log.error("oAuth Invalid Jwt " + token + " - Exception is " + exp);
                throw new WebApplicationException("Jwt Token is Invalid.",
                        Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        log.info("\n Validate Reference token \n");
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse(token,
                token.substring("Bearer".length()).trim(), issuer);

        log.trace("oAuth  Authorization introspectionResponse = " + introspectionResponse);
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            log.error("Token is Invalid.");
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        log.info("Validate token scopes");
        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            log.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: "
                    + introspectionResponse.getScope());
            throw new WebApplicationException("Insufficient scopes. Required scope",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

}