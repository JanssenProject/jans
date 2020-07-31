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
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.model.OxAuthApplicationType;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthSubjectType;
import org.gluu.oxtrust.service.ClientService;
import org.gluu.oxtrust.service.EncryptionService;
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

	@Inject
	EncryptionService encryptionService;

	@GET
	@Operation(summary = "Get list of OpenID Connect clients")
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
			return getInternalServerError(ex);
		}
	}

	@GET
	@Operation(summary = "Get OpenId Connect Client by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = false))),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) String inum) {
		try {
			logger.info("OIDClientResource::getOpenIdClientByInum - Get OpenId Connect Client by Inum");
			OxAuthClient client = clientService.getClientByInum(inum);
			if (client == null) {
				return getResourceNotFoundError();
			}
			return Response.ok(client).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch  openId Client " + inum, ex);
			return getInternalServerError(ex);
		}
	}

	@POST
	@Operation(summary = "Create new OpenId connect client")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = OxAuthClient.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createOpenIdConnect(@Valid OxAuthClient client) {
		try {
			logger.info("OIDClientResource::createOpenIdConnect - Create new openid connect client");
			String inum = clientService.generateInumForNewClient();
			client.setInum(inum);
			if (client.getDisplayName() == null) {
				return getMissingAttributeError("displayName");
			}
			if (client.getOxAuthClientSecret() != null) {
				client.setEncodedClientSecret(encryptionService.encrypt(client.getOxAuthClientSecret()));
			}
			if (client.getOxAuthAppType() == null) {
				client.setOxAuthAppType(OxAuthApplicationType.WEB);
			}
			if (client.getSubjectType() == null) {
				client.setSubjectType(OxAuthSubjectType.PUBLIC);
			}
			client.setDn(clientService.getDnForClient(inum));
			client.setDeletable(client.getExp() != null);
			clientService.addClient(client);
			OxAuthClient result = clientService.getClientByInum(inum);
			if (result.getEncodedClientSecret() != null) {
				result.setOxAuthClientSecret(encryptionService.decrypt(client.getEncodedClientSecret()));
			}
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception ex) {
			logger.error("Failed to create new openid connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@PUT
	@Operation(summary = "Update OpenId Connect client", description = "Update openidconnect client")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateClient(@Valid OxAuthClient client) {
		try {
			logger.info("OIDClientResource::updateOpenIdConnect - Update openid connect client");
			String inum = client.getInum();
			if (inum == null) {
				return getMissingAttributeError("inum");
			}
			if (client.getDisplayName() == null) {
				return getMissingAttributeError("displayName");
			}
			OxAuthClient existingClient = clientService.getClientByInum(inum);
			if (existingClient != null) {
				client.setInum(existingClient.getInum());
				client.setBaseDn(clientService.getDnForClient(inum));
				client.setDeletable(client.getExp() != null);
				if (client.getOxAuthClientSecret() != null) {
					client.setEncodedClientSecret(encryptionService.encrypt(client.getOxAuthClientSecret()));
				}
				clientService.updateClient(client);
				OxAuthClient result = clientService.getClientByInum(existingClient.getInum());
				if (result.getEncodedClientSecret() != null) {
					result.setOxAuthClientSecret(encryptionService.decrypt(client.getEncodedClientSecret()));
				}
				return Response.ok(result).build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to update OpenId Connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete OpenId Connect client ", description = "Delete an OpenId Connect client")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		logger.info("OIDClientResource::deleteOpenIdConnect - Delete OpenID Connect client");
		try {
			OxAuthClient client = clientService.getClientByInum(inum);
			if (client != null) {
				clientService.removeClient(client);
				return Response.noContent().build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to Delete OpenId Connect client", ex);
			return getInternalServerError(ex);
		}
	}

}
