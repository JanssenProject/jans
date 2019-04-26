/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxauth.model.uma.UmaErrorResponseType;
import org.gluu.oxauth.uma.service.UmaScopeService;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/05/2013
 */

@Path("/uma/scopes/icons")
@Api(value= "/uma/scopes/icons", description = "UMA Scope Icon endpoint provides scope icon by scope id.")
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
        try {
            if (StringUtils.isNotBlank(id)) {
                final Scope scope = umaScopeService.getScope(id);
                if (scope != null && StringUtils.isNotBlank(scope.getIconUrl())) {
                    return Response.temporaryRedirect(new URI(scope.getIconUrl())).build();
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
