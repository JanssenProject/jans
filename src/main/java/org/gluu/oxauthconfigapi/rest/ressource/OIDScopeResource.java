/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.oxauth.persistence.model.Scope;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.service.ScopeService;
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
			logger.info(getClass().getName() + "::Get list of OpenID Connect scopes");
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
			logger.info("OIDClientResource::getOpenIdClientByInum - Get OpenId Connect Scope by Inum");
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

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete OpenId Connect Scope ", description = "Delete an OpenId Connect Scope")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteScope(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		logger.info("OIDScopeResource::deleteScope - Delete OpenID Connect Scope");
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
