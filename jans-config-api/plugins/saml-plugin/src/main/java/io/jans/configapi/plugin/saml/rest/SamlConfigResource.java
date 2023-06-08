/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.service.SamlService;
import io.jans.configapi.plugin.saml.util.SamlUtil;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.saml.model.conf.AppConfiguration;

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

@Path(Constants.SAML_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlConfigResource extends BaseResource {

    private static final String saml_CONFIGURATION = "samlConfiguration";

    @Inject
    Logger logger;

    @Inject
    samlService samlService;

    @Inject
    samlUtil samlUtil;

    @Operation(summary = "Gets Jans SAML configuration properties", description = "Gets Jans SAML configuration properties", operationId = "get-properties-saml", tags = {
            "SAML - Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.saml_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AppConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.saml_CONFIG_READ_ACCESS })
    public Response getsamlConfiguration() {
        AppConfiguration appConfiguration = this.samlService.find();
        logger.debug("SAML details appConfiguration():{}", appConfiguration);
        return Response.ok(appConfiguration).build();
    }

 }