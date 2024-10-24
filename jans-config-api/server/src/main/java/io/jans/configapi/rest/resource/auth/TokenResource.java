/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.model.SearchRequest;
import io.jans.model.token.TokenEntity;
import io.jans.orm.model.PagedResult;
import io.jans.configapi.service.auth.TokenService;
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

    private static final String TOKEN_NOT_FOUND = "Token identified by %s not found.";

    @Inject
    TokenService tokenService;

    @Inject
    ClientService clientService;

    @Operation(summary = "Get token details by Id.", description = "Get token details by Id.", operationId = "get-token-by-id", tags = {
            "Token" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenEntity.class), examples = @ExampleObject(name = "Response example", value = "example/token/get-token.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.TOKEN_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.TOKEN_CODE_PATH + ApiConstants.TOKEN_CODE_PATH_PARAM)
    public Response getTokenById(
            @Parameter(description = "Token identifier") @PathParam(ApiConstants.TOKEN_CODE) @NotNull String tknCde) {

        if (logger.isInfoEnabled()) {
            logger.info("Serach tokens by id:{}", escapeLog(tknCde));
        }
        checkNotNull(tknCde, ApiConstants.TOKEN_CODE);

        TokenEntity tokenEntity = this.tokenService.getTokenEntityByCode(tknCde);
        if (tokenEntity == null) {
            throwNotFoundException("Not Found", String.format(TOKEN_NOT_FOUND, tknCde));
        }
        logger.info("Token fetched tokenEntity:{}", tokenEntity);
        return Response.ok(tokenEntity).build();

    }

    @Operation(summary = "Get token details by client.", description = "Get token details by client.", operationId = "get-token-by-client", tags = {
            "Token" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenEntityPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/token/get-all-token.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.TOKEN_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.CLIENT + ApiConstants.CLIENTID_PATH)
    public Response getClientToken(
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.CLIENTID) @NotNull String clientId) {

        if (logger.isInfoEnabled()) {
            logger.info("Serach tokens by clientId:{}", escapeLog(clientId));
        }
        checkNotNull(clientId, ApiConstants.CLIENTID);

        // validate clientId
        Client client = clientService.getClientByInum(clientId);
        checkResourceNotNull(client, "Client");
        logger.debug("Serach tokens by client:{}", client);

        String fieldValuePair = "clnId=" + clientId;
        SearchRequest searchReq = createSearchRequest(tokenService.getDnForTokenEntity(null), null,
                ApiConstants.TOKEN_CODE, ApiConstants.ASCENDING,
                Integer.parseInt(ApiConstants.DEFAULT_LIST_START_INDEX),
                Integer.parseInt(ApiConstants.DEFAULT_LIST_SIZE), null, null, this.getMaxCount(), fieldValuePair,
                TokenEntity.class);

        TokenEntityPagedResult tokenEntityPagedResult = searchTokens(searchReq);
        logger.info("Token fetched are:{}", tokenEntityPagedResult);
        return Response.ok(tokenEntityPagedResult).build();

    }

    @Operation(summary = "Search tokens", description = "Search tokens", operationId = "search-token", tags = {
            "Token" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenEntityPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/token/get-all-token.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.TOKEN_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.SEARCH)
    public Response searchTokenEntries(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.TOKEN_CODE) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "grtTyp=client_credentials,tknTyp=access_token")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Token serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

        SearchRequest searchReq = createSearchRequest(tokenService.getDnForTokenEntity(null), pattern, sortBy,
                sortOrder, startIndex, limit, null, null, this.getMaxCount(), fieldValuePair, TokenEntity.class);

        TokenEntityPagedResult tokenEntityPagedResult = searchTokens(searchReq);
        logger.info("Token fetched are:{}", tokenEntityPagedResult);
        return Response.ok(tokenEntityPagedResult).build();

    }

    @Operation(summary = "Revoke client token.", description = "Revoke client token.", operationId = "revoke-token", tags = {
            "Token" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.TOKEN_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @ProtectedApi(scopes = { ApiAccessConstants.TOKEN_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    @Path(ApiConstants.REVOKE + ApiConstants.TOKEN_CODE_PATH_PARAM)
    public Response revokeClientToken(
            @Parameter(description = "Token Code") @PathParam(ApiConstants.TOKEN_CODE) @NotNull String tknCde) {
        if (logger.isInfoEnabled()) {
            logger.info("Revoke token - tknCde():{}", escapeLog(tknCde));
        }

        checkResourceNotNull(tknCde, ApiConstants.TOKEN_CODE);
        tokenService.revokeTokenEntity(tknCde);
        logger.info(" Successfully deleted token identified by tknCde:{}", tknCde);

        return Response.noContent().build();
    }

    private TokenEntityPagedResult searchTokens(SearchRequest searchReq) {

        logger.debug("Search Token by name params - searchReq:{} ", searchReq);
        TokenEntityPagedResult tokenEntityPagedResult = null;
        PagedResult<TokenEntity> pagedResult = tokenService.searchToken(searchReq);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Token fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            tokenEntityPagedResult = getTokenEntityPagedResult(pagedResult);
        }

        logger.debug("Token tokenEntityPagedResult:{} ", tokenEntityPagedResult);
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
