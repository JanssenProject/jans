/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.userinfo.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum
 * @version September 7, 2017
 */
@Api(value = "/", description = "The UserInfo Endpoint is an OAuth 2.0 Protected Resource that returns Claims about the authenticated End-User. To obtain the requested Claims about the End-User, the Client makes a request to the UserInfo Endpoint using an Access Token obtained through OpenID Connect Authentication. These Claims are normally represented by a JSON object that contains a collection of name and value pairs for the Claims. ")
public interface UserInfoRestWebService {

    @GET
    @Path("/userinfo")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Returns Claims about the authenticated End-User.",
            notes = "The Access Token obtained from an OpenID Connect Authentication Request is sent as a Bearer Token.",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter, uses more than one method for including an access token, or is otherwise malformed.  The resource server SHOULD respond with the HTTP 400 (Bad Request) status code."),
            @ApiResponse(code = 401, message = "invalid_token\n" +
                    "The access token provided is expired, revoked, malformed, or invalid for other reasons.  The resource SHOULD respond with the HTTP 401 (Unauthorized) status code.  The client MAY request a new access token and retry the protected resource request."),
            @ApiResponse(code = 403, message = "insufficient_scope\n" +
                    "The request requires higher privileges than provided by the access token.  The resource server SHOULD respond with the HTTP 403 (Forbidden) status code and MAY include the \"scope\"\n" +
                    " attribute with the scope necessary to access the protected resource.")
    })
    Response requestUserInfoGet(
            @QueryParam("access_token")
            @ApiParam(value = "OAuth 2.0 Access Token.", required = true)
                    String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext);

    @POST
    @Path("/userinfo")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Returns Claims about the authenticated End-User.",
            notes = "The Access Token obtained from an OpenID Connect Authentication Request is sent as a Bearer Token.",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    "The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats the same parameter, uses more than one method for including an access token, or is otherwise malformed.  The resource server SHOULD respond with the HTTP 400 (Bad Request) status code."),
            @ApiResponse(code = 401, message = "invalid_token\n" +
                    "The access token provided is expired, revoked, malformed, or invalid for other reasons.  The resource SHOULD respond with the HTTP 401 (Unauthorized) status code.  The client MAY request a new access token and retry the protected resource request."),
            @ApiResponse(code = 403, message = "insufficient_scope\n" +
                    "The request requires higher privileges than provided by the access token.  The resource server SHOULD respond with the HTTP 403 (Forbidden) status code and MAY include the \"scope\"\n" +
                    " attribute with the scope necessary to access the protected resource.")
    })
    Response requestUserInfoPost(
            @FormParam("access_token")
            @ApiParam(value = "OAuth 2.0 Access Token.", required = true)
                    String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext);
}