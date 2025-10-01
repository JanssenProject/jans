/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.ApiConf;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.conf.ConfigApiService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import org.slf4j.Logger;

@Path(ApiConstants.API_CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigApiResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigApiService configApiService;

    @Operation(summary = "Gets config-api configuration properties.", description = "Gets config-api configuration properties.", operationId = "get-config-api-properties", tags = {
            "Configuration – Config API" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CONFIG_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAppConfiguration() {
        ApiAppConfiguration appConfiguration = configApiService.find();
        log.debug("Config API Configuration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Partially modifies config-api configuration properties.", description = "Partially modifies config-api Configuration properties.", operationId = "patch-config-api-properties", tags = {
            "Configuration – Config API" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/config/config-api-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String jsonPatchString)
            throws JsonPatchException, IOException {
        log.debug("Config API - jsonPatchString:{} ", jsonPatchString);
        ApiConf conf = configApiService.findApiConf();
        ApiAppConfiguration appConfiguration = Jackson.applyPatch(jsonPatchString, conf.getDynamicConf());
        conf.setDynamicConf(appConfiguration);
        configApiService.merge(conf);
        appConfiguration = configApiService.find();
        log.debug("Config-api post patch - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

}
