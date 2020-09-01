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

import org.gluu.oxauth.model.common.ScopeType;
import org.gluu.oxauthconfigapi.exception.ApiException;
import org.gluu.oxauthconfigapi.exception.ApiExceptionType;
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
		List<Scope> scopes = new ArrayList<Scope>();
		if (!pattern.isEmpty()) {
			scopes = umaScopeService.findUmaScopes(pattern, limit);
		} else {
			scopes = umaScopeService.getAllUmaScopes(limit);
		}
		return Response.ok(scopes).build();
	}

	@GET
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUmaScopeByImun(@PathParam(value = ApiConstants.INUM) @NotNull String inum) throws ApiException {
		Scope scope = umaScopeService.getUmaScopeByInum(inum);
		if (scope == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		return Response.ok(scope).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createUmaScope(@Valid Scope scope) throws ApiException {
		if (scope.getId() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.ID);
		}
		if (scope.getDisplayName() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.DISPLAY_NAME);
		}
		String inum = umaScopeService.generateInumForNewScope();
		scope.setInum(inum);
		scope.setDn(umaScopeService.getDnForScope(inum));
		scope.setScopeType(ScopeType.UMA);
		umaScopeService.addUmaScope(scope);
		Scope result = umaScopeService.getUmaScopeByInum(inum);
		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUmaScope(@Valid Scope scope) throws ApiException {
		String inum = scope.getInum();
		if (inum == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		Scope existingScope = umaScopeService.getUmaScopeByInum(inum);
		if (existingScope == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		scope.setInum(existingScope.getInum());
		scope.setBaseDn(umaScopeService.getDnForScope(inum));
		scope.setScopeType(ScopeType.UMA);
		umaScopeService.updateUmaScope(scope);
		Scope result = umaScopeService.getUmaScopeByInum(inum);
		return Response.ok(result).build();
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response deleteUmaScope(@PathParam(value = ApiConstants.INUM) @NotNull String inum) throws ApiException {
		Scope scope = umaScopeService.getUmaScopeByInum(inum);
		if (scope == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		umaScopeService.removeUmaScope(scope);
		return Response.status(Response.Status.NO_CONTENT).build();
	}
}
