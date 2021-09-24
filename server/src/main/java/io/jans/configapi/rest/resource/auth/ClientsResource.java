/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.Jackson;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.OPENID + ApiConstants.CLIENTS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClientsResource extends BaseResource {

    private static final String OPENID_CONNECT_CLIENT = "openid connect client";

    @Inject
    Logger log;

    @Inject
    ClientService clientService;

    @Inject
    EncryptionService encryptionService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS })
    public Response getOpenIdConnectClients(
            @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) throws Exception {
        final List<Client> clients;
        if (!pattern.isEmpty() && pattern.length() >= 2) {
            clients = clientService.searchClients(pattern, limit);
        } else {
            clients = clientService.getAllClients(limit);
        }
        return Response.ok(getClients(clients)).build();
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        return Response.ok(client).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    public Response createOpenIdConnect(@Valid Client client) throws NoSuchAlgorithmException, EncryptionException {
        log.debug("Client details to be added - client = " + client);
        String inum = client.getClientId();
        if (inum == null || inum.isEmpty() || inum.isBlank()) {
            inum = clientService.generateInumForNewClient();
            client.setClientId(inum);
        }
        checkNotNull(client.getClientName(), AttributeNames.DISPLAY_NAME);
        String clientSecret = client.getClientSecret();

        if (StringHelper.isEmpty(clientSecret)) {
            clientSecret = generatePassword();
        }

        client.setClientSecret(encryptionService.encrypt(clientSecret));
        client.setDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getClientSecretExpiresAt() != null);
        clientService.addClient(client);
        Client result = clientService.getClientByInum(inum);
        result.setClientSecret(encryptionService.decrypt(result.getClientSecret()));

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    public Response updateClient(@Valid Client client) throws EncryptionException {
        log.debug("Client details to be updated - client = " + client);
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
        result.setClientSecret(encryptionService.decrypt(client.getClientSecret()));

        return Response.ok(result).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        log.debug("Client details to be patched - inum = " + inum + " , pathString = " + pathString);
        Client existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);

        existingClient = Jackson.applyPatch(pathString, existingClient);
        clientService.updateClient(existingClient);
        return Response.ok(existingClient).build();
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS })
    public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug("Client to be deleted - inum = " + inum);
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        clientService.removeClient(client);
        return Response.noContent().build();
    }

    private List<Client> getClients(List<Client> clients) throws EncryptionException {
        if (clients != null && !clients.isEmpty()) {
            for (Client client : clients) {
                client.setClientSecret(encryptionService.decrypt(client.getClientSecret()));
            }
        }
        return clients;
    }

    private String generatePassword() throws NoSuchAlgorithmException {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        return secureRandom.ints(12, 0, characters.length()).mapToObj(characters::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

    }
}
