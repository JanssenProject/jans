/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.fido.u2f;

import io.jans.as.model.fido.u2f.protocol.RegisterRequestMessage;
import io.jans.as.model.fido.u2f.protocol.RegisterStatus;

import javax.ws.rs.*;

/**
 * Еhe endpoint allows to start and finish U2F registration process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public interface RegistrationRequestService {

    @GET
    @Produces({"application/json"})
    public RegisterRequestMessage startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId);

    @GET
    @Produces({"application/json"})
    public RegisterRequestMessage startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId, @QueryParam("enrollment_code") String enrollmentCode);

    @POST
    @Produces({"application/json"})
    public RegisterStatus finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString);

}