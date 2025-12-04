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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import jakarta.inject.Inject;
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

    @Inject
    Logger log;

    @Inject
    private ApiAppConfiguration appConfiguration;

    @Inject
    ConfigurationService configurationService;

    @Inject
    AgamaRepoService agamaRepoService;

    /**
     * Retrieve all Agama repositories.
     *
     * Returns an HTTP response whose entity is a JSON representation of the available Agama repositories.
     *
     * @return HTTP 200 with a JSON body containing the repositories (JsonNode) or HTTP 204 if no repositories are available.
     */
    @Operation(summary = "Gets all agama repositories.", description = "Gets all agama repositories.", operationId = "get-agama-repositories", tags = {
            "Agama" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama repositories", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class), examples = @ExampleObject(name = "Response json example", value = "example/agama/agama-repo-get.json"))),
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }, superScopes = { ApiAccessConstants.AGAMA_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAgamaRepositories() {
        return Response.ok(agamaRepoService.getAllAgamaRepositories()).build();
    }

    /**
     * Downloads an Agama project from the specified download link.
     *
     * @param downloadLink the URL or link used to retrieve the Agama project
     * @return a JAX-RS Response whose entity is the project content as a plain-text/binary payload
     * @throws IOException if an I/O error occurs while obtaining the project
     */
    @Operation(summary = "Download agama project.", description = "Download agama project.", operationId = "get-agama-project", tags = {
            "Agama" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.AGAMA_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agama project", content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class, format = "binary"))),
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.AGAMA_REPO_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.AGAMA_REPO_WRITE_ACCESS }, superScopes = { ApiAccessConstants.AGAMA_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/download")
    public Response getAgamaProject(
            @Parameter(description = "Agama project download Link") @QueryParam(value = "downloadLink") String downloadLink)
            throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info(" Agama Project File downloadLink :{}", escapeLog(downloadLink));
        }
        return Response.ok(agamaRepoService.getAgamaProject(downloadLink)).build();
    }

}