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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import javax.inject.Named;

import org.xdi.model.GluuImage;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.service.XmlService;

import com.wordnik.swagger.annotations.Api;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/05/2013
 */

@Path("/uma/scopes/icons")
@Named("umaScopeIconRestWebService")
@Api(value= "/uma/scopes/icons", description = "UMA Scope Icon endpoint provides scope icon by scope id.")
public class ScopeIconWS {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private ScopeService umaScopeService;
    @Inject
    private XmlService xmlService;

    @GET
    @Path("{id}")
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response getScopeDescription(@PathParam("id") String id) {
        log.trace("UMA - get scope's icon : id: {0}", id);
        try {
            if (StringUtils.isNotBlank(id)) {
                final ScopeDescription scope = umaScopeService.getInternalScope(id);
                if (scope != null && StringUtils.isNotBlank(scope.getFaviconImageAsXml())) {
                    final GluuImage gluuImage = xmlService.getGluuImageFromXML(scope.getFaviconImageAsXml());

                    if (gluuImage != null && ArrayUtils.isNotEmpty(gluuImage.getData())) {
                        // todo yuriyz : it must be clarified how exactly content of image must be shared between oxTrust and oxAuth
                        // currently oxTrust save content on disk however oxAuth expects it in ldap as we must support clustering!

                        // send non-streamed image as it's anyway picked up in memory (i know it's not nice...)

                        return Response.status(Response.Status.OK).entity(gluuImage.getData()).build();
                    }
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
