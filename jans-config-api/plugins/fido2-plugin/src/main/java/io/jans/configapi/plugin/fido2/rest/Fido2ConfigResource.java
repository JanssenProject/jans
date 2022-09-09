/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.fido2.rest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.jans.config.oxtrust.DbApplicationConfiguration;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.fido2.service.Fido2Service;
import io.jans.configapi.plugin.fido2.util.Fido2Util;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.plugin.fido2.util.Constants;
import io.jans.configapi.core.util.Jackson;

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

@Path(Constants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ConfigResource extends BaseResource {

    private static final String FIDO2_CONFIGURATION = "fido2Configuration";

    @Inject
    Logger logger;

    @Inject
    Fido2Service fido2Service;

    @Inject
    Fido2Util fido2Util;

    @Operation(summary = "Gets Jans Authorization Server Fido2 configuration properties", description = "Gets Jans Authorization Server Fido2 configuration properties", operationId = "get-properties-fido2", tags = {
            "Fido2 - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/fido2.readonly" }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DbApplicationConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.FIDO2_CONFIG_READ_ACCESS })
    public Response getFido2Configuration() throws JsonProcessingException {
        DbApplicationConfiguration dbApplicationConfiguration = this.fido2Service.find();
        logger.debug("FIDO2 details dbApplicationConfiguration.getDynamicConf():{}",
                dbApplicationConfiguration.getDynamicConf());
        return Response.ok(Jackson.asJsonNode(dbApplicationConfiguration.getDynamicConf())).build();
    }

    @Operation(summary = "Updates Fido2 configuration properties", description = "Updates Fido2 configuration properties", operationId = "put-properties-fido2", tags = {
            "Fido2 - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/fido2.write" }))
    @RequestBody(description = "Fido2Config", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DbApplicationConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fido2Config", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.FIDO2_CONFIG_WRITE_ACCESS })
    public Response updateFido2Configuration(@NotNull String fido2ConfigJson) {
        logger.debug("FIDO2 details to be updated - fido2ConfigJson:{} ", fido2ConfigJson);
        checkResourceNotNull(fido2ConfigJson, FIDO2_CONFIGURATION);
        this.fido2Service.merge(fido2ConfigJson);
        return Response.ok(fido2ConfigJson).build();
    }

}