package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(ApiConstants.JANS_AUTH + ApiConstants.HEALTH)
public class HealthCheckResource extends ConfigBaseResource {

    private static final String HEALTH_CHECK_URL = "/jans-auth/sys/health-check";

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;

    @Operation(summary = "Returns auth server health status", description = "Returns auth server health status", operationId = "get-auth-server-health", tags = {
            "Auth Server Health - Check" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/health/health.json"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealthCheckStatus() {
        String url = getIssuer() + HEALTH_CHECK_URL;
        JsonNode jsonNode = authService.getHealthCheckResponse(url);
        logger.debug("StatResource::getUserStatistics() - jsonNode:{} ", jsonNode);
        return Response.ok(jsonNode).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
