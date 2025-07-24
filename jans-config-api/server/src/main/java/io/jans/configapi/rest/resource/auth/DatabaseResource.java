/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.config.GluuConfiguration;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;

import io.jans.configapi.service.auth.DatabaseService;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.ldap.GluuLdapConfiguration;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.PagedResult;
import io.jans.service.custom.CustomScriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.CONFIG + ApiConstants.DATABASE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DatabaseResource extends ConfigBaseResource {

    private  Map<String, Map<String, AttributeType>> tableColumnsMap;

    @Inject
    Logger log;

    @Inject
    private ApiAppConfiguration appConfiguration;

    @Inject
    DatabaseService databaseService;

    

    @Operation(summary = "Gets schema objects", description = "Gets schema objects.", operationId = "get-schema", tags = {
            "Database" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_READ_ACCESS, ApiAccessConstants.DATABASE_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthenticationMethod.class), examples = @ExampleObject(name = "Response example", value = "example/database/tableInfo.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ACRS_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
        Map<String, Map<String, AttributeType>> tableColumnsMap = databaseService.getTableColumnsMap();


        return Response.ok(tableColumnsMap).build();
    }

   

}