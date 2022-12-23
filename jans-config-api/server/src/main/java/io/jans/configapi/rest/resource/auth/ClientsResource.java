/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.AuthUtil;
import io.jans.model.GluuAttribute;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.util.StringHelper;
import io.jans.util.security.StringEncrypter.EncryptionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.OPENID + ApiConstants.CLIENTS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ClientsResource extends ConfigBaseResource {

    private static final String OPENID_CONNECT_CLIENT = "openid connect client";

    @Inject
    ClientService clientService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    private InumService inumService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    AuthUtil authUtil;

    @Inject
    ScopeService scopeService;

    @Inject
    AttributeService attributeService;

    @Operation(summary = "Gets list of OpenID Connect clients", description = "Gets list of OpenID Connect clients", operationId = "get-oauth-openid-clients", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/openid-clients-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getOpenIdConnectClients(
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client serach param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        SearchRequest searchReq = createSearchRequest(clientService.getDnForClient(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount());
        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Get OpenId Connect Client by Inum", description = "Get OpenId Connect Client by Inum", operationId = "get-oauth-openid-clients-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getOpenIdClientByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Client serach by inum:{}", escapeLog(inum));
        }
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        return Response.ok(client).build();
    }

    @Operation(summary = "Create new OpenId Connect client", description = "Create new OpenId Connect client", operationId = "post-oauth-openid-client", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }))
    @RequestBody(description = "OpenID Connect Client object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/openid-clients-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createOpenIdConnect(@Valid Client client) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client to be added - client:{}, client.getAttributes():{}, client.getCustomAttributes():{}",
                    escapeLog(client), escapeLog(client.getAttributes()), escapeLog(client.getCustomAttributes()));
        }

        String inum = client.getClientId();
        if (inum == null || inum.isEmpty() || inum.isBlank()) {
            inum = inumService.generateClientInum();
            client.setClientId(inum);
        }
        checkNotNull(client.getRedirectUris(), AttributeNames.REDIRECT_URIS);

        // scope validation
        checkScopeFormat(client);

        // Claim validation
        if (client.getClaims() != null && client.getClaims().length > 0) {
            validateClaim(client);
        }

        String clientSecret = client.getClientSecret();

        if (StringHelper.isEmpty(clientSecret)) {
            clientSecret = generatePassword();
        }

        client.setClientSecret(encryptionService.encrypt(clientSecret));
        client.setDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getClientSecretExpiresAt() != null);
        ignoreCustomObjectClassesForNonLDAP(client);

        logger.trace(
                "Final Client details to be added - client:{}, client.getAttributes():{}, client.getCustomAttributes():{}",
                client, client.getAttributes(), client.getCustomAttributes());
        clientService.addClient(client);
        Client result = clientService.getClientByInum(inum);
        result.setClientSecret(encryptionService.decrypt(result.getClientSecret()));

        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @Operation(summary = "Update OpenId Connect client", description = "Update OpenId Connect client", operationId = "put-oauth-openid-client", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }))
    @RequestBody(description = "OpenID Connect Client object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/openid-clients-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateClient(@Valid Client client) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client details to be updated - client:{}", escapeLog(client));
        }
        String inum = client.getClientId();
        checkNotNull(inum, AttributeNames.INUM);
        checkNotNull(client.getRedirectUris(), AttributeNames.REDIRECT_URIS);
        Client existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);

        // scope validation
        checkScopeFormat(client);

        // Claim validation
        if (client.getClaims() != null && client.getClaims().length > 0) {
            validateClaim(client);
        }

        client.setClientId(existingClient.getClientId());
        client.setBaseDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getExpirationDate() != null);
        if (client.getClientSecret() != null) {
            client.setClientSecret(encryptionService.encrypt(client.getClientSecret()));
        }
        ignoreCustomObjectClassesForNonLDAP(client);

        logger.debug("Final Client details to be updated - client:{}", client);
        clientService.updateClient(client);
        Client result = clientService.getClientByInum(existingClient.getClientId());
        result.setClientSecret(encryptionService.decrypt(client.getClientSecret()));

        return Response.ok(result).build();
    }

    @Operation(summary = "Patch OpenId Connect client", description = "Patch OpenId Connect client", operationId = "patch-oauth-openid-client-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/openid-clients-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchClient(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String jsonPatchString)
            throws JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client details to be patched - inum:{}, jsonPatchString:{}", escapeLog(inum),
                    escapeLog(jsonPatchString));
        }
        Client existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);

        existingClient = Jackson.applyPatch(jsonPatchString, existingClient);
        clientService.updateClient(existingClient);
        return Response.ok(existingClient).build();
    }

    @Operation(summary = "Delete OpenId Connect client", description = "Delete OpenId Connect client", operationId = "delete-oauth-openid-client-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteClient(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Client to be deleted - inum:{} ", escapeLog(inum));
        }
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

    private String generatePassword() {
        return UUID.randomUUID().toString();
    }

    private PagedResult<Client> doSearch(SearchRequest searchReq) throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client search params - searchReq:{} ", escapeLog(searchReq));
        }

        PagedResult<Client> pagedResult = clientService.getClients(searchReq);
        if (logger.isTraceEnabled()) {
            logger.trace("PagedResult  - pagedResult:{}", pagedResult);
        }

        if (pagedResult != null) {
            logger.debug(
                    "Client fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());

            List<Client> clients = pagedResult.getEntries();
            getClients(clients);
            logger.debug("Clients fetched  - clients:{}", clients);
            pagedResult.setEntries(clients);
        }
        logger.debug("Clients pagedResult:{}", pagedResult);
        return pagedResult;

    }

    private Client ignoreCustomObjectClassesForNonLDAP(Client client) {
        String persistenceType = configurationService.getPersistenceType();
        logger.debug("persistenceType: {}", persistenceType);
        if (!PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType)) {
            logger.debug(
                    "Setting CustomObjectClasses :{} to null as its used only for LDAP and current persistenceType is {} ",
                    client.getCustomObjectClasses(), persistenceType);
            client.setCustomObjectClasses(null);
        }
        return client;
    }

    private Client checkScopeFormat(Client client) {
        if (client == null) {
            return client;
        }

        // check scope
        logger.debug("Checking client.getScopes():{}", client.getScopes());
        if (client.getScopes() == null || client.getScopes().length == 0) {
            return client;
        }

        List<String> validScopes = new ArrayList<>();
        List<String> invalidScopes = new ArrayList<>();

        for (String scope : client.getScopes()) {
            logger.debug("Is scope:{} valid:{}", scope, authUtil.isValidDn(scope));
            List<Scope> scopes = new ArrayList<>();
            if (authUtil.isValidDn(scope)) {
                Scope scp = findScopeByDn(scope);
                if (scp != null) {
                    scopes.add(scp);
                }
            } else {
                scopes = scopeService.searchScopesById(scope);
            }
            logger.debug("Scopes from DB - {}'", scopes);
            if (!scopes.isEmpty()) {
                validScopes.add(scopes.get(0).getDn());
            } else {
                invalidScopes.add(scope);
            }
        }
        logger.debug("Scope validation result - validScopes:{}, invalidScopes:{} ", validScopes, invalidScopes);

        if (!invalidScopes.isEmpty()) {
            thorwBadRequestException("Invalid scope in request -> " + invalidScopes.toString());
        }

        // reset scopes
        if (!validScopes.isEmpty()) {
            String[] scopeArr = validScopes.stream().toArray(String[]::new);
            client.setScopes(scopeArr);
        }
        return client;
    }

    private Scope findScopeByDn(String scopeDn) {
        try {
            return scopeService.getScopeByDn(scopeDn);
        } catch (EntryPersistenceException e) {
            return null;
        }
    }

    private Client validateClaim(Client client) {
        if (client == null) {
            return client;
        }

        // check claims
        logger.debug("client.getClaims():{}", client.getClaims());
        List<String> claims = client.getClaims() != null ? Arrays.asList(client.getClaims()) : null;
        logger.debug("Client claims:{}", claims);

        List<String> validClaims = new ArrayList<>();
        List<String> invalidClaims = new ArrayList<>();

        for (String claim : claims) {
            logger.debug("Is claim:{} valid-DN?:{}", claim, authUtil.isValidDn(claim));
            GluuAttribute gluuAttribute = null;
            if (authUtil.isValidDn(claim)) {
                gluuAttribute = attributeService.getAttributeUsingDn(claim);
            } else {
                gluuAttribute = attributeService.getAttributeUsingName(claim);
            }
            logger.debug("Attribute from DB - {}'", gluuAttribute);
            if (gluuAttribute != null) {
                validClaims.add(claim);
            } else {
                invalidClaims.add(claim);
            }
        }
        logger.debug("Claim validation result - validClaims:{}, invalidClaims:{} ", validClaims, invalidClaims);

        if (!invalidClaims.isEmpty()) {
            thorwBadRequestException("Invalid claim in request -> " + invalidClaims.toString());
        }

        // reset Claims
        if (!validClaims.isEmpty()) {
            String[] scopeArr = validClaims.stream().toArray(String[]::new);
            client.setClaims(scopeArr);
        }

        return client;
    }

}
