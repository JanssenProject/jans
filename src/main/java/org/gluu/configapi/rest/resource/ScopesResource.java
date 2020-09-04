/**
 * 
 */
package org.gluu.configapi.rest.resource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.AttributeNames;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxtrust.service.ScopeService;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.SCOPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScopesResource extends BaseResource {
	/**
	 * 
	 */
	private static final String OPENID_SCOPE = "openid connect scope";

	@Inject
	Logger logger;

	@Inject
	ScopeService scopeService;

	@Context
	UriInfo uriInfo;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConnectScopes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<Scope> scopes = new ArrayList<Scope>();
		if (!pattern.isEmpty() && pattern.length() >= 2) {
			scopes = scopeService.searchScopes(pattern, limit);
		} else {
			scopes = scopeService.getAllScopesList(limit);
		}
		return Response.ok(scopes).build();
	}

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getOpenIdScopeByInum(@NotNull @PathParam(ApiConstants.INUM) String inum) throws Exception {
		Scope scope = scopeService.getScopeByInum(inum);
		checkResourceNotNull(scope, OPENID_SCOPE);
		return Response.ok(scope).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createOpenidScope(@Valid Scope scope) throws Exception {
		checkNotNull(scope.getId(), AttributeNames.ID);
		if (scope.getDisplayName() == null) {
			scope.setDisplayName(scope.getId());
		}
		String inum = scopeService.generateInumForNewScope();
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
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateOpenIdConnectScope(@Valid Scope scope) throws Exception {
		String inum = scope.getInum();
		checkNotNull(inum, OPENID_SCOPE);
		Scope existingScope = scopeService.getScopeByInum(inum);
		checkResourceNotNull(existingScope, OPENID_SCOPE);
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
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response patchScope(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
		Scope existingScope = scopeService.getScopeByInum(inum);
		checkResourceNotNull(existingScope, OPENID_SCOPE);
		try {
			existingScope = Jackson.applyPatch(pathString, existingScope);
			scopeService.updateScope(existingScope);
			return Response.ok(existingScope).build();
		} catch (Exception e) {
			logger.error("",e);
			throw new WebApplicationException(e.getMessage());
		}

	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteScope(@PathParam(ApiConstants.INUM) @NotNull String inum) throws Exception {
		Scope scope = scopeService.getScopeByInum(inum);
		checkResourceNotNull(scope, OPENID_SCOPE);
		scopeService.removeScope(scope);
		return Response.noContent().build();
	}

}
