package org.xdi.oxauth.token.ws.rs;

import javax.ws.rs.GET;
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

/**
 * Provides interface for validate token REST web services
 *
 * @author Javier Rojas Blum Date: 10.27.2011
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
    Response validateAccessToken(
            @QueryParam("access_token")
            @ApiParam(value = "Access Token to validate.", required = true)
            String accessToken,
            @Context SecurityContext sec);
}