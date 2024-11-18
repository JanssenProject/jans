/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.health;

import static io.jans.as.model.util.Util.escapeLog;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.model.HealthStatus;
import io.jans.configapi.core.model.Status;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.status.StatsData;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.status.StatusCheckerTimer;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Map;

@Path(ApiConstants.HEALTH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiHealthCheck  {
    
    @Inject
    Logger logger;

        
    @Inject
    ConfigurationService configurationService;
    
    @Inject
    StatusCheckerTimer statusCheckerTimer;

    @Operation(summary = "Returns application health status", description = "Returns application health status", operationId = "get-config-health", tags = {
    "Health - Check" })
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = HealthStatus.class)))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    public Response getHealthResponse() {
        logger.debug("Api Health Check - /health/");
        
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
        logger.info("ApiHealthCheck::/health/live");
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
        logger.info("ApiHealthCheck::/health/ready");
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
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatsData.class), examples = @ExampleObject(name = "Response json example", value = "example/health/server-stat.json"))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.SERVER_STAT)
    public Response getServerStat() {
        logger.debug("Server Stat - Entry");
        StatsData statsData = statusCheckerTimer.getServerStatsData();
        logger.debug("Server Stat - statsData:{}",statsData);
        return Response.ok(statsData).build();

    }

    @Operation(summary = "Returns application version", description = "Returns application version", operationId = "get-app-version", tags = {
    "Health - Check" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            ApiAccessConstants.APP_VERSION_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.APP_VERSION_READ_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.APP_VERSION)
    public Response getApplicationVersion(@Parameter(description = "artifact name for which version is requied else ALL") @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.ARTIFACT) String artifact) {
        logger.debug("Application Version - artifact:{}", artifact);
        return Response.ok(statusCheckerTimer.getAppVersionData(artifact)).build();
    }

    @Operation(summary = "Fetch service status", description = "Fetch service status", operationId = "get-service-status", tags = {
            "Health - Check" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.APP_DATA_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @ExampleObject(name = "Response json example", value = "example/health/service-status.json"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.APP_DATA_READ_ACCESS }, groupScopes = {}, superScopes = {ApiAccessConstants.SUPER_ADMIN_READ_ACCESS})
    @Path(ApiConstants.SERVICE_STATUS_PATH)
    public Response getServiceStatus(
            @Parameter(description = "Service name to check status") @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.JANS_SERVICE_NAME) String service) {
        if (logger.isInfoEnabled()) {
            logger.info("Fetch ServiceStatus info - service:{}", escapeLog(service));
        }
        
        Map<String, String> serviceStatus = statusCheckerTimer.getServiceStatus(service);
        logger.debug("serviceStatus:{}", serviceStatus);
        return Response.ok(serviceStatus).build();
    }

    private void checkDatabaseConnection() {
        configurationService.findConf();
    }
}
