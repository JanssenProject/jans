/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.rest;


import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.lock.service.LockConfigService;
import io.jans.configapi.plugin.lock.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.config.Conf;
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

@Path(Constants.LOCK_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LockConfigResource extends BaseResource {
    
    private static final String CONFIG_NULL_ERR_MSG = "It seems Lock module is not setup, kindly check."; 

    @Inject
    Logger logger;

    @Inject
    LockConfigService lockConfigService;

    @Operation(summary = "Gets Lock configuration properties", description = "Gets Lock configuration properties", operationId = "get-lock-properties", tags = {
            "Lock - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_READ_ACCESS }, groupScopes = {
            Constants.LOCK_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getlockConf() {
       
        AppConfiguration lockConfiguration = lockConfigService.find();
        logger.info("Lock details lockConfiguration():{}", lockConfiguration);
        if(lockConfiguration==null) {
            throwInternalServerException(CONFIG_NULL_ERR_MSG);
        }
        return Response.ok(lockConfiguration).build();
        
    }

    @Operation(summary = "Update Lock configuration properties", description = "Update Lock configuration properties", operationId = "put-lock-properties", tags = {
            "Lock - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class), examples = @ExampleObject(name = "Request example", value = "example/lock/config/lock-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updatelockConf(@Valid AppConfiguration lockAppConf) {
        logger.info("Update Lock conf details lockAppConf():{}", lockAppConf);
        Conf conf = lockConfigService.findLockConf();
        logger.info("Lock conf:{} ", conf);
        if(conf==null) {
            throwInternalServerException(CONFIG_NULL_ERR_MSG);
        }
       
        conf.setDynamic(lockAppConf);
        lockConfigService.mergeLockConfig(conf);
        lockAppConf = lockConfigService.find();
        logger.info("Lock conf, post update - lockAppConf:{}", lockAppConf);
        return Response.ok(lockAppConf).build();

    }

    @Operation(summary = "Partially modifies Lock configuration properties.", description = "Partially modifies Lock configuration properties.", operationId = "patch-lock-properties", tags = {
            "Lock - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.LOCK_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/lock/config/lock-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { Constants.LOCK_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchlockConf(@NotNull String jsonPatchString) throws JsonPatchException, IOException {
        logger.info("Lock Config - jsonPatchString:{} ", jsonPatchString);
        Conf conf = lockConfigService.findLockConf();
        logger.info("Lock conf:{} ", conf);
        if(conf==null) {
            throwInternalServerException(CONFIG_NULL_ERR_MSG);
        }
       
        AppConfiguration lockAppConf = Jackson.applyPatch(jsonPatchString, conf.getDynamic());
        logger.info("Lock conf details lockAppConf():{}", lockAppConf);
        
        conf.setDynamic(lockAppConf);
        lockConfigService.mergeLockConfig(conf);
        lockAppConf = lockConfigService.find();
        logger.info("Lock post patch - lockAppConf:{}", lockAppConf);
        return Response.ok(lockAppConf).build();
    }
}