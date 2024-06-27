/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;


import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.lock.service.LockConfigService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

import org.slf4j.Logger;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.plugin.lock.model.stat.TelemetryEntry;

@Path(Constants.LOCK_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    LockConfigService lockConfigService;

    @Operation(summary = "Save telemetry data", description = "Save telemetry data", operationId = "save-telemetry-data", tags = {
            "Lock - Telemetry" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_TELEMETRY_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_WRITE_ACCESS }, groupScopes = { })
    @Path(Constants.TELEMETRY)
    public Response getTelemetryData(@Valid TelemetryEntry telemetryEntry) {
       
        logger.info("Post telemetryEntry():{}", telemetryEntry);
        return Response.ok(telemetryEntry).build();
        
    }

   
}