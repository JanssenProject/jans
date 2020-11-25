/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.common.ScopeType;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ScopeService;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.Jackson;
import io.jans.util.StringHelper;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Configures both OpenID Connect and UMA scopes.
 *
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.SCOPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScopesResource extends BaseResource {

    private static final String SCOPE = "scope";

    @Inject
    ScopeService scopeService;

    @Context
    UriInfo uriInfo;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getScopes(@DefaultValue("") @QueryParam(ApiConstants.TYPE) String type,
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        final List<Scope> scopes;
        if (StringHelper.isNotEmpty(pattern)) {
            scopes = scopeService.searchScopes(pattern, limit, type);
        } else {
            scopes = scopeService.getAllScopesList(limit, type);
        }
        return Response.ok(scopes).build();
    }

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response getScopeById(@NotNull @PathParam(ApiConstants.INUM) String inum) {
        Scope scope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(scope, SCOPE);
        return Response.ok(scope).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response createOpenidScope(@Valid Scope scope) {
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
        if (ScopeType.UMA.getValue().equalsIgnoreCase(scope.getScopeType().getValue())) {
            scope.setScopeType(ScopeType.OAUTH);
        }
        scopeService.addScope(scope);
        Scope result = scopeService.getScopeByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateScope(@Valid Scope scope) {
        String inum = scope.getInum();
        checkNotNull(inum, SCOPE);
        Scope existingScope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(existingScope, SCOPE);
        if (scope.getScopeType() == null) {
            scope.setScopeType(ScopeType.OAUTH);
        }
        if (ScopeType.UMA.getValue().equalsIgnoreCase(scope.getScopeType().getValue())) {
            scope.setScopeType(ScopeType.OAUTH);
        }
        scope.setInum(existingScope.getInum());
        scope.setBaseDn(scopeService.getDnForScope(inum));
        scopeService.updateScope(scope);
        Scope result = scopeService.getScopeByInum(inum);
        return Response.ok(result).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) throws JsonPatchException, IOException {
        Scope existingScope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(existingScope, SCOPE);
        existingScope = Jackson.applyPatch(pathString, existingScope);
        scopeService.updateScope(existingScope);
        return Response.ok(existingScope).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response deleteScope(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        Scope scope = scopeService.getScopeByInum(inum);
        checkResourceNotNull(scope, SCOPE);
        scopeService.removeScope(scope);
        return Response.noContent().build();
    }
}
