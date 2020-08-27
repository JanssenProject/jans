/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.model.OxAuthApplicationType;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthSubjectType;
import org.gluu.oxtrust.service.ClientService;
import org.gluu.oxtrust.service.EncryptionService;
import org.gluu.oxtrust.service.ScopeService;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CLIENTS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OIDClientResource extends BaseResource {
	@Inject
	Logger logger;

	@Inject
	ClientService clientService;

	@Inject
	ScopeService scopeService;

	@Inject
	EncryptionService encryptionService;

	@GET
	@Operation(summary = "Get list of OpenID Connect clients")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OxAuthClient[].class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConnectClients(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
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
			String inum = clientService.generateInumForNewClient();
			client.setInum(inum);
			if (client.getDisplayName() == null) {
				return getMissingAttributeError(AttributeNames.DISPLAY_NAME);
			}
			if (client.getEncodedClientSecret() != null) {
				client.setEncodedClientSecret(encryptionService.encrypt(client.getEncodedClientSecret()));
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
			String inum = client.getInum();
			if (inum == null) {
				return getMissingAttributeError(AttributeNames.INUM);
			}
			if (client.getDisplayName() == null) {
				return getMissingAttributeError(AttributeNames.DISPLAY_NAME);
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

	@PUT
	@Operation(summary = "Add scopes to existing client", description = "Add scopes to existing client")
	@APIResponses(value = { @APIResponse(responseCode = "200"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH + ApiConstants.SCOPES)
	public Response addScopesToClient(@NotNull @PathParam(ApiConstants.INUM) @NotNull String inum,
			@NotNull JsonObject object) {
		try {
			if (inum == null) {
				return getMissingAttributeError(AttributeNames.INUM);
			}
			JsonArray scopeInums = object.getJsonArray("scopes");
			if (scopeInums == null || scopeInums.isEmpty()) {
				return getMissingAttributeError(AttributeNames.SCOPES);
			}
			OxAuthClient existingClient = clientService.getClientByInum(inum);
			JsonObjectBuilder builder = Json.createObjectBuilder();
			if (existingClient != null) {
				List<String> oxAuthScopes = existingClient.getOxAuthScopes();
				if (oxAuthScopes == null) {
					oxAuthScopes = new ArrayList<String>();
				}
				for (JsonValue scopeInum : scopeInums) {
					String inumScope = ((JsonString) scopeInum).getString();
					Scope scope = scopeService.getScopeByInum(inumScope);
					if (scope != null) {
						builder.add(inumScope, Response.Status.OK.getStatusCode());
						oxAuthScopes.add(scope.getDn());
					} else {
						builder.add(inumScope, Response.Status.NOT_FOUND.getStatusCode());
					}
				}
				existingClient.setOxAuthScopes(oxAuthScopes);
				clientService.updateClient(existingClient);
				return Response.ok(builder.build()).build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to add scopes to openId connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@PUT
	@Operation(summary = "Add grant types to existing client", description = "Add grant types to existing client")
	@APIResponses(value = { @APIResponse(responseCode = "200"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH + ApiConstants.GRANT_TYPES)
	public Response addGrantTypeToClient(@NotNull @PathParam(ApiConstants.INUM) @NotNull String inum,
			@NotNull JsonObject object) {
		try {
			if (inum == null) {
				return getMissingAttributeError(AttributeNames.INUM);
			}
			JsonArray grantTypesValues = object.getJsonArray("grant-types");
			if (grantTypesValues == null || grantTypesValues.isEmpty()) {
				return getMissingAttributeError(AttributeNames.GRANT_TYPES);
			}
			OxAuthClient existingClient = clientService.getClientByInum(inum);
			JsonObjectBuilder builder = Json.createObjectBuilder();
			if (existingClient != null) {
				GrantType[] grantTypes = existingClient.getGrantTypes();
				if (grantTypes == null) {
					grantTypes = new GrantType[] {};
				}
				for (JsonValue grantType : grantTypesValues) {
					builder.add(((JsonString) grantType).getString(), Response.Status.OK.getStatusCode());
				}
				existingClient.setGrantTypes(grantTypes);
				clientService.updateClient(existingClient);
				return Response.ok(builder.build()).build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (

		Exception ex) {
			logger.error("Failed to add grand typpes to openId connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH + ApiConstants.SCOPES + ApiConstants.SEPARATOR + ApiConstants.SCOPE_INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response removeScopeFromClient(@PathParam(ApiConstants.INUM) @NotNull String inum,
			@PathParam(ApiConstants.SCOPE_INUM) @NotNull String scopeInum) {
		try {
			OxAuthClient client = clientService.getClientByInum(inum);

            if (client == null) {
                return getResourceNotFoundError("client");
            }

            Scope scope = scopeService.getScopeByInum(scopeInum);
            if (scope == null) {
                return getResourceNotFoundError("scope");
            }

            List<String> oxAuthScopes = client.getOxAuthScopes();
            if (oxAuthScopes == null) {
                oxAuthScopes = new ArrayList<>();
            }
            oxAuthScopes.remove(scope.getDn());
            client.setOxAuthScopes(oxAuthScopes);
            clientService.updateClient(client);
            return Response.ok().build();
		} catch (Exception ex) {
			logger.error("Failed to Delete OpenId Connect client", ex);
			return getInternalServerError(ex);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete OpenId Connect client ", description = "Delete an OpenId Connect client")
	@APIResponses(value = { @APIResponse(responseCode = "204", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
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
