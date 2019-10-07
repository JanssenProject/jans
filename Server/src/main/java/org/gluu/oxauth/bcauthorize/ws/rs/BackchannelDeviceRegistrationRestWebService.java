/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

import com.wordnik.swagger.annotations.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Api(value = "/", description = "The Backchannel Device Registration Endpoint is used to register the end-user device for push notifications (FCM).")
public interface BackchannelDeviceRegistrationRestWebService {

    @POST
    @Path("/bc-deviceRegistration")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Performs backchannel device registration.",
            notes = "The Backchannel Device Registration Endpoint is used to register the end-user device for push notifications (FCM).",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "    The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed."),
            @ApiResponse(code = 400, message = "unknown_user_id\n" +
                    "    The OpenID Provider is not able to identify the end-user."),
            @ApiResponse(code = 400, message = "unauthorized_client\n" +
                    "    The Client is not authorized to use this authentication flow."),
            @ApiResponse(code = 403, message = "access_denied\n" +
                    "    The resource owner or OpenID Provider denied the request.")
    })
    Response requestBackchannelDeviceRegistrationPost(
            @FormParam("id_token_hint")
            @ApiParam(value = "An ID Token previously issued to the Client by the OpenID Provider being passed back as a hint to identify the end-user for whom the device registration is being requested.", required = true)
                    String idTokenHint,
            @FormParam("device_registration_token")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server. ", required = true)
                    String deviceRegistrationToken,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext
    );
}