package org.xdi.oxauth.clientinfo.ws.rs;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for Client Info REST web services
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
@Path("/oxauth")
public interface ClientInfoRestWebService {

    @GET
    @Path("/clientinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestUserInfoGet(
            @QueryParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);

    @POST
    @Path("/clientinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestUserInfoPost(
            @FormParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);
}