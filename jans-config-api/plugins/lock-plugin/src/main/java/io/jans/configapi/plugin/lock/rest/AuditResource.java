/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.lock.service.LockConfigService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
import io.jans.model.JansAttribute;
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

import io.jans.configapi.plugin.lock.model.stat.*;
import io.jans.configapi.plugin.lock.service.AuditService;

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = HealthEntry.class), examples = @ExampleObject(name = "Response example", value = "example/lock/audit/health.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_HEALTH_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.HEALTH)
    public Response postHealthData(@Valid HealthEntry healthEntry) {

        logger.info("Save Health Data - healthEntry:{}", healthEntry);
        healthEntry = auditService.addHealthEntry(healthEntry);

        logger.info("Afer saving healthEntry():{}", healthEntry);
        return Response.status(Response.Status.CREATED).entity(healthEntry).build();

    }

    @Operation(summary = "Save log data", description = "Save log data", operationId = "save-log-data", tags = {
            "Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_LOG_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LogEntry.class), examples = @ExampleObject(name = "Response example", value = "example/lock/audit/log.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_LOG_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.LOG)
    public Response postLogData(@Valid LogEntry logEntry) {

        logger.info("Save - logEntry:{}", logEntry);
        logEntry = auditService.addLogData(logEntry);

        logger.info("Afer saving logEntry():{}", logEntry);
        return Response.status(Response.Status.CREATED).entity(logEntry).build();

    }

    @Operation(summary = "Save telemetry data", description = "Save telemetry data", operationId = "save-telemetry-data", tags = {
            "Lock - Audit" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_TELEMETRY_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TelemetryEntry.class), examples = @ExampleObject(name = "Response example", value = "example/lock/audit/telemetry.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.LOCK_TELEMETRY_WRITE_ACCESS }, groupScopes = {})
    @Path(Constants.TELEMETRY)
    public Response postTelemetryData(@Valid TelemetryEntry telemetryEntry) {

        logger.info("Save telemetryEntry():{}", telemetryEntry);
        telemetryEntry = auditService.addTelemetryData(telemetryEntry);

        logger.info("Afer saving telemetryEntry():{}", telemetryEntry);
        return Response.status(Response.Status.CREATED).entity(telemetryEntry).build();

    }

}