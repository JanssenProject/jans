/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.core.model.PersistenceConfiguration;
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

@Path(ApiConstants.JANS_AUTH + ApiConstants.CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthConfigResource extends ConfigBaseResource {

    private static final String AGAMACONFIGURATION = "agamaConfiguration";

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Operation(summary = "Gets all Jans authorization server configuration properties.", description = "Gets all Jans authorization server configuration properties.", operationId = "get-properties", tags = {
            "Configuration – Properties" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS }, superScopes = {
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("AuthConfigResource::getAppConfiguration() appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Partially modifies Jans authorization server Application configuration properties.", description = "Partially modifies Jans authorization server AppConfiguration properties.", operationId = "patch-properties", tags = {
            "Configuration – Properties" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/auth/config/auth-config-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String jsonPatchString)
            throws JsonPatchException, IOException {
        log.debug("AUTH CONF details to patch - jsonPatchString:{} ", jsonPatchString);
        Conf conf = configurationService.findConf();
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("AUTH CONF details BEFORE patch - appConfiguration :{}", appConfiguration);
        appConfiguration = Jackson.applyPatch(jsonPatchString, conf.getDynamic());
        log.debug("AUTH CONF details BEFORE patch merge - appConfiguration:{}", appConfiguration);
        conf.setDynamic(appConfiguration);

        // validate Agama Configuration
        if (jsonPatchString.contains(AGAMACONFIGURATION)) {
            validateAgamaConfiguration(appConfiguration.getAgamaConfiguration());
        }

        configurationService.merge(conf);
        appConfiguration = configurationService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @Operation(summary = "Returns persistence type configured for Jans authorization server.", description = "Returns persistence type configured for Jans authorization server.", operationId = "get-properties-persistence", tags = {
            "Configuration – Properties" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jans Authorization Server persistence type", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersistenceConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/auth/config/auth-config-persistence.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS }, superScopes = {
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.PERSISTENCE)
    public Response getPersistenceDetails() {
        String persistenceType = configurationService.getPersistenceType();
        log.debug("AuthConfigResource::getPersistenceDetails() - persistenceType:{}", persistenceType);
        PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration();
        persistenceConfiguration.setPersistenceType(persistenceType);
        log.debug("AuthConfigResource::getPersistenceDetails() - persistenceConfiguration:{}", persistenceConfiguration);
        return Response.ok(persistenceConfiguration).build();
    }

    private void validateAgamaConfiguration(EngineConfig engineConfig) {
        log.debug("engineConfig:{}", engineConfig);

        if (engineConfig == null) {
            return;
        }

        if (engineConfig.getMaxItemsLoggedInCollections() < 1) {
            throwBadRequestException("maxItemsLoggedInCollections should be greater than zero -> "
                    + engineConfig.getMaxItemsLoggedInCollections());
        }
    }
}
