/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.service.OpenIdService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;

@ApplicationScoped
@Named("openIdAuthorizationService")
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger logger;

    @Inject
    OpenIdService openIdService;

    public void validateAuthorization(String token, ResourceInfo resourceInfo, String methods, String path) throws Exception {
        if (StringUtils.isBlank(token)) {
            logger.error("Token is blank !!!!!!");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
        List<String> resourceScopes = getRequestedScopes(resourceInfo);

        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionService()
                .introspectToken("Bearer " + token, token);
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            logger.error("Token is Invalid.");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            logger.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: "
                    + introspectionResponse.getScope());
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

    }

}