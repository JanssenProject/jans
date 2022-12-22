/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.client;

import io.jans.fido2.model.u2f.protocol.RegisterRequestMessage;
import io.jans.fido2.model.u2f.protocol.RegisterStatus;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

/**
 * Еhe endpoint allows to start and finish U2F registration process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public interface RegistrationRequestService {

    @GET
    @Produces({"application/json"})
    RegisterRequestMessage startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId);

    @GET
    @Produces({"application/json"})
    RegisterRequestMessage startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId, @QueryParam("enrollment_code") String enrollmentCode);

    @POST
    @Produces({"application/json"})
    RegisterStatus finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString);

}