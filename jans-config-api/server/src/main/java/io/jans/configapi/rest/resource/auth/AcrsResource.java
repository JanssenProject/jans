/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import org.apache.commons.lang.StringUtils;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AcrsResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Operation(summary = "Gets default authentication method.", description = "Gets default authentication method.", operationId = "get-acrs", tags = {
            "Default Authentication Method" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ACRS_READ_ACCESS, ApiAccessConstants.ACRS_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class), examples = @ExampleObject(name = "Response example", value = "example/acr/acr.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ACRS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        AuthenticationMethod authenticationMethod = new AuthenticationMethod();
        authenticationMethod.setDefaultAcr(gluuConfiguration.getAuthenticationMode());
        return Response.ok(authenticationMethod).build();
    }

    @Operation(summary = "Updates default authentication method.", description = "Updates default authentication method.", operationId = "put-acrs", tags = {
            "Default Authentication Method" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ACRS_WRITE_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class), examples = @ExampleObject(name = "Request json example", value = "example/acr/acr.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_WRITE_ACCESS }, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateDefaultAuthenticationMethod(@NotNull AuthenticationMethod authenticationMethod) {
        log.debug("ACRS details to  update - authenticationMethod:{}", authenticationMethod);

        if (authenticationMethod == null || StringUtils.isBlank(authenticationMethod.getDefaultAcr())) {
            thorwBadRequestException("Default authentication method should not be null or empty !");
        }

        if (authenticationMethod != null) {
            final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();
            gluuConfiguration.setAuthenticationMode(authenticationMethod.getDefaultAcr());
            configurationService.merge(gluuConfiguration);
        }
        return Response.ok(authenticationMethod).build();
    }

}