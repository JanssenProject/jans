/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.fido.u2f;

import io.jans.as.model.fido.u2f.protocol.AuthenticateRequestMessage;
import io.jans.as.model.fido.u2f.protocol.AuthenticateStatus;

import javax.ws.rs.*;

/**
 * The endpoint allows to start and finish U2F authentication process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public interface AuthenticationRequestService {

    @GET
    @Produces({"application/json"})
    public AuthenticateRequestMessage startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId);

    @POST
    @Produces({"application/json"})
    public AuthenticateStatus finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString);

}