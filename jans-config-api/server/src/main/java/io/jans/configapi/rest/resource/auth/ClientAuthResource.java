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

import io.jans.configapi.service.auth.ClientAuthService;

import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

import io.jans.orm.model.PagedResult;

import io.swagger.v3.oas.annotations.Hidden;
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
import java.util.Map;
import java.util.Set;

@Hidden
@Path(ApiConstants.CLIENTS + ApiConstants.AUTHORIZATIONS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClientAuthResource extends ConfigBaseResource {

    @Inject
    ClientAuthService clientAuthService;

    @Operation(summary = "Gets list of client authorizations", description = "Gets list of client authorizations", operationId = "get-client-authorizations", tags = {
            "OpenID Connect - Clients - Authorizations" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CLIENT_AUTHORIZATIONS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getOpenIdConnectClients(
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {

        if (logger.isDebugEnabled()) {
            logger.debug("Client serach param - inum:{}", escapeLog(inum));
        }

        Map<Client, Set<Scope>> clientAuths = clientAuthService.getUserAuthorizations(inum);
        logger.debug("Client serach param - clientAuths:{}", clientAuths);
        return Response.ok(clientAuths).build();
    }

}
