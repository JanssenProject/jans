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
public class OIDClientResource extends BaseResource {
	@Inject
	Logger logger;

	@Inject
	ClientService clientService;

	@GET
	@Operation(summary = "Get list of OpenID connect clients")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConnectClients(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			logger.info("OIDClientResource::getOpenIdConnectClients - Get list of OpenID connect clients");
			List<OxAuthClient> clients = new ArrayList<OxAuthClient>();
			if (!pattern.isEmpty() && pattern.length() >= 2) {
				clients = clientService.searchClients(pattern, limit);
			} else {
				clients = clientService.getAllClients(limit);
			}
			return Response.ok(clients).build();
		} catch (Exception ex) {
			logger.error("Failed to openid connects clients", ex);
			return getServerError(ex);
		}
	}

	@GET
	@Operation(summary = "Get OpenId Connect Client by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path("{inum}")
	public Response getOpenIdClientByInum(@PathParam("inum") String inum) {
		try {
			logger.info("OIDClientResource::getOpenIdClientByInum - Get OpenId Connect Client by Inum");
			OxAuthClient client = clientService.getClientByInum(inum);
			if (client == null) {
				return getNotFoundError();
			}
			return Response.ok(client).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch  openId Client " + inum, ex);
			return getServerError(ex);
		}
	}

}
