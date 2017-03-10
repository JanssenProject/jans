/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import javax.inject.Named;

import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.util.ServerUtil;

import com.wordnik.swagger.annotations.Api;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/04/2013
 */
@Path("/uma/scopes")
@Named("umaScopeRestWebService")
@Api(value="/uma/scopes", description = "UMA Scope Endpoint provides scope description (json document) by scope id.")
public class ScopeWS {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private ScopeService umaScopeService;

    @GET
    @Path("{id}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response getScopeDescription(@PathParam("id") String id) {
        log.trace("UMA - get scope description: id: {0}", id);
        try {
            if (StringUtils.isNotBlank(id)) {
                final ScopeDescription scope = umaScopeService.getInternalScope(id);
                if (scope != null) {
                    final org.xdi.oxauth.model.uma.ScopeDescription jsonScope = new org.xdi.oxauth.model.uma.ScopeDescription();
                    jsonScope.setIconUri(scope.getIconUrl());
                    jsonScope.setName(scope.getDisplayName());
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
