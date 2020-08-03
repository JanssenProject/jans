/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.oxauth.persistence.model.Scope;
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
			logger.info("OIDScopeResource::getOpenIdConnectScopes - Get list of OpenID connect scopes");
			List<Scope> clients = new ArrayList<Scope>();

			if (!pattern.isEmpty() && pattern.length() >= 2) {
				clients = scopeService.searchScopes(pattern, limit);
			} else {
				clients = scopeService.getAllScopesList(limit);
			}
			return Response.ok(clients).build();
		} catch (Exception ex) {
			logger.error("Failed to openid connects clients", ex);
			return getInternalServerError(ex);
		}
	}

}
