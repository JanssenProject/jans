/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.SsaService;
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

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.util.Util.escapeLog;

import org.slf4j.Logger;

@Path(ApiConstants.JANS_AUTH + ApiConstants.SSA)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SsaResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    SsaService ssaService;

    @Operation(summary = "Get SSA based on `jti` or `org_id`", description = "Get SSA based on `jti` or `org_id`", operationId = "get-ssa", tags = {
            "Software Statement Assertion (SSA)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SSA_DELETE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Response.class), examples = @ExampleObject(name = "Response json example", value = "example/session/get-session.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SSA_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.SSA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getSsa(
            @Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "JWT ID - unique identifier for the JWT") @QueryParam(value = ApiConstants.JTI) @NotNull String jti,
            @Parameter(description = "Organization identifier") @QueryParam(value = ApiConstants.ORGID) @NotNull String orgId) {
        if (log.isInfoEnabled()) {
            logger.info("SSA search parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }

        logger.error("SSA search parameters - jti:{}, orgId:{}", jti, orgId);
        checkNotEmpty(jti, "jti");
        JsonNode jsonNode = null;
        try {
            jsonNode = ssaService.getSsa(authorization, jti, orgId);

        } catch (Exception ex) {
            ex.printStackTrace();
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    @Operation(summary = "Create SSA ", description = "Create SSA", operationId = "create-ssa", tags = {
            "Software Statement Assertion (SSA)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SSA_DELETE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Response.class), examples = @ExampleObject(name = "Response json example", value = "example/session/get-session.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SSA_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.SSA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createSsa(
            @Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "SSA") @NotNull String ssaJson) {
        if (log.isInfoEnabled()) {
            logger.info("Create SSA parameters - ssaJson:{}", escapeLog(ssaJson));
        }
        logger.error("Create SSA search parameters - ssaJson:{}", ssaJson);
        checkNotEmpty(ssaJson, "ssaJson");
        JsonNode jsonNode = null;
        try {
            jsonNode = ssaService.createSsa(authorization, ssaJson);

        } catch (Exception ex) {
            ex.printStackTrace();
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

    @Operation(summary = "Revoke existing active SSA based on `jti` or `org_id`", description = "Revoke existing active SSA based on `jti` or `org_id`", operationId = "revoke-ssa", tags = {
            "Software Statement Assertion (SSA)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SSA_DELETE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Response.class), examples = @ExampleObject(name = "Response json example", value = "example/session/get-session.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.SSA_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.SSA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response revokeSsa(
            @Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "JWT ID - unique identifier for the JWT") @QueryParam(value = ApiConstants.JTI) String jti,
            @Parameter(description = "Organization identifier") @QueryParam(value = ApiConstants.ORGID) @NotNull String orgId) {
        if (log.isInfoEnabled()) {
            logger.info("SSA search parameters - jti:{}, orgId:{}", escapeLog(jti), escapeLog(orgId));
        }
        logger.error("Delete SSA parameters - jti:{}, orgId:{}", jti, orgId);
        JsonNode jsonNode = null;
        try {
            jsonNode = ssaService.revokeSsa(authorization, jti, orgId);

        } catch (Exception ex) {
            ex.printStackTrace();
            throwInternalServerException(ex);
        }
        return Response.ok(jsonNode).build();
    }

}
