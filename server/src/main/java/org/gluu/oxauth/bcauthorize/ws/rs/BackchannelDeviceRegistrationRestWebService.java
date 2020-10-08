/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

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