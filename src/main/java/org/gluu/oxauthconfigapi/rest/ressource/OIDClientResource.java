/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
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

import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxauthconfigapi.exception.ApiException;
import org.gluu.oxauthconfigapi.exception.ApiExceptionType;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.model.OxAuthApplicationType;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthSubjectType;
import org.gluu.oxtrust.service.ClientService;
import org.gluu.oxtrust.service.EncryptionService;
import org.gluu.oxtrust.service.ScopeService;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

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
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConnectClients(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<OxAuthClient> clients = new ArrayList<OxAuthClient>();
		if (!pattern.isEmpty() && pattern.length() >= 2) {
			clients = clientService.searchClients(pattern, limit);
		} else {
			clients = clientService.getAllClients(limit);
		}
		return Response.ok(clients).build();
	}

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) throws ApiException {
		OxAuthClient client = clientService.getClientByInum(inum);
		if (client == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		return Response.ok(client).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createOpenIdConnect(@Valid OxAuthClient client) throws ApiException, EncryptionException {
		String inum = clientService.generateInumForNewClient();
		client.setInum(inum);
		if (client.getDisplayName() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.DISPLAY_NAME);
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
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateClient(@Valid OxAuthClient client) throws ApiException, EncryptionException {
		String inum = client.getInum();
		if (inum == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.INUM);
		}
		if (client.getDisplayName() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.DISPLAY_NAME);
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
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH + ApiConstants.SCOPES)
	public Response addScopesToClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull JsonObject object)
			throws Exception {
		if (inum == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.INUM);
		}
		JsonArray scopeInums = object.getJsonArray("scopes");
		if (scopeInums == null || scopeInums.isEmpty()) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.SCOPES);
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
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Path(ApiConstants.INUM_PATH + ApiConstants.GRANT_TYPES)
	public Response addGrantTypeToClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull JsonObject object)
			throws ApiException {
		if (inum == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.INUM);
		}
		JsonArray grantTypesValues = object.getJsonArray("grant-types");
		if (grantTypesValues == null || grantTypesValues.isEmpty()) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.GRANT_TYPES);
		}
		OxAuthClient existingClient = clientService.getClientByInum(inum);
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (existingClient != null) {
			GrantType[] grantTypes = existingClient.getGrantTypes();
			if (grantTypes == null) {
				grantTypes = new GrantType[] {};
			}
			List<GrantType> myList = new ArrayList<GrantType>(Arrays.asList(grantTypes));
			for (JsonValue grantType : grantTypesValues) {
				String grantTypeName = ((JsonString) grantType).getString();
				GrantType mGrantType = getGrantTypeFromName(grantTypeName);
				if (mGrantType != null) {
					builder.add(grantTypeName, Response.Status.OK.getStatusCode());
					myList.add(mGrantType);
				} else {
					builder.add(grantTypeName, Response.Status.NOT_FOUND.getStatusCode());
				}
			}
			GrantType[] types = new GrantType[myList.size()];
			existingClient.setGrantTypes(myList.toArray(types));
			clientService.updateClient(existingClient);
			return Response.ok(builder.build()).build();
		} else {
			throw new ApiException(ApiExceptionType.NOT_FOUND, "openid client");
		}
	}

	private GrantType getGrantTypeFromName(String grantTypeName) {
		try {
			GrantType mGrantType = GrantType.fromString(grantTypeName);
			return mGrantType;
		} catch (Exception e) {
			return null;
		}

	}

	@DELETE
	@Path(ApiConstants.INUM_PATH + ApiConstants.SCOPES + ApiConstants.SEPARATOR + ApiConstants.SCOPE_INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response removeScopeFromClient(@PathParam(ApiConstants.INUM) @NotNull String inum,
			@PathParam(ApiConstants.SCOPE_INUM) @NotNull String scopeInum) throws Exception {
		OxAuthClient client = clientService.getClientByInum(inum);
		Scope scope = scopeService.getScopeByInum(scopeInum);
		if (client != null) {
			if (scope != null) {
				List<String> oxAuthScopes = client.getOxAuthScopes();
				if (oxAuthScopes == null) {
					oxAuthScopes = new ArrayList<String>();
				}
				oxAuthScopes.remove(scope.getDn());
				client.setOxAuthScopes(oxAuthScopes);
				clientService.updateClient(client);
				return Response.ok().build();
			} else {
				throw new ApiException(ApiExceptionType.NOT_FOUND, "scope");
			}
		} else {
			throw new ApiException(ApiExceptionType.NOT_FOUND, "client");
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) throws ApiException {
		OxAuthClient client = clientService.getClientByInum(inum);
		if (client != null) {
			clientService.removeClient(client);
			return Response.noContent().build();
		} else {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
	}

}
