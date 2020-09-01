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
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.service.uma.UmaScopeService;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.UMA + ApiConstants.SCOPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UMAScopeResource extends BaseResource {

	@Inject
	Logger logger;

	@Inject
	UmaScopeService umaScopeService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAllUmaScopes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			List<Scope> scopes = new ArrayList<Scope>();
			if (!pattern.isEmpty()) {
				scopes = umaScopeService.findUmaScopes(pattern, limit);
			} else {
				scopes = umaScopeService.getAllUmaScopes(limit);
			}
			return Response.ok(scopes).build();
		} catch (Exception e) {
			logger.info("Failed to fetch Uma Scopes");
			return getInternalServerError(e);
		}
	}

	@GET
	@Path(ApiConstants.INUM_PATH)
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Scope.class, required = false))),
			@APIResponse(responseCode = "404", description = "Resource Not Found"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUmaScopeByImun(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		try {
			Scope scope = umaScopeService.getUmaScopeByInum(inum);
			if (scope == null) {
				return getResourceNotFoundError();
			}
			return Response.ok(scope).build();
		} catch (Exception e) {
			logger.error("Failed to retrieve uma scope", e);
			return getInternalServerError(e);
		}
	}

	@POST
	@Operation(summary = "Create Uma Scope")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = Scope.class, required = true))),
			@APIResponse(responseCode = "500", description = "Internal Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createUmaScope(@Valid Scope scope) {
		try {
			if (scope.getId() == null) {
				return getMissingAttributeError(AttributeNames.ID);
			}
			if (scope.getDisplayName() == null) {
				return getMissingAttributeError(AttributeNames.DISPLAY_NAME);
			}
			String inum = umaScopeService.generateInumForNewScope();
			scope.setInum(inum);
			scope.setDn(umaScopeService.getDnForScope(inum));
			scope.setScopeType(ScopeType.UMA);
			umaScopeService.addUmaScope(scope);
			Scope result = umaScopeService.getUmaScopeByInum(inum);
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			logger.error("Failed to create uma scope", e);
			return getInternalServerError(e);
		}

	}

	@PUT
	@Operation(summary = "Update existing Uma Scope")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Scope.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUmaScope(@Valid Scope scope) {
		try {
			String inum = scope.getInum();
			if (inum == null) {
				return getResourceNotFoundError();
			}
			Scope existingScope = umaScopeService.getUmaScopeByInum(inum);
			if (existingScope == null) {
				return getResourceNotFoundError();
			}
			scope.setInum(existingScope.getInum());
			scope.setBaseDn(umaScopeService.getDnForScope(inum));
			scope.setScopeType(ScopeType.UMA);
			umaScopeService.updateUmaScope(scope);
			Scope result = umaScopeService.getUmaScopeByInum(inum);
			return Response.ok(result).build();
		} catch (Exception e) {
			logger.error("Failed to update uma scope", e);
			return getInternalServerError(e);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = Scope.class, required = false))),
			@APIResponse(responseCode = "404", description = "Resource Not Found"),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response deleteUmaScope(@PathParam(value = ApiConstants.INUM) @NotNull String inum) {
		try {
			Scope scope = umaScopeService.getUmaScopeByInum(inum);
			if (scope == null) {
				return getResourceNotFoundError();
			}
			umaScopeService.removeUmaScope(scope);
			return Response.status(Response.Status.NO_CONTENT).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}
}
