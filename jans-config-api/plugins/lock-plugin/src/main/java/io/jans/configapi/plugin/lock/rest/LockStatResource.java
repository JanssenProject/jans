/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;



import com.fasterxml.jackson.databind.JsonNode;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.lock.service.LockService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Path(Constants.LOCK_STAT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LockStatResource extends BaseResource {

    private static final String STAT_URL = "/jans-lock/v1/internal/stat";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    LockService lockService;

    @Operation(summary = "Provides basic statistic", description = "Provides basic statistic", operationId = "get-lock-stat", tags = {
            "Statistics" }, security = @SecurityRequirement(name = "oauth2", scopes = { Constants.LOCK_READ_ACCESS,
                    ApiAccessConstants.JANS_STAT }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stats", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonNode.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.LOCK_READ_ACCESS, ApiAccessConstants.JANS_STAT }, groupScopes = {}, superScopes = {
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(
            @Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "Month for which the stat report is to be fetched. The parameter is mandatory if start_month and end_month parameters are not present.") @QueryParam(value = "month") String month,
            @Parameter(description = "Start-Month for which the stat report is to be fetched") @QueryParam(value = "start_month") String startMonth,
            @Parameter(description = "End-Month for which the stat report is to be fetched") @QueryParam(value = "end_month") String endMonth,
            @Parameter(description = "Report format") @QueryParam(value = "format") String format) {
        if (StringUtils.isBlank(format)) {
            format = "";
        }
        JsonNode jsonNode = null;
        try {
            if (logger.isInfoEnabled()) {
                logger.info(
                        "LockStatResource::getStatistics() - authorization:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                        escapeLog(authorization), escapeLog(month), escapeLog(startMonth), escapeLog(endMonth), escapeLog(format));
            }
            
            logger.error(
                    "LockStatResource::getStatistics() - authorization:{}, month:{},  startMonth:{}, endMonth:{}, format:{}",
                    authorization, month, startMonth, endMonth, format);
            String url = getIssuer() + STAT_URL;
            jsonNode = this.lockService.getStat(url, authorization, month, startMonth, endMonth, format);
            logger.error("StatResource::getUserStatistics() - jsonNode:{} ", jsonNode);
        } catch (Exception ex) {
            logger.error(" Error while fetching lock stat is", ex);
            throwBadRequestException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    private String getIssuer() {
        return configurationService.find().getIssuer();
    }

}