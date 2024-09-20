/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.model.token.TokenEntity;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
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

@Path(ApiConstants.TOKEN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TokenResource extends ConfigBaseResource {

    @Inject
    ClientService clientService;

    @Inject
    ClientService clientService;

    @Operation(summary = "Get client token details", description = "Get client token details", operationId = "get-token-details", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TokenEntity.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.TOKEN_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(PATH_SEPARATOR + ApiConstants.INUM + PATH_SEPARATOR + ApiConstants.INUM_PATH)
    public Response getClientToken(
            @Parameter(description = "Script identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        logger.error("Serach tokens by inum:{}", inum);
        if (logger.isDebugEnabled()) {
            logger.debug("Serach tokens by inum:{}", escapeLog(inum));
        }
        checkNotNull(inum, ApiConstants.INUM);

//validate inum
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, "Client");
        logger.error("Serach tokens by client:{}", client);

        List<TokenEntity> tokenEntityList = clientService.getTokenOfClient(inum);
        logger.error("Client:{} token are tokenEntityList:{}", inum, tokenEntityList);
        return Response.ok(tokenEntityList).build();
    }

    @Operation(summary = "Revoke client token.", description = "Revoke client token.", operationId = "revoke-token", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path("/test/revoke")
    public Response revokeClientToken(@Valid TokenEntity tokenEntity) {
        if (logger.isDebugEnabled()) {
            logger.debug("Delete client token - tokenEntity():{}", escapeLog(tokenEntity));
        }

        checkResourceNotNull(tokenEntity, "TokenEntity object");

        Response response = clientService.revokeClientToken(tokenEntity);
        logger.debug(" Revoke client token response:{}", response);
        if (response != null) {
            logger.debug(" Revoke client token - response.getStatus():{}, response.getEntity():{}",
                    response.getStatus(), response.getEntity());

        }

        return Response.status(Response.Status.OK).entity(response).build();
    }

}
