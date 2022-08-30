/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.config.Conf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;


import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import org.json.JSONObject;
import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.CONFIG)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource extends ConfigBaseResource {
    
    private static final String AGAMACONFIGURATION = "agamaConfiguration";

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    
    @Operation(summary = "Gets a list of Gluu attributes.",
    description= "Gets all Jans authorization server configuration properties.",
    tags = {"Configuration – Properties"}
    )
    @ApiResponses(value = { 
    @ApiResponse(responseCode = "200", description = "Gluu Attributes",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = AppConfiguration.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS })
    public Response getAppConfiguration() {
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("ConfigResource::getAppConfiguration() appConfiguration:{}",appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_WRITE_ACCESS })
    public Response patchAppConfigurationProperty(@NotNull String requestString) throws JsonPatchException, IOException {
        log.debug("AUTH CONF details to patch - requestString:{} ", requestString);
        Conf conf = configurationService.findConf();
        AppConfiguration appConfiguration = configurationService.find();
        log.debug("AUTH CONF details BEFORE patch - appConfiguration :{}", appConfiguration);
        appConfiguration = Jackson.applyPatch(requestString, conf.getDynamic());
        log.debug("AUTH CONF details BEFORE patch merge - appConfiguration:{}", appConfiguration);
        conf.setDynamic(appConfiguration);
        
        //validate Agama Configuration
        if(requestString.contains(AGAMACONFIGURATION)){
            validateAgamaConfiguration(appConfiguration.getAgamaConfiguration());
        }
        
        configurationService.merge(conf);
        appConfiguration = configurationService.find();
        log.debug("AUTH CONF details AFTER patch merge - appConfiguration:{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_AUTH_CONFIG_READ_ACCESS })
    @Path(ApiConstants.PERSISTENCE)
    public Response getPersistenceDetails() {
        String persistenceType = configurationService.getPersistenceType();
        log.debug("ConfigResource::getPersistenceDetails() - persistenceType:{}", persistenceType);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("persistenceType", persistenceType);
        log.debug("ConfigResource::getPersistenceDetails() - jsonObject:{}", jsonObject );
        return Response.ok(jsonObject.toString()).build();
    }

    
    private void validateAgamaConfiguration(EngineConfig engineConfig) {
        log.debug("engineConfig:{}", engineConfig);
        
        if(engineConfig == null) {
            return;
        }
        
        if(engineConfig.getMaxItemsLoggedInCollections()<1) {
            thorwBadRequestException("maxItemsLoggedInCollections should be greater than zero -> " + engineConfig.getMaxItemsLoggedInCollections());
        }
    }
}
