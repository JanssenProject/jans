/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaConstants;
import org.xdi.oxauth.model.uma.UmaErrorResponseType;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.UmaValidationService;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 *
 * @author Yuriy Zabrovarnyy
 */
@Path("/requester/rpt")
@Api(value = "/requester/rpt", description = "The endpoint at which the requester asks the AM to issue an RPT")
@Name("rptRestWebService")
public class CreateRptWS {

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private RPTManager rptManager;
    @In
    private UmaValidationService umaValidationService;

    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    @ApiOperation(value = "The endpoint at which the requester asks the AM to issue an RPT",
            produces = UmaConstants.JSON_MEDIA_TYPE,
            notes = "The endpoint at which the requester asks the AM to issue an RPT")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public Response getRequesterPermissionToken(@HeaderParam("Authorization") String authorization,
                                                @HeaderParam("Host") String amHost) {
        try {
            umaValidationService.assertHasAuthorizationScope(authorization);
            String validatedAmHost = umaValidationService.validateAmHost(amHost);

            return createRpt(authorization, validatedAmHost);
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getUmaJsonErrorResponse(UmaErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private Response createRpt(String authorization, String amHost) throws IOException {
        UmaRPT rpt = rptManager.createRPT(authorization, amHost);

        // convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
        final String entity = ServerUtil.asJson(new RPTResponse(rpt.getCode()));

        final ResponseBuilder builder = Response.status(Response.Status.CREATED);
        builder.entity(entity);
        return builder.build();
    }
}
