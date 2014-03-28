package org.xdi.oxauth.userinfo.ws.rs;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum Date: 11.29.2011
 */
@Path("/oxauth")
public interface UserInfoRestWebService {

    @GET
    @Path("/userinfo")
    Response requestUserInfoGet(
            @QueryParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);

    @POST
    @Path("/userinfo")
    Response requestUserInfoPost(
            @FormParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);
}