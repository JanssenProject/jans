/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.ws.rs;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.uma.UmaConstants;
import io.jans.as.model.uma.UmaErrorResponseType;
import io.jans.as.model.uma.UmaScopeDescription;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.uma.service.UmaScopeService;
import io.jans.as.server.util.ServerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */
@Path("/uma/scopes")
public class UmaScopeWS {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UmaScopeService umaScopeService;

    @GET
    @Path("{id}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response getScopeDescription(@PathParam("id") String id) {
        log.trace("UMA - get scope description: id: {}", id);
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);
        try {
            if (StringUtils.isNotBlank(id)) {
                final Scope scope = umaScopeService.getScope(id);
                if (scope != null) {
                    final UmaScopeDescription jsonScope = new UmaScopeDescription();
                    jsonScope.setIconUri(scope.getIconUrl());
                    jsonScope.setName(scope.getId());
                    jsonScope.setDescription(scope.getDescription());
                    return Response.status(Response.Status.OK).entity(ServerUtil.asJson(jsonScope)).build();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Internal error.");
        }
        throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_FOUND, UmaErrorResponseType.NOT_FOUND, "Not found.");
    }
}
