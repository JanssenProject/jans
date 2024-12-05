/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;


import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.service.auth.AgamaRepoService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.model.PagedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import static io.jans.as.model.util.Util.escapeLog;


import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * @author Puja Sharma
 */
@Path(ApiConstants.AGAMA_REPO)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgamaRepoResource extends ConfigBaseResource {

    private class AgamaRepoPagedResult extends PagedResult<JsonNode> {
    };

    @Inject
    Logger log;

    @Inject
    private ApiAppConfiguration appConfiguration;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AgamaRepoService agamaRepoService;

    @Operation(summary = "Gets all agama repositories.", description = "Gets all agama repositories.", operationId = "get-agama-repositories", tags = {
            "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_REPO_READ_ACCESS, ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama repositories", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-repo-get.json"))),
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAgamaRepositories() throws Exception {
        return Response.ok(agamaRepoService.getAllAgamaRepositories()).build();
    }

    @Operation(summary = "Gets Agama project latest release.", description = "Gets Agama project latest release.", operationId = "get-agama-project-latest-release", tags = {
            "Agama" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.AGAMA_REPO_READ_ACCESS, ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama project latest release info", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-prj-repo-get.json"))),
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgamaProjectLatestRelease(
            @Parameter(description = "Agama Projetc Rrepository Name") @PathParam(ApiConstants.NAME) @NotNull String repositoryName) {

        if (logger.isInfoEnabled()) {
            logger.info("Custom Script to be fetched based on type - repositoryName:{} ", escapeLog(repositoryName));
        }
        return Response.ok(agamaRepoService.getAgamaProjectLatestRepository(repositoryName)).build();
    }

}