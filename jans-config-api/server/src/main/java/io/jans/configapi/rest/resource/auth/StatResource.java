package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AuthService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@Path(ApiConstants.STATISTICS)
public class StatResource extends ConfigBaseResource {

    private final String statUrl = "/jans-auth/restv1/internal/stat";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AuthService authService;

    @Operation(summary = "Provides server with basic statistic", description = "Provides server with basic statistic", operationId = "get-stat", tags = {
            "Statistics - User" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.STATS_USER_READ_ACCESS, ApiAccessConstants.JANS_STAT  }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonNode.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.STATS_USER_READ_ACCESS, ApiAccessConstants.JANS_STAT } , groupScopes = {}, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("Authorization") String authorization,
            @QueryParam(value = "month") String month, @QueryParam(value = "start_month") String startMonth,
            @QueryParam(value = "end_month") String endMonth, @QueryParam(value = "format") String format) {
        if (StringUtils.isBlank(format)) {
            format = "";
        }
        String url = getIssuer() + this.statUrl;
        JsonNode jsonNode = this.authService.getStat(url, authorization, month, startMonth, endMonth, format);
        logger.trace("StatResource::getUserStatistics() - jsonNode:{} ", jsonNode);
        return Response.ok(jsonNode.get("response")).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}
