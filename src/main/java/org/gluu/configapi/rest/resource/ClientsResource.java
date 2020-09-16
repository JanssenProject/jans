/**
 * 
 */
package org.gluu.configapi.rest.resource;

import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @author Mougang T.Gasmyr
 *
 */

//@Path(ApiConstants.BASE_API_URL + ApiConstants.CLIENTS)
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
//@RequestScoped
public class ClientsResource extends BaseResource {
    /**
     * 
     */
    private static final String OPENID_CONNECT_CLIENT = "openid connect client";

    @Inject
    Logger logger;
/*
    @Inject
    ClientService clientService;

    @Inject
    ScopeService scopeService;

    @Inject
    EncryptionService encryptionService;

    @GET
    @ProtectedApi(scopes = { READ_ACCESS })
    public Response getOpenIdConnectClients(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
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
    public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        OxAuthClient client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        return Response.ok(client).build();
    }

    @POST
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response createOpenIdConnect(@Valid OxAuthClient client) throws EncryptionException {
        String inum = clientService.generateInumForNewClient();
        client.setInum(inum);
        checkNotNull(client.getDisplayName(), AttributeNames.DISPLAY_NAME);
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
    public Response updateClient(@Valid OxAuthClient client) throws EncryptionException {
        String inum = client.getInum();
        checkNotNull(inum, AttributeNames.INUM);
        checkNotNull(client.getDisplayName(), AttributeNames.DISPLAY_NAME);
        OxAuthClient existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);
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
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
        OxAuthClient existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);
        try {
            existingClient = Jackson.applyPatch(pathString, existingClient);
            clientService.updateClient(existingClient);
            return Response.ok(existingClient).build();
        } catch (Exception e) {
            logger.error("", e);
            throw new WebApplicationException(e.getMessage());
        }
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { WRITE_ACCESS })
    public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        OxAuthClient client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        clientService.removeClient(client);
        return Response.noContent().build();
    }
*/
}
