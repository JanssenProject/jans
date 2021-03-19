/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.clientinfo.ws.rs;

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

/**
 * Provides interface for Client Info REST web services
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
public interface ClientInfoRestWebService {

    @GET
    @Path("/clientinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestClientInfoGet(
            @QueryParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);

    @POST
    @Path("/clientinfo")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestClientInfoPost(
            @FormParam("access_token") String accessToken,
            @HeaderParam("Authorization") String authorization,
            @Context SecurityContext securityContext);
}