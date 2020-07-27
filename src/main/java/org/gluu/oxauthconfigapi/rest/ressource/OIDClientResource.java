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
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.service.ClientService;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CLIENTS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OIDClientResource {
	@Inject
	Logger logger;

	@Inject
	ClientService clientService;

	@GET
	@Operation(summary = "Gets list of OpenID connect clients")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response getOpenIdConnectClients(@DefaultValue("100") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			logger.info("OIDClientResource::getOpenIdConnectClients - Gets list of OpenID connect clients");
			List<OxAuthClient> clients = new ArrayList<OxAuthClient>();
			if (!pattern.isEmpty() && pattern.length() >= 2) {
				clients = clientService.searchClients(pattern, limit);
			} else {
				clients = clientService.getAllClients(limit);
			}
			return Response.ok(clients).build();
		} catch (Exception ex) {
			logger.error("Failed to openid connects clients", ex);
			ApiError error = new ApiError(Response.Status.INTERNAL_SERVER_ERROR.toString(), "Server error",
					ex.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
		}
	}

}
