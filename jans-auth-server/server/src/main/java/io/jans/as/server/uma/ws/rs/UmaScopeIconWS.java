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
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.uma.service.UmaScopeService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.net.URI;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/05/2013
 */

@Path("/uma/scopes/icons")
public class UmaScopeIconWS {

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
        log.trace("UMA - get scope's icon : id: {}", id);
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.UMA);

        try {
            if (StringUtils.isNotBlank(id)) {
                final Scope scope = umaScopeService.getScope(id);
                if (scope != null && StringUtils.isNotBlank(scope.getIconUrl())) {
                    return Response.temporaryRedirect(new URI(scope.getIconUrl())).build();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, UmaErrorResponseType.SERVER_ERROR, "Internal error.");
        }
        throw errorResponseFactory.createWebApplicationException(Response.Status.NOT_FOUND, UmaErrorResponseType.NOT_FOUND, "Scope not found.");
    }
}
