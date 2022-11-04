/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public interface SsaRestWebService {

    @POST
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response create(
            String requestParams,
            @Context HttpServletRequest httpRequest
    );

    @GET
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response get(
            @QueryParam("software_roles") Boolean softwareRoles,
            @QueryParam("jti") String jti,
            @QueryParam("org_id") Long orgId,
            @Context HttpServletRequest httpRequest
    );

    @HEAD
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response validate(@HeaderParam("jti") String jti);

    @DELETE
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response revoke(
            @QueryParam("jti") String jti,
            @QueryParam("org_id") Long orgId,
            @Context HttpServletRequest httpRequest
    );
}