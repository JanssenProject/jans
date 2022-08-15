/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.common.model.session.SessionId;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.configapi.service.auth.SessionService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.SESSION)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SessionResource extends ConfigBaseResource {
    
    @Inject
    Logger log;

    @Inject
    SessionService sessionService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS })
    public Response getAllSessions() {
     
        final List<SessionId> sessions = sessionService.getAllSessions();
        logger.error("sessions:{}", sessions);
        return Response.ok(sessions).build(); 
    }

    
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS, ApiAccessConstants.JANS_AUTH_REVOKE_SESSION })
    @Path(ApiConstants.USERID_PATH)
    public Response getAppConfiguration(@PathParam(ApiConstants.USERID) @NotNull String userId) {
        logger.error("userId:{}", userId);
        sessionService.revokeSession(userId);        
        return Response.ok("OK").build(); 
    }
    
}
