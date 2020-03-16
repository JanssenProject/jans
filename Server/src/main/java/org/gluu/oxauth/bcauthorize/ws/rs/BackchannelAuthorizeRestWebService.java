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
 * @version August 20, 2019
 */
@Api(value = "/", description = "The Backchannel Authentication Endpoint is used to initiate an out-of-band authentication of the end-user.")
public interface BackchannelAuthorizeRestWebService {

    @POST
    @Path("/bc-authorize")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Performs backchannel authorization.",
            notes = "The Backchannel Authentication Endpoint is used to initiate an out-of-band authentication of the end-user.",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "    The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, contains more than one of the hints, or is otherwise malformed."),
            @ApiResponse(code = 400, message = "invalid_scope\n" +
                    "    The requested scope is invalid, unknown, or malformed."),
            @ApiResponse(code = 400, message = "expired_login_hint_token\n" +
                    "    The login_hint_token provided in the authentication request is not valid because it has expired."),
            @ApiResponse(code = 400, message = "unknown_user_id\n" +
                    "    The OpenID Provider is not able to identify which end-user the Client wishes to be authenticated by means of the hint provided in the request (login_hint_token, id_token_hint or login_hint)."),
            @ApiResponse(code = 400, message = "unauthorized_client\n" +
                    "    The Client is not authorized to use this authentication flow."),
            @ApiResponse(code = 400, message = "missing_user_code\n" +
                    "    User code is required but was missing from the request."),
            @ApiResponse(code = 400, message = "invalid_user_code\n" +
                    "    User code was invalid."),
            @ApiResponse(code = 400, message = "invalid_binding_message\n" +
                    "    The binding message is invalid or unacceptable for use in the context of the given request."),
            @ApiResponse(code = 401, message = "invalid_client\n" +
                    "    Client authentication failed (e.g., invalid client credentials, unknown client, no client authentication included, or unsupported authentication method)."),
            @ApiResponse(code = 403, message = "access_denied\n" +
                    "    The resource owner or OpenID Provider denied the CIBA (Client Initiated Backchannel Authentication) request.")
    })
    Response requestBackchannelAuthorizationPost(
            @FormParam("client_id")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server. ", required = true)
                    String clientId,
            @FormParam("scope")
            @ApiParam(value = "CIBA authentication requests must contain the openid scope value.", required = true)
                    String scope,
            @FormParam("client_notification_token")
            @ApiParam(value = "It is a bearer token provided by the Client that will be used by the OpenID Provider to authenticate the callback request to the Client. It is required if the Client is registered to use Ping or Push modes.", required = true)
                    String clientNotificationToken,
            @FormParam("acr_values")
            @ApiParam(value = "Requested Authentication Context Class Reference values.", required = false)
                    String acrValues,
            @FormParam("login_hint_token")
            @ApiParam(value = "A token containing information identifying the end-user for whom authentication is being requested.", required = false)
                    String loginHintToken,
            @FormParam("id_token_hint")
            @ApiParam(value = "An ID Token previously issued to the Client by the OpenID Provider being passed back as a hint to identify the end-user for whom authentication is being requested.", required = false)
                    String idTokenHint,
            @FormParam("login_hint")
            @ApiParam(value = "A hint to the OpenID Provider regarding the end-user for whom authentication is being requested.", required = false)
                    String loginHint,
            @FormParam("binding_message")
            @ApiParam(value = "A human readable identifier or message intended to be displayed on both the consumption device and the authentication device to interlock them together for the transaction by way of a visual cue for the end-user.", required = false)
                    String bindingMessage,
            @FormParam("user_code")
            @ApiParam(value = "A secret code, such as password or pin, known only to the user but verifiable by the OP.", required = false)
                    String userCode,
            @FormParam("requested_expiry")
            @ApiParam(value = "A positive integer allowing the client to request the expires_in value for the auth_req_id the server will return.", required = false)
                    Integer requestedExpiry,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext
    );
}