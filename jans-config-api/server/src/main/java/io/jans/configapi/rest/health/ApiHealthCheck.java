/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.health;

import io.jans.configapi.core.model.HealthStatus;
import io.jans.configapi.core.model.Status;
import io.jans.configapi.model.status.StatsData;
import io.jans.configapi.rest.resource.auth.ConfigBaseResource;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import java.util.ArrayList;

@Path(ApiConstants.HEALTH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiHealthCheck extends ConfigBaseResource {
    
    @Inject
    Logger logger;

        
    @Inject
    ConfigurationService configurationService;

    @Operation(summary = "Returns application health status", description = "Returns application health status", operationId = "get-config-health", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    public Response getHealthResponse() {
        logger.debug("Api Health Check - Entry");
        
        HealthStatus healthStatus = new HealthStatus();
        healthStatus.setStatus("UP");
        ArrayList<Status> satusList = new ArrayList<>();
        
        // liveness
        Status liveness = new Status();
        liveness.setName("jans-config-api liveness");
        liveness.setStatus("UP");
        satusList.add(liveness);
        
        // readiness
        Status readiness = new Status();
        readiness.setName("jans-config-api readiness");
         
        try {
            checkDatabaseConnection();
            readiness.setStatus("UP");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            readiness.setStatus("DOWN");
            readiness.setError("e.getMessage()");
            logger.debug("Api Health Check - Error - status2:{}",readiness);
        }
        satusList.add(readiness);

        healthStatus.setChecks(satusList);
        logger.debug("ApiHealthCheck::getHealthResponse() - satusList:{}",satusList);
        return Response.ok(satusList).build();
    }


    @Operation(summary = "Returns application liveness status", description = "Returns application liveness status", operationId = "get-config-health-live", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Status.class))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.LIVE)
    public Response getLivenessResponse() {
        logger.debug("ApiHealthCheck::getLivenessResponse() - Entry");
        Status liveness = new Status();
        liveness.setName("jans-config-api liveness");
        liveness.setStatus("UP");
  
        logger.debug("ApiHealthCheck::getLivenessResponse() - liveness:{}",liveness);
        return Response.ok(liveness).build();
    }

    @Operation(summary = "Returns application readiness status", description = "Returns application readiness status", operationId = "get-config-health-ready", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Status.class))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.READY)
    public Response getReadinessResponse() {
        logger.debug("ApiHealthCheck::getReadinessResponse() - Entry");
     // readiness
        Status readiness = new Status();
        readiness.setName("jans-config-api readiness");
        try {
            checkDatabaseConnection();
            readiness.setStatus("UP");
            logger.debug("Api Health Readiness - Success - readiness:{}",readiness);
            return Response.ok(readiness).build();
        } catch (Exception e) {
            readiness.setStatus("DOWN");
            readiness.setError("e.getMessage()");
            logger.debug("Api Health Readiness - Error - readiness:{}",readiness);
            return Response.ok(readiness).build();
        }
    }

    @Operation(summary = "Returns application server status", description = "Returns application server status", operationId = "get-server-stat", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatsData.class))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.SERVER_STAT)
    public Response getServerStat() {
        logger.debug("Server Stat - Entry");
        StatsData statsData = configurationService.getStatsData();
        logger.debug("Server Stat - statsData:{}",statsData);
        return Response.ok(statsData).build();

    }

    private void checkDatabaseConnection() {
        configurationService.findConf();
    }
}
