/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.model.uma.persistence.UmaScopeDescription;
import org.slf4j.Logger;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.uma.service.UmaScopeService;
import org.xdi.oxauth.util.ServerUtil;

import com.wordnik.swagger.annotations.Api;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */
@Path("/uma/scopes")
@Api(value="/uma/scopes", description = "UMA Scope Endpoint provides scope description (json document) by scope id.")
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
        try {
            if (StringUtils.isNotBlank(id)) {
                final UmaScopeDescription scope = umaScopeService.getScope(id);
                if (scope != null) {
                    final org.gluu.oxauth.model.uma.UmaScopeDescription jsonScope = new org.gluu.oxauth.model.uma.UmaScopeDescription();
                    jsonScope.setIconUri(scope.getIconUrl());
                    jsonScope.setName(scope.getDisplayName());
                    jsonScope.setDescription(scope.getDescription());
                    return Response.status(Response.Status.OK).entity(ServerUtil.asJson(jsonScope)).build();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.NOT_FOUND)).build());
    }
}
