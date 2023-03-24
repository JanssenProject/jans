/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.as.persistence.model.GluuOrganization;
import io.jans.configapi.service.auth.OrganizationService;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path(ApiConstants.ORG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource extends ConfigBaseResource {

    @Inject
    OrganizationService organizationService;

    @Operation(summary = "Retrieves organization configuration", description = "Retrieves organization configuration", operationId = "get-organization-config", tags = {
            "Organization Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ORG_CONFIG_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuOrganization.class), examples = @ExampleObject(name = "Response json example", value = "example/org/org.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ORG_CONFIG_READ_ACCESS } , groupScopes = {
            ApiAccessConstants.ORG_CONFIG_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getOrganization() {
        return Response.ok(organizationService.getOrganization()).build();
    }

    @Operation(summary = "Patch organization configuration", description = "Patch organization configuration", operationId = "patch-organization-config", tags = {
            "Organization Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ORG_CONFIG_WRITE_ACCESS }))
    @RequestBody(description = "String representing JsonPatch request.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/org/org-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuOrganization.class), examples = @ExampleObject(name = "Response json example", value = "example/org/org-patch-response.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ORG_CONFIG_WRITE_ACCESS }, groupScopes = { }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchOrganization(@NotNull String pathString) throws JsonPatchException, IOException {
        logger.trace("Organization patch request - pathString:{} ", pathString);
        GluuOrganization organization = organizationService.getOrganization();
        try {

            organization = Jackson.applyPatch(pathString, organization);
            organizationService.updateOrganization(organization);

        } catch (Exception ex) {
            logger.error("Error while patching Organization details", ex);
            throwInternalServerException(ex);
        }
        return Response.ok(organizationService.getOrganization()).build();
    }

}