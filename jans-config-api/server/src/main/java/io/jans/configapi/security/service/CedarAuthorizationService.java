/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.security.service;

import io.jans.configapi.service.cedar.CedarlingService;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.util.*;

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

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;

@ApplicationScoped
@Named("cedarAuthorizationService")
@Alternative
@Priority(2)
public class CedarAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    transient Logger logger;

    @Inject
    transient CedarlingService cedarlingService;

    public String processAuthorization(String token, String issuer, ResourceInfo resourceInfo, String method,
            String path) throws WebApplicationException, Exception {
        logger.info("oAuth  Authorization parameters , token:{}, issuer:{}, resourceInfo:{}, method: {}, path: {} ",
                token, issuer, resourceInfo, method, path);

        if (StringUtils.isBlank(token)) {
            logger.info("Token is blank !!!");
            throw new WebApplicationException("Token is blank.", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        // authorize
        boolean isAuthorized = cedarlingService.authorize(token, issuer, resourceInfo, resourceInfo.toString(), path);

        // Validate issuer
        logger.info("isAuthorized:{}", isAuthorized);
        if (!isAuthorized) {
            throw new WebApplicationException("Token is Invalid.",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return token;
    }

}