/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.configapi.core.model.ClientAuth;
import io.jans.configapi.service.auth.ClientAuthService;
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

@Path(ApiConstants.CLIENTS + ApiConstants.AUTHORIZATIONS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClientAuthResource extends ConfigBaseResource {

    @Inject
    ClientAuthService clientAuthService;

    @Operation(summary = "Gets list of client authorization", description = "Gets list of client authorizations", operationId = "get-client-authorization", tags = {
            "Client Authorization" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(schema = @Schema(implementation = ClientAuth.class), examples = @ExampleObject(name = "Response json example", value = "example/client-auth/client-auth-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.USERID_PATH)
    public Response getClientAuthorization(
            @Parameter(description = "User identifier") @PathParam(ApiConstants.USERID) @NotNull String userId) {

        if (logger.isInfoEnabled()) {
            logger.info("Client Authorization serach param - userId:{}", escapeLog(userId));
        }

        Map<Client, Set<Scope>> clientAuths = clientAuthService.getUserAuthorizations(userId);
        logger.info("Client serach param - clientAuths:{}", clientAuths);
        
        ClientAuth clientAuth = new ClientAuth();
        clientAuth.setClientAuths(clientAuths);
        
        return Response.ok(clientAuths).build();
    }

    @Operation(summary = "Revoke client authorization", description = "Revoke client authorizations", operationId = "delete-client-authorization", tags = {
            "Client Authorization" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CLIENT_AUTHORIZATIONS_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.USERID_PATH + ApiConstants.CLIENTID_PATH + ApiConstants.USERNAME_PATH)
    public Response deleteClientAuthorization(
            @Parameter(description = "User identifier") @PathParam(ApiConstants.USERID) @NotNull String userId,
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.CLIENTID) @NotNull String clientId,
            @Parameter(description = "User name") @PathParam(ApiConstants.USERNAME) @NotNull String userName) {

        if (logger.isInfoEnabled()) {
            logger.info("ClientAuthorization to be deleted for - userId:{}, clientId:{}, userName:{}",
                    escapeLog(userId), escapeLog(clientId), escapeLog(userName));
        }
        clientAuthService.removeClientAuthorizations(userId, clientId, userName);
        logger.info("ClientAuthorizations removed!!!");
        return Response.noContent().build();
    }

}
