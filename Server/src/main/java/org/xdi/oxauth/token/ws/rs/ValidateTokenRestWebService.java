/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.token.ws.rs;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for validate token REST web services
 *
 * @author Javier Rojas Blum
 * @version January 27, 2016
 */
@Path("/oxauth")
@Api(value = "/oxauth", description = "Validation Endpoint is used to validate an Access Token.")
public interface ValidateTokenRestWebService {

    @GET
    @Path("/validate")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Validates Access Token",
            notes = "Validates Access Token",
            response = Response.class,
            responseContainer = "JSON"
    )
    Response validateAccessTokenGet(
            @QueryParam("access_token")
            @ApiParam(value = "Access Token to validate.", required = true)
            String accessToken,
            @Context HttpServletRequest httpRequest,
            @Context SecurityContext sec);

    @POST
    @Path("/validate")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Validates Access Token",
            notes = "Validates Access Token",
            response = Response.class,
            responseContainer = "JSON"
    )
    Response validateAccessTokenPost(
            @QueryParam("access_token")
            @ApiParam(value = "Access Token to validate.", required = true)
            String accessToken,
            @Context HttpServletRequest httpRequest,
            @Context SecurityContext sec);
}