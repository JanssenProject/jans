/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.configapi.plugin.shibboleth.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;

import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginAppConf;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginConfiguration;
import io.jans.configapi.plugin.shibboleth.service.ShibbolethConfigService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
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

import com.github.fge.jsonpatch.JsonPatchException;

@Path(Constants.SHIBBOLETH_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShibbolethPluginConfigResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    ShibbolethConfigService shibbolethConfigService;

    @Operation(summary = "Get Shibboleth Plugin configuration properties", description = "Gets Shibboleth Plugin configuration properties", operationId = "get-shibboleth-plugin-config", tags = {
            "Shibboleth - Plugin Configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shibboleth Plugin configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethPluginConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_CONFIG_ADMIN_ACCESS,
                    Constants.SHIBBOLETH_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getShibbolethPluginAppConf() {
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethConfigService.find();
        return Response.ok(shibbolethPluginConfiguration).build();
    }

    @Operation(summary = "Update Shibboleth Plugin properties", description = "Update Shibboleth Plugin properties", operationId = "update-shibboleth-plugin-config", tags = {
            "Shibboleth - Plugin Configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_ADMIN_ACCESS }) })
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethPluginConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/shibboleth-plugin-config-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethPluginConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateShibbolethPluginConfiguration(
            @Valid ShibbolethPluginConfiguration shibbolethPluginConfiguration) {
        logger.info("Update shibbolethPluginConfiguration():{}", shibbolethPluginConfiguration);
        ShibbolethPluginAppConf shibbolethPluginAppConf = shibbolethConfigService.findShibbolethPluginConfiguration();
        shibbolethPluginAppConf.setDynamicConf(shibbolethPluginConfiguration);
        shibbolethConfigService.mergeShibbolethPluginAppConf(shibbolethPluginAppConf);
        shibbolethPluginConfiguration = shibbolethConfigService.find();
        logger.info("Post update - shibbolethPluginConfiguration:{}", shibbolethPluginConfiguration);
        return Response.ok(shibbolethPluginConfiguration).build();

    }

    @Operation(summary = "Partially modifies Shibboleth Plugin configuration properties.", description = "Partially modifies Shibboleth Plugin Configuration properties.", operationId = "patch-shibboleth-plugin-config", tags = {
            "Shibboleth - Plugin Configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_CONFIG_ADMIN_ACCESS }) })

    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethPluginConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/shibboleth-plugin-config-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethPluginConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchShibbolethPluginConfiguration(@NotNull String jsonPatchString)
            throws JsonPatchException, IOException {
        logger.info("ShibbolethPluginConfiguration - jsonPatchString:{} ", jsonPatchString);
        ShibbolethPluginAppConf shibbolethPluginAppConf = shibbolethConfigService.findShibbolethPluginConfiguration();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = Jackson.applyPatch(jsonPatchString,
                shibbolethPluginAppConf.getDynamicConf());
        shibbolethPluginAppConf.setDynamicConf(shibbolethPluginConfiguration);
        shibbolethConfigService.mergeShibbolethPluginAppConf(shibbolethPluginAppConf);
        shibbolethPluginConfiguration = shibbolethConfigService.find();
        logger.info("Post patch - shibbolethPluginConfiguration:{}", shibbolethPluginConfiguration);
        return Response.ok(shibbolethPluginConfiguration).build();
    }
}