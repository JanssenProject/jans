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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

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

    @Operation(summary = "Returns current session", description = "Returns current session", operationId = "get-sessions", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS , "revoke_session" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SessionId.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_READ_ACCESS } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAllSessions() {
        final List<SessionId> sessions = sessionService.getSessions();
        logger.debug("sessions:{}", sessions);
        return Response.ok(sessions).build();
    }

    @Operation(summary = "Revoke all sessions by userDn", description = "Revoke all sessions by userDn", operationId = "revoke-user-session", tags = {
            "Auth - Session Management" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS,
                    ApiAccessConstants.JANS_AUTH_REVOKE_SESSION }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_SESSION_DELETE_ACCESS,
            ApiAccessConstants.JANS_AUTH_REVOKE_SESSION } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.USERDN_PATH)
    public Response getAppConfiguration(@Parameter(description = "User domain name") @PathParam(ApiConstants.USERDN) @NotNull String userDn) {
        logger.debug("userDn:{}", userDn);
        sessionService.revokeSession(userDn);
        return Response.ok().build();
    }

}
