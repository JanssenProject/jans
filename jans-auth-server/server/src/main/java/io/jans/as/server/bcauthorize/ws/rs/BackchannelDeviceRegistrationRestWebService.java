/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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