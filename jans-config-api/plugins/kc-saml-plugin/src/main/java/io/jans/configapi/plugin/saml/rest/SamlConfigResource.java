/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.saml.model.config.SamlAppConfiguration;
import io.jans.configapi.plugin.saml.model.config.SamlConf;
import io.jans.configapi.plugin.saml.service.SamlConfigService;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;

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

@Path(Constants.SAML_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlConfigResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    SamlConfigService samlConfigService;

    @Operation(summary = "Gets SAML configuration properties", description = "Gets SAML configuration properties", operationId = "get-saml-properties", tags = {
            "SAML - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SamlAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.SAML_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getSamlConfiguration() {
        SamlAppConfiguration samlConfiguration = samlConfigService.find();
        logger.info("SAML details samlConfiguration():{}", samlConfiguration);
        return Response.ok(samlConfiguration).build();
    }

    @Operation(summary = "Update SAML configuration properties", description = "Update SAML configuration properties", operationId = "put-saml-properties", tags = {
            "SAML - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SamlAppConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/saml/config/saml-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SamlAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT    @ProtectedApi(scopes = { Constants.SAML_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateSamlConfiguration(@Valid SamlAppConfiguration samlConfiguration) {
        logger.info("Update SAML details samlConfiguration():{}", samlConfiguration);
        SamlConf conf = samlConfigService.findSamlConf();
        conf.setDynamicConf(samlConfiguration);
        samlConfigService.mergeSamlConfig(conf);
        samlConfiguration = samlConfigService.find();
        logger.info("SAML post update - samlConfiguration:{}", samlConfiguration);
        return Response.ok(samlConfiguration).build();

    }

    @Operation(summary = "Partially modifies SAML configuration properties.", description = "Partially modifies SAML Configuration properties.", operationId = "patch-saml-properties", tags = {
            "SAML - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/saml/config/saml-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SamlAppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { Constants.SAML_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchSamlConfiguration(@NotNull String jsonPatchString) throws JsonPatchException, IOException {
        logger.info("Config API - jsonPatchString:{} ", jsonPatchString);
        SamlConf conf = samlConfigService.findSamlConf();
        SamlAppConfiguration samlConfiguration = Jackson.applyPatch(jsonPatchString, conf.getDynamicConf());
        conf.setDynamicConf(samlConfiguration);
        samlConfigService.mergeSamlConfig(conf);
        samlConfiguration = samlConfigService.find();
        logger.info("SAML post patch - samlConfiguration:{}", samlConfiguration);
        return Response.ok(samlConfiguration).build();
    }
}