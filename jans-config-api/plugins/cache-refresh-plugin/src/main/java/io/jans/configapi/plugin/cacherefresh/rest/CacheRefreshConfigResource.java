/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.cacherefresh.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.cacherefresh.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import  io.jans.configapi.plugin.cacherefresh.service.CacheRefreshService;
import io.jans.configapi.plugin.cacherefresh.model.config.Conf;
import io.jans.configapi.plugin.cacherefresh.model.config.CacheRefreshConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

import org.slf4j.Logger;

@Path(Constants.CACHEREFRESH_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheRefreshConfigResource extends BaseResource {

    private static final String CACHEREFRESH_CONFIGURATION = "cacheRefreshConfiguration";

    @Inject
    Logger logger;

    @Inject
    CacheRefreshService cacheRefreshService;

    @Operation(summary = "Gets Cache Refresh configuration.", description = "Gets Cache Refresh configuration.", operationId = "get-properties-cache-refresh", tags = {
            "Cache Refresh - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHEREFRESH_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CacheRefreshConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CACHEREFRESH_CONFIG_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHEREFRESH_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getCacheRefreshConfiguration() {
        CacheRefreshConfiguration appConfiguration = this.cacheRefreshService.find();
        logger.debug("Cache Refresh details appConfiguration():{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Updates Cache Refresh configuration properties.", description = "Updates Cache Refresh configuration properties.", operationId = "put-properties-cache-refresh", tags = {
            "Cache Refresh - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHEREFRESH_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "CacheRefreshConfiguration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CacheRefreshConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CacheRefreshConfiguration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CacheRefreshConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.CACHEREFRESH_CONFIG_WRITE_ACCESS } , groupScopes = {}
    , superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateCacheRefreshConfiguration(@NotNull CacheRefreshConfiguration appConfiguration) {
        logger.debug("Cache Refresh details to be updated - appConfiguration:{} ", appConfiguration);
        checkResourceNotNull(appConfiguration, CACHEREFRESH_CONFIGURATION);
        this.cacheRefreshService.merge(appConfiguration);
        appConfiguration = this.cacheRefreshService.find();
        return Response.ok(appConfiguration).build();
    }

}