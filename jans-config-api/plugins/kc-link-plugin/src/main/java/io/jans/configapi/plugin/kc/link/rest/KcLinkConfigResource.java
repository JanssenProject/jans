/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.kc.link.rest;


import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.keycloak.link.model.config.AppConfiguration;
import io.jans.keycloak.link.model.config.Conf;
import io.jans.configapi.plugin.kc.link.service.KcLinkConfigService;
import io.jans.configapi.plugin.kc.link.util.Constants;
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

@Path(Constants.KC_LINK_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KcLinkConfigResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    KcLinkConfigService kcLinkConfigService;

    @Operation(summary = "Gets KC Link configuration properties", description = "Gets KC Link configuration properties", operationId = "get-kc-link-properties", tags = {
            "KC Link - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_LINK_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.KC_LINK_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.KC_LINK_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getkcLinkConf() {
       
        AppConfiguration kcLinkConfiguration = kcLinkConfigService.find();
        logger.info("KC Link details kcLinkConfiguration():{}", kcLinkConfiguration);
        if(kcLinkConfiguration==null) {
            throwInternalServerException("It seems Kc Link module is not setup, kindly check.");
        }
        return Response.ok(kcLinkConfiguration).build();
        
    }

    @Operation(summary = "Update KC Link configuration properties", description = "Update KC Link configuration properties", operationId = "put-kc-link-properties", tags = {
            "KC Link - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_LINK_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/kc-link/config/kc-link-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT    @ProtectedApi(scopes = { Constants.KC_LINK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updatekcLinkConf(@Valid AppConfiguration kcLinkAppConf) {
        logger.info("Update KC Link conf details kcLinkAppConf():{}", kcLinkAppConf);
        Conf conf = kcLinkConfigService.findKcLinkConf();
        logger.info("KC Link conf:{} ", conf);
        if(conf==null) {
            throwInternalServerException("It seems Kc Link module is not setup, kindly check.");
        }
       
        conf.setDynamic(kcLinkAppConf);
        kcLinkConfigService.mergeKcLinkConfig(conf);
        kcLinkAppConf = kcLinkConfigService.find();
        logger.info("KC Link conf, post update - kcLinkAppConf:{}", kcLinkAppConf);
        return Response.ok(kcLinkAppConf).build();

    }

    @Operation(summary = "Partially modifies KC Link configuration properties.", description = "Partially modifies KC Link configuration properties.", operationId = "patch-kc-link-properties", tags = {
            "KC Link - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_LINK_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/kc-link/config/kc-link-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { Constants.KC_LINK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchkcLinkConf(@NotNull String jsonPatchString) throws JsonPatchException, IOException {
        logger.info("KC Link Config - jsonPatchString:{} ", jsonPatchString);
        Conf conf = kcLinkConfigService.findKcLinkConf();
        logger.info("KC Link conf:{} ", conf);
        if(conf==null) {
            throwInternalServerException("It seems Kc Link module is not setup, kindly check.");
        }
       
        AppConfiguration kcLinkAppConf = Jackson.applyPatch(jsonPatchString, conf.getDynamic());
        logger.info("KC Link conf details kcLinkAppConf():{}", kcLinkAppConf);
        
        conf.setDynamic(kcLinkAppConf);
        kcLinkConfigService.mergeKcLinkConfig(conf);
        kcLinkAppConf = kcLinkConfigService.find();
        logger.info("KC KC Link post patch - kcLinkAppConf:{}", kcLinkAppConf);
        return Response.ok(kcLinkAppConf).build();
    }
}