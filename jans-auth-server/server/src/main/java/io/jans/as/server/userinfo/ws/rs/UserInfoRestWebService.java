/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.userinfo.ws.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for User Info REST web services
 *
 * @author Javier Rojas Blum
 * @version September 7, 2017
 */
public interface UserInfoRestWebService {

    @GET
    @Path("/userinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestUserInfoGet(
            @QueryParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext);

    @POST
    @Path("/userinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestUserInfoPost(
            @FormParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext);
}