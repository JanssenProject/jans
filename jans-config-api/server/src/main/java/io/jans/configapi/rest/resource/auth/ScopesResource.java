/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.as.model.common.ScopeType;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.CustomScope;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
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

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

/**
 * Configures both OpenID Connect and UMA scopes.
 *
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.SCOPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScopesResource extends ConfigBaseResource {

    private static final String SCOPE = "scope";

    @Inject
    Logger log;

    @Inject
    ScopeService scopeService;

    @Context
    UriInfo uriInfo;

    @Operation(summary = "Gets list of Scopes", description = "Gets list of Scopes", operationId = "get-oauth-scopes", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_READ_ACCESS })
    public Response getScopes(@DefaultValue("") @QueryParam(ApiConstants.TYPE) String type,
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @DefaultValue("false") @QueryParam(value = ApiConstants.WITH_ASSOCIATED_CLIENTS) boolean withAssociatedClients) {
        log.debug(
                "SCOPES to be fetched based on type:{}, limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, withAssociatedClients:{}",
                type, limit, pattern, startIndex, sortBy, sortOrder, withAssociatedClients);
        SearchRequest searchReq = createSearchRequest(scopeService.getDnForScope(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount());

        return Response.ok(doSearch(searchReq, type, withAssociatedClients)).build();
    }

    @Operation(summary = "Get Scope by Inum", description = "Get Scope by Inum", operationId = "get-oauth-scopes-by-inum", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomScope.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getScopeById(@NotNull @PathParam(ApiConstants.INUM) String inum,
            @DefaultValue("false") @QueryParam(value = ApiConstants.WITH_ASSOCIATED_CLIENTS) boolean withAssociatedClients) {
        log.debug("SCOPES to be fetched by inum:{}", inum);
        CustomScope scope = scopeService.getScopeByInum(inum, withAssociatedClients);
        checkResourceNotNull(scope, SCOPE);
        return Response.ok(scope).build();
    }

    @Operation(summary = "Get Scope by creatorId", description = "Get Scope by creatorId", operationId = "get-scope-by-creator", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = CustomScope.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_READ_ACCESS })
    @Path(ApiConstants.CREATOR + ApiConstants.CREATORID_PATH)
    public Response getScopeByClientId(@NotNull @PathParam(ApiConstants.CREATORID) String creatorId) {
        log.debug("SCOPES to be fetched by creatorId:{}", creatorId);
        SearchRequest searchReq = new SearchRequest();
        searchReq.setFilterAttributeName(Arrays.asList("creatorId"));
        searchReq.setFilter(creatorId);
        List<CustomScope> scopes = scopeService.searchScope(searchReq);
        return Response.ok(scopes).build();
    }

    @Operation(summary = "Get Scope by type", description = "Get Scope by type", operationId = "get-scope-by-type", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = CustomScope.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_READ_ACCESS })
    @Path(ApiConstants.TYPE + ApiConstants.TYPE_PATH)
    public Response getScopeByType(@NotNull @PathParam(ApiConstants.TYPE) String type) {
        log.debug("SCOPES to be fetched by type:{}", type);
        SearchRequest searchReq = new SearchRequest();
        searchReq.setFilterAttributeName(Arrays.asList("jansScopeTyp"));
        searchReq.setFilter(type);
        List<CustomScope> scopes = scopeService.searchScope(searchReq);
        return Response.ok(scopes).build();
    }

    @Operation(summary = "Create Scope", description = "Create Scope", operationId = "post-oauth-scopes", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_WRITE_ACCESS }))
    @RequestBody(description = "Scope object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_WRITE_ACCESS })
    public Response createOpenidScope(@Valid Scope scope) {
        log.debug("SCOPE to be added - scope:{}", scope);

        checkNotNull(scope.getId(), AttributeNames.ID);
        if (scope.getDisplayName() == null) {
            scope.setDisplayName(scope.getId());
        }
        String inum = UUID.randomUUID().toString();
        scope.setInum(inum);
        scope.setDn(scopeService.getDnForScope(inum));
        if (scope.getScopeType() == null) {
            scope.setScopeType(ScopeType.OAUTH);
        }

        scopeService.addScope(scope);
        Scope result = scopeService.getScopeByInum(inum);
        log.debug("Id of newly added is {}", result.getId());
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @Operation(summary = "Update Scope", description = "Update Scope", operationId = "put-oauth-scopes", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_WRITE_ACCESS }))
    @RequestBody(description = "Scope object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_WRITE_ACCESS })
    public Response updateScope(@Valid Scope scope) {
        log.debug("SCOPE to be updated - scop:{}", scope.getId());
        String inum = scope.getInum();
        checkNotNull(inum, SCOPE);
        Scope existingScope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(existingScope, SCOPE);
        if (scope.getScopeType() == null) {
            scope.setScopeType(ScopeType.OAUTH);
        }

        scope.setInum(existingScope.getInum());
        scope.setBaseDn(scopeService.getDnForScope(inum));
        scopeService.updateScope(scope);
        Scope result = scopeService.getScopeByInum(inum);

        log.debug("Updated scope:{}", result.getId());
        return Response.ok(result).build();
    }

    @Operation(summary = "Patch Scope", description = "Patch Scope", operationId = "patch-oauth-scopes-by-id", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = {
            @ExampleObject(value = "[ {op:replace, path: clients, value: [\"client_1\",\"client_2\"] },{op:add, path: clients/2, value: \"client_3\" } ]") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Scope.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        log.debug("SCOPES patch details - inum:{}, pathString:{}", inum, pathString);
        Scope existingScope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(existingScope, SCOPE);
        existingScope = Jackson.applyPatch(pathString, existingScope);
        scopeService.updateScope(existingScope);

        existingScope = scopeService.getScopeByInum(inum);
        log.debug("patched scope:{}", existingScope.getId());

        return Response.ok(existingScope).build();
    }

    @Operation(summary = "Delete Scope", description = "Delete Scope", operationId = "delete-oauth-scopes-by-inum", tags = {
            "OAuth - Scopes" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.SCOPES_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_DELETE_ACCESS })
    public Response deleteScope(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug("SCOPES to be deleted - inum:{}", inum);
        Scope scope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(scope, SCOPE);
        scopeService.removeScope(scope);
        log.debug("SCOPE is deleted");
        return Response.noContent().build();
    }

    private PagedResult<CustomScope> doSearch(SearchRequest searchReq, String type, boolean withAssociatedClients) {
        if (logger.isDebugEnabled()) {
            logger.debug("CustomScope search params - searchReq:{} ", escapeLog(searchReq));
        }

        PagedResult<CustomScope> pagedResult = scopeService.getScopeResult(searchReq, type, withAssociatedClients);
        if (logger.isTraceEnabled()) {
            logger.trace("PagedResult  - pagedResult:{}", pagedResult);
        }

        if (pagedResult != null) {
            logger.debug(
                    "Scope fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }
        logger.debug("Scope  - pagedResult:{}", pagedResult);
        return pagedResult;

    }
}
