/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
public interface BackchannelDeviceRegistrationRestWebService {

    @POST
    @Path("/bc-deviceRegistration")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestBackchannelDeviceRegistrationPost(
            @FormParam("id_token_hint") String idTokenHint,
            @FormParam("device_registration_token") String deviceRegistrationToken,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext
    );
}