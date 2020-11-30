/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.ws;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * It is used to process CIBA callbacks for ping and push modes.
 */
public interface CibaClientNotificationEndpoint {

    @POST
    @Path("/cb")
    @Produces({MediaType.APPLICATION_JSON})
    Response processCallback(
            @HeaderParam("Authorization") String authorization,
            String requestParams,
            @Context HttpServletRequest request,
            @Context SecurityContext securityContext);

}