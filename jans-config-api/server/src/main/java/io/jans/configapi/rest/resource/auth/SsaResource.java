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
   
    @Operation(summary = "Revoke existing active SSA based on `jti` or `org_id`", description = "Revoke existing active SSA based on `jti` or `org_id`", operationId = "revoke-ssa", tags = {
            "Software Statement Assertion (SSA)" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SSA_DELETE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.SSA_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.SSA_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response revokeSsa(
            @Parameter(description = "Authorization code") @HeaderParam("Authorization") String authorization,
            @Parameter(description = "JWT ID - unique identifier for the JWT") @QueryParam(value = ApiConstants.JTI) String jti) {
        if (log.isInfoEnabled()) {
            log.info("Delete SSA - jti:{}", escapeLog(jti));
        }
        checkNotEmpty(jti, ApiConstants.JTI);
        String jsonNode = null;
        try {
            jsonNode = ssaService.revokeSsa(authorization, jti);
            log.info("SSA search parameters - jsonNode:{}",jsonNode);
        } catch (Exception ex) {
             throwInternalServerException(ex);
        }
        return Response.ok().build();
    }

}
