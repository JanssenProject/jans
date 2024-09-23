/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.model.token.TokenEntity;
import io.jans.orm.model.PagedResult;
import io.jans.configapi.service.auth.ClientAuthService;
import io.jans.configapi.service.auth.ClientService;
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

@Path(ApiConstants.TOKEN)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TokenResource extends ConfigBaseResource {

    private class TokenEntityPagedResult extends PagedResult<TokenEntity> {
    };

    @Inject
    ClientAuthService clientAuthService;

    @Inject
    ClientService clientService;

    @Operation(summary = "Get client token details", description = "Get client token details", operationId = "get-token-details", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/token/token-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.TOKEN_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.CLIENT + ApiConstants.CLIENTID_PATH)
    public Response getClientToken(
            @Parameter(description = "Script identifier") @PathParam(ApiConstants.CLIENTID) @NotNull String clientId) {
        
        if (logger.isInfoEnabled()) {
            logger.info("Serach tokens by clientId:{}", escapeLog(clientId));
        }
        checkNotNull(clientId, ApiConstants.CLIENTID);

        // validate clientId
        Client client = clientService.getClientByInum(clientId);
        checkResourceNotNull(client, "Client");
        logger.debug("Serach tokens by client:{}", client);

        SearchRequest searchReq = createSearchRequest(clientAuthService.geTokenDn(null), clientId, "tknCde",
                ApiConstants.ASCENDING, Integer.parseInt(ApiConstants.DEFAULT_LIST_START_INDEX),
                Integer.parseInt(ApiConstants.DEFAULT_LIST_SIZE), null, null, this.getMaxCount(), null,
                JansAttribute.class);

        TokenEntityPagedResult tokenEntityPagedResult = searchTokenByClientId(searchReq);
        logger.info("Asset fetched based on name are:{}", tokenEntityPagedResult);
        return Response.ok(tokenEntityPagedResult).build();

    }

    @Operation(summary = "Revoke client token.", description = "Revoke client token.", operationId = "revoke-token", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.REVOKE + ApiConstants.TOKEN_CODE_PATH)
    public Response revokeClientToken(
            @Parameter(description = "Token Code") @PathParam(ApiConstants.TOKEN_CODE_PARAM) @NotNull String tknCde) {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke token - tknCde():{}", escapeLog(tknCde));
        }

        checkResourceNotNull(tknCde, ApiConstants.TOKEN_CODE_PARAM);
        clientAuthService.revokeTokenEntity(tknCde);
        logger.info(" Successfully deleted token identified by tknCde:{}", tknCde);

        return Response.noContent().build();
    }

    private TokenEntityPagedResult searchTokenByClientId(SearchRequest searchReq) {

        logger.debug("Search asset by name params - searchReq:{} ", searchReq);
        TokenEntityPagedResult tokenEntityPagedResult = null;
        PagedResult<TokenEntity> pagedResult = clientAuthService.getTokenOfClient(searchReq);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Asset fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            tokenEntityPagedResult = getTokenEntityPagedResult(pagedResult);
        }

        logger.debug("Asset tokenEntityPagedResult:{} ", tokenEntityPagedResult);
        return tokenEntityPagedResult;
    }

    private TokenEntityPagedResult getTokenEntityPagedResult(PagedResult<TokenEntity> pagedResult) {
        TokenEntityPagedResult tokenEntityPagedResult = null;
        if (pagedResult != null) {
            List<TokenEntity> tokenEntityList = pagedResult.getEntries();
            tokenEntityPagedResult = new TokenEntityPagedResult();
            tokenEntityPagedResult.setStart(pagedResult.getStart());
            tokenEntityPagedResult.setEntriesCount(pagedResult.getEntriesCount());
            tokenEntityPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            tokenEntityPagedResult.setEntries(tokenEntityList);
        }
        return tokenEntityPagedResult;
    }

}
