/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.lock.model.stat.*;
import io.jans.configapi.plugin.lock.service.AuditService;
import io.jans.configapi.plugin.lock.util.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

@Path(Constants.AUDIT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    AuditService auditService;

    @Operation(summary = "Save health data", description = "Save health data", operationId = "save-health-data", tags = {
            "Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_HEALTH_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_HEALTH_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.HEALTH)
    public Response postHealthData(@Valid HealthEntry healthEntry) {
        logger.debug("Save Health Data - healthEntry:{}", healthEntry);
        auditService.addHealthEntry(healthEntry);
        return Response.status(Response.Status.OK).build();

    }

    @Operation(summary = "Save log data", description = "Save log data", operationId = "save-log-data", tags = {
            "Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_LOG_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_LOG_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.LOG)
    public Response postLogData(@Valid LogEntry logEntry) {
        logger.debug("Save - logEntry:{}", logEntry);
        auditService.addLogData(logEntry);
        return Response.status(Response.Status.OK).build();

    }

    @Operation(summary = "Save telemetry data", description = "Save telemetry data", operationId = "save-telemetry-data", tags = {
            "Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_TELEMETRY_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.TELEMETRY)
    public Response postTelemetryData(@Valid TelemetryEntry telemetryEntry) {
        logger.debug("Save telemetryEntry():{}", telemetryEntry);
        auditService.addTelemetryData(telemetryEntry);
        return Response.status(Response.Status.OK).build();

    }

}