package org.xdi.oxauth.register.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
 * Provides interface for register REST web services.
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 * @version 0.1, 01.11.2012
 */
@Path("/oxauth")
@Api(value = "/oxauth", description = "The Client Registration Endpoint is an OAuth 2.0 Protected Resource through which a new Client registration can be requested. The OpenID Provider MAY require an Initial Access Token that is provisioned out-of-band (in a manner that is out of scope for this specification) to restrict registration requests to only authorized Clients or developers.")
public interface RegisterRestWebService {

    /**
     * In order for an OpenID Connect client to utilize OpenID services for a user, the client needs to register with
     * the OpenID Provider to acquire a client ID and shared secret.
     *
     * @param requestParams   request parameters
     * @param authorization   authorization
     * @param httpRequest     http request object
     * @param securityContext An injectable interface that provides access to security related information.
     * @return response
     */
    @POST
    @Path("/register")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Registers new client dynamically.",
            notes = "Registers new client dynamically.",
            response = Response.class
    )
    Response requestRegister(
            @ApiParam(value = "Request parameters as JSON object with data described by Connect Client Registration Specification. ", required = true)
            String requestParams,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest httpRequest,
            @Context SecurityContext securityContext);

    /**
     * This operation updates the Client Metadata for a previously registered client.
     *
     * @param requestParams   request parameters
     * @param clientId        client id
     * @param authorization   Access Token that is used at the Client Configuration Endpoint
     * @param httpRequest     http request object
     * @param securityContext An injectable interface that provides access to security related information.
     * @return response
     */

    @PUT
    @Path("register")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Updates client info.",
            notes = "Updates client info.",
            response = Response.class,
            responseContainer = "JSON"
    )
    Response requestClientUpdate(
            @ApiParam(value = "Request parameters as JSON object with data described by Connect Client Registration Specification. ", required = true)
            String requestParams,
            @QueryParam("client_id")
            @ApiParam(value = "Client ID that identifies client that must be updated by this request.", required = true)
            String clientId,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest httpRequest,
            @Context SecurityContext securityContext);

    /**
     * This operation retrieves the Client Metadata for a previously registered client.
     *
     * @param clientId        Unique Client identifier.
     * @param securityContext An injectable interface that provides access to security related information.
     * @return response
     */
    @GET
    @Path("/register")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
                value = "Reads client info.",
                notes = "Reads client info.",
                response = Response.class,
                responseContainer = "JSON"
        )
    Response requestClientRead(
            @QueryParam("client_id")
            @ApiParam(value = "Client ID that identifies client.", required = true)
            String clientId,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest httpRequest,
            @Context SecurityContext securityContext);
}