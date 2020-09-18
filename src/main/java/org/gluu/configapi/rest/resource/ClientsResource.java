/**
 *
 */
package org.gluu.configapi.rest.resource;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.service.ClientService;
import org.gluu.configapi.service.ScopeService;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.AttributeNames;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.common.EncryptionService;
import org.gluu.util.security.StringEncrypter.EncryptionException;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
public class ClientsResource extends BaseResource {
    /**
     *
     */
    private static final String OPENID_CONNECT_CLIENT = "openid connect client";

    @Inject
    Logger logger;

    @Inject
    ClientService clientService;

    @Inject
    ScopeService scopeService;

    @Inject
    EncryptionService encryptionService;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    public Response getOpenIdConnectClients(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
        List<Client> clients = new ArrayList<Client>();
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            clients = clientService.searchClients(pattern, limit);
        } else {
            clients = clientService.getAllClients(limit);
        }
        return Response.ok(clients).build();
    }

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        return Response.ok(client).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response createOpenIdConnect(@Valid Client client) throws EncryptionException {
        String inum = clientService.generateInumForNewClient();
        client.setClientId(inum);
        checkNotNull(client.getClientName(), AttributeNames.DISPLAY_NAME);
        if (client.getClientSecret() != null) {
            client.setClientSecret(encryptionService.encrypt(client.getClientSecret()));
        }
        client.setDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getClientSecretExpiresAt() != null);
        clientService.addClient(client);
        Client result = clientService.getClientByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateClient(@Valid Client client) throws EncryptionException {
        String inum = client.getClientId();
        checkNotNull(inum, AttributeNames.INUM);
        checkNotNull(client.getClientName(), AttributeNames.DISPLAY_NAME);
        Client existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);
        client.setClientId(existingClient.getClientId());
        client.setBaseDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getExpirationDate() != null);
        if (client.getClientSecret() != null) {
            client.setClientSecret(encryptionService.encrypt(client.getClientSecret()));
        }
        clientService.updateClient(client);
        Client result = clientService.getClientByInum(existingClient.getClientId());
        if (result.getClientSecret() != null) {
            result.setClientSecret(encryptionService.decrypt(client.getClientSecret()));
        }
        return Response.ok(result).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response patchClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
        Client existingClient = clientService.getClientByInum(inum);
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
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        clientService.removeClient(client);
        return Response.noContent().build();
    }

}
