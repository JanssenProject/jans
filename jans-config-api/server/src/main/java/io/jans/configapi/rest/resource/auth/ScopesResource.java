/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.common.ScopeType;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.CustomScope;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.core.util.Jackson;
import io.jans.util.StringHelper;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
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

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.SCOPES_READ_ACCESS })
    public Response getScopes(@DefaultValue("") @QueryParam(ApiConstants.TYPE) String type,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue("false") @QueryParam(value = ApiConstants.WITH_ASSOCIATED_CLIENTS) boolean withAssociatedClients) {
        log.debug("SCOPES to be fetched based on type:{}, limit:{}, pattern:{}", type, limit, pattern);
        final List<CustomScope> scopes;
        if (StringHelper.isNotEmpty(pattern)) {
            scopes = scopeService.searchScopes(pattern, limit, type, withAssociatedClients);
        } else {
            scopes = scopeService.getAllScopesList(limit, type, withAssociatedClients);
        }
        return Response.ok(scopes).build();
    }

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
}
