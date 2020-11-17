/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.configapi.auth.service.OpenIdService;
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
    OpenIdService openIdService;

    public void processAuthorization(String token, ResourceInfo resourceInfo, String method, String path)
            throws Exception {

        if (StringUtils.isBlank(token)) {
            log.error("Token is blank !!!");
            throw new WebApplicationException("Token is blank.", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        List<String> resourceScopes = getRequestedScopes(resourceInfo);
        // IntrospectionResponse introspectionResponse =
        // openIdService.getIntrospectionService().introspectToken("Bearer " + token,
        // token);
        IntrospectionResponse introspectionResponse = openIdService.getIntrospectionResponse("Bearer " + token, token);
        
        if (introspectionResponse == null || !introspectionResponse.isActive()) {
            log.error("Token is Invalid.");
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        if (!validateScope(introspectionResponse.getScope(), resourceScopes)) {
            log.error("Insufficient scopes. Required scope: " + resourceScopes + ", token scopes: "
                    + introspectionResponse.getScope());
            throw new WebApplicationException("Insufficient scopes. Required scope",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

    }

}