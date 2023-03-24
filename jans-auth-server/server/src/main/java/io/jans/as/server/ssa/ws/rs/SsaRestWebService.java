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

/**
 * Interface to handle all SSA REST web services.
 */
public interface SsaRestWebService {

    /**
     * Create SSA for the organization with "expiration" (optional).
     *
     * @param requestParams Valid json
     * @param httpRequest   Http request object
     * @return {@link Response} with status {@code 201 (Created)} and with body the ssa token,
     * or with status {@code 401 (Unauthorized)} if unauthorized access request,
     * or with status {@code 500 (Internal Server Error)} if internal error occurred.
     */
    @POST
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response create(
            String requestParams,
            @Context HttpServletRequest httpRequest
    );

    /**
     * Get list of SSA based on "jti" or "org_id" filter.
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return the {@link Response} with status {@code 200 (Ok)} and with body the ssa list,
     * or with status {@code 401 (Unauthorized)} if unauthorized access request,
     * or with status {@code 500 (Internal Server Error)} if internal error occurred.
     */
    @GET
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response get(
            @QueryParam("jti") String jti,
            @QueryParam("org_id") String orgId,
            @Context HttpServletRequest httpRequest
    );

    /**
     * Validate existing active SSA based on "jti".
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200 (Ok)} if is was validated successfully,
     * or with status {@code 401 (Unauthorized)} if unauthorized access request,
     * or with status {@code 500 (Internal Server Error)} if internal error occurred.
     */
    @HEAD
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response validate(@HeaderParam("jti") String jti);

    /**
     * Revokes existing active SSA based on "jti" or "org_id".
     *
     * @param jti         Unique identifier
     * @param orgId       Organization ID
     * @param httpRequest Http request
     * @return the {@link Response} with status {@code 200 (Ok)} if it was revoked successfully,
     * or with status {@code 401 (Unauthorized)} if unauthorized access request,
     * or with status {@code 500 (Internal Server Error)} if internal error occurred.
     */
    @DELETE
    @Path("/ssa")
    @Produces({MediaType.APPLICATION_JSON})
    Response revoke(
            @QueryParam("jti") String jti,
            @QueryParam("org_id") String orgId,
            @Context HttpServletRequest httpRequest
    );

    /**
     * Get JWT from existing active SSA based on "jti".
     *
     * @param jti Unique identifier
     * @return {@link Response} with status {@code 200 (Ok)} and the body containing JWT of SSA.
     * or with status {@code 401} if this functionality is not enabled, request has to have at least scope "ssa.admin",
     * or with status {@code 422} if the SSA does not exist, is expired or used,
     * or with status {@code 500} in case an uncontrolled error occurs when processing the method.
     */
    @GET
    @Path("/ssa/jwt")
    @Produces({MediaType.APPLICATION_JSON})
    Response getSsaJwtByJti(
            @QueryParam("jti") String jti
    );
}