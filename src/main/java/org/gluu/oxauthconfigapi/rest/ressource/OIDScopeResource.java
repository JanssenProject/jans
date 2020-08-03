/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.model.OxAuthClient;
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
public class OIDScopeResource extends BaseResource {
	@Inject
	Logger logger;

	@Inject
	ScopeService scopeService;

	@GET
	@Operation(summary = "List of OpenID Connect Scopes")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Scope.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConnectScopes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			List<Scope> clients = new ArrayList<Scope>();
			if (!pattern.isEmpty() && pattern.length() >= 2) {
				clients = scopeService.searchScopes(pattern, limit);
			} else {
				clients = scopeService.getAllScopesList(limit);
			}
			return Response.ok(clients).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch openid connects scopes", ex);
			return getInternalServerError(ex);
		}
	}

	@GET
	@Operation(summary = "Get OpenId Connect Scope by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = false))),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getOpenIdScopeByInum(@PathParam(ApiConstants.INUM) String inum) {
		try {
			Scope scope = scopeService.getScopeByInum(inum);
			if (scope == null) {
				return getResourceNotFoundError();
			}
			return Response.ok(scope).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch  OpenId Connect Scope " + inum, ex);
			return getInternalServerError(ex);
		}
	}

	@POST
	@Operation(summary = "Create OpenId Connect Scope")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = Scope.class, required = true))),
			@APIResponse(responseCode = "500", description = "Internal Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateOpenidScope(@Valid Scope scope) {
		try {
			if (scope.getId() == null) {
				return getMissingAttributeError(AttributeNames.ID);
			}
			if (scope.getDisplayName() == null) {
				scope.setDisplayName(scope.getId());
			}
			String inum = scopeService.generateInumForNewScope();
			scope.setInum(inum);
			scope.setDn(scopeService.getDnForScope(inum));
			if (scope.getScopeType() == null) {
				scope.setScopeType(ScopeType.OAUTH);
			}
			scopeService.addScope(scope);
			Scope result = scopeService.getScopeByInum(inum);
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			logger.error("Failed to create Connect scope", e);
			return getInternalServerError(e);
		}

	}

	@PUT
	@Operation(summary = "Update existing OpenId Connect Scope")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Scope.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateOpenIdConnectScope(@Valid Scope scope) {
		try {
			String inum = scope.getInum();
			if (inum == null) {
				return getResourceNotFoundError();
			}
			Scope existingScope = scopeService.getScopeByInum(inum);
			if (existingScope == null) {
				return getResourceNotFoundError();
			}
			scope.setInum(existingScope.getInum());
			scope.setBaseDn(scopeService.getDnForScope(inum));
			scopeService.updateScope(scope);
			Scope result = scopeService.getScopeByInum(inum);
			return Response.ok(result).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete OpenId Connect Scope ", description = "Delete an OpenId Connect Scope")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteScope(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		try {
			Scope scope = scopeService.getScopeByInum(inum);
			if (scope != null) {
				scopeService.removeScope(scope);
				return Response.noContent().build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to delete OpenId Connect scope", ex);
			return getInternalServerError(ex);
		}
	}

}
