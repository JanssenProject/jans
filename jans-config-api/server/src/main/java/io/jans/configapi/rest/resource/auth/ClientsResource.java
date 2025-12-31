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
import io.jans.as.common.service.common.InumService;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.annotation.Ignore;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.service.auth.ClientService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.service.auth.ScopeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.AuthUtil;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter.EncryptionException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import org.apache.commons.lang3.StringUtils;

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
    private static final String CLIENT_SECRET = "clientSecret";

    @Inject
    private ApiAppConfiguration appConfiguration;

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

    /**
     * Retrieve a paged list of OpenID Connect clients matching the given search
     * criteria.
     *
     * @param limit          maximum number of results to return
     * @param pattern        search pattern to filter clients
     * @param startIndex     1-based index of the first result to return
     * @param sortBy         attribute used to sort results
     * @param sortOrder      sorting direction; allowed values are "ascending" and
     *                       "descending"
     * @param fieldValuePair comma-separated field=value pairs to further filter
     *                       results (e.g.
     *                       "applicationType=web,persistClientAuthorizations=true")
     * @return HTTP 200 response whose entity is a PagedResult<Client> containing
     *         the matching clients
     * @throws EncryptionException if decrypting client secrets for the response
     *                             fails
     */
    @Operation(summary = "Gets list of OpenID Connect clients", description = "Gets list of OpenID Connect clients", operationId = "get-oauth-openid-clients", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients/openid-clients-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getOpenIdConnectClients(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for searching", examples = @ExampleObject(name = "Field value example", value = "applicationType=web,persistClientAuthorizations=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair)
            throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Client search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }

        SearchRequest searchReq = createSearchRequest(clientService.getDnForClient(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount(), fieldValuePair, Client.class);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    /**
     * Retrieve a specific OpenID Connect client by its Inum.
     *
     * @param inum the client's Inum (identifier)
     * @return a Response whose entity is the requested Client and whose status is
     *         200 (OK)
     */
    @Operation(summary = "Get OpenId Connect Client by Inum", description = "Get OpenId Connect Client by Inum", operationId = "get-oauth-openid-clients-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_READ_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getOpenIdClientByInum(
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.INUM) @NotNull String inum)
            throws EncryptionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client search by inum:{}", escapeLog(inum));
        }
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);

        return Response.ok(applyResponsePolicy(client)).build();
    }

    /**
     * Create a new OpenID Connect client.
     *
     * Validates redirect URIs, scopes, and claims; generates a clientId if missing;
     * ensures a client secret exists (generating one when omitted) and stores it
     * encrypted; sets the client's DN and deletable flag, and persists the client.
     *
     * @param client the Client to create; must include redirect URIs. If clientId
     *               is absent, a new inum will be generated and assigned.
     * @return an HTTP 201 Created response containing the created Client with its
     *         clientSecret decrypted and the original claims preserved.
     * @throws EncryptionException if encryption or decryption of the client secret
     *                             fails.
     */
    @Operation(summary = "Create new OpenId Connect client", description = "Create new OpenId Connect client", operationId = "post-oauth-openid-client", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "OpenID Connect Client object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/clients/openid-clients-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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
        String[] claims = client.getClaims();
        if (client.getClaims() != null && client.getClaims().length > 0) {
            validateClaim(client);
        }

        String clientSecret = client.getClientSecret();

        if (StringUtils.isNotBlank(clientSecret)) {
            clientSecret = generatePassword();
        }

        client.setClientSecret(this.encryptPassword(client.getClientName(), clientSecret));
        client.setDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getClientSecretExpiresAt() != null);
        ignoreCustomObjectClassesForNonLDAP(client);

        logger.trace(
                "Final Client details to be added - client:{}, client.getAttributes():{}, client.getCustomAttributes():{}",
                client, client.getAttributes(), client.getCustomAttributes());
        clientService.addClient(client);
        Client result = clientService.getClientByInum(inum);
        result.setClaims(claims);

        // Response handling
        applyResponsePolicy(result);
        logger.debug("Claim post creation - result.getClaims():{} ", result.getClaims());
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    /**
     * Update an existing OpenID Connect client.
     *
     * <p>
     * Validates scopes and claims, preserves the existing clientId and base DN,
     * encrypts a provided client secret before persistence, and returns the stored
     * client with the client secret decrypted and claims restored for the response.
     * </p>
     *
     * @param client the Client object containing updated fields; must include the
     *               client's `clientId` and `redirectUris`
     * @return the updated Client with decrypted `clientSecret` and restored
     *         `claims`
     * @throws EncryptionException if encryption or decryption of the client secret
     *                             fails
     */
    @Operation(summary = "Update OpenId Connect client", description = "Update OpenId Connect client", operationId = "put-oauth-openid-client", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "OpenID Connect Client object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/clients/openid-clients-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Ignore
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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
        String[] claims = client.getClaims();
        if (client.getClaims() != null && client.getClaims().length > 0) {
            validateClaim(client);
        }

        client.setClientId(existingClient.getClientId());
        client.setBaseDn(clientService.getDnForClient(inum));
        client.setDeletable(client.getExpirationDate() != null);
        if (client.getClientSecret() != null) {
            client.setClientSecret(this.encryptPassword(client.getClientName(), client.getClientSecret()));
        }
        ignoreCustomObjectClassesForNonLDAP(client);

        logger.debug("Final Client details to be updated - client:{}", client);
        clientService.updateClient(client);
        Client result = clientService.getClientByInum(existingClient.getClientId());
        result.setClaims(claims);

        // Response handling
        applyResponsePolicy(result);
        logger.debug("Claim post updation - result.getClaims():{} ", result.getClaims());
        return Response.ok(result).build();
    }

    /**
     * Apply a JSON Patch to an existing OpenID Connect client identified by its
     * inum.
     *
     * @param inum            the client identifier (inum)
     * @param jsonPatchString the JSON Patch document as a string
     * @return a Response containing the patched Client and an HTTP 200 status
     * @throws JsonPatchException if the patch cannot be applied to the client
     * @throws IOException        if an I/O error occurs while processing the patch
     */
    @Operation(summary = "Patch OpenId Connect client", description = "Patch OpenId Connect client", operationId = "patch-oauth-openid-client-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/openid-clients/clients/openid-clients-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class), examples = @ExampleObject(name = "Response json example", value = "example/openid-clients/clients/openid-clients-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_WRITE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_WRITE_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchClient(
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.INUM) @NotNull String inum,
            @NotNull String jsonPatchString) throws EncryptionException, JsonPatchException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Client details to be patched - inum:{}, jsonPatchString:{}", escapeLog(inum),
                    escapeLog(jsonPatchString));
        }
        Client existingClient = clientService.getClientByInum(inum);
        checkResourceNotNull(existingClient, OPENID_CONNECT_CLIENT);

        existingClient = Jackson.applyPatch(jsonPatchString, existingClient);

        // ClientSecret encryption check
        boolean isClientSecretPresent = Jackson.isFieldPresent(jsonPatchString, CLIENT_SECRET);
        logger.debug(" isFieldPresent - CLIENT_SECRET - isClientSecretPresent:{}", isClientSecretPresent);

        if (isClientSecretPresent && StringUtils.isNotBlank(existingClient.getClientSecret())) {
            existingClient.setClientSecret(
                    this.encryptPassword(existingClient.getClientName(), existingClient.getClientSecret()));
        }

        clientService.updateClient(existingClient);

        applyResponsePolicy(existingClient);
        return Response.ok(existingClient).build();
    }

    /**
     * Delete the OpenID Connect client identified by the given inum.
     *
     * Validates that the client exists and removes it from storage.
     *
     * @param inum the client identifier (inum) of the client to delete
     * @return a 204 No Content response on successful deletion
     */
    @Operation(summary = "Delete OpenId Connect client", description = "Delete OpenId Connect client", operationId = "delete-oauth-openid-client-by-inum", tags = {
            "OAuth - OpenID Connect - Clients" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS }) })
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.OPENID_CLIENTS_DELETE_ACCESS }, groupScopes = {
            ApiAccessConstants.OPENID_DELETE_ACCESS }, superScopes = { ApiAccessConstants.OPENID_CLIENTS_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteClient(
            @Parameter(description = "Client identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (logger.isDebugEnabled()) {
            logger.debug("Client to be deleted - inum:{} ", escapeLog(inum));
        }
        Client client = clientService.getClientByInum(inum);
        checkResourceNotNull(client, OPENID_CONNECT_CLIENT);
        clientService.removeClient(client);
        return Response.noContent().build();
    }

    private List<Client> applyResponsePolicy(List<Client> clients) {
        logger.debug("isReturnClientSecretInResponse():{}, isReturnEncryptedClientSecretInResponse():{}, clients:{}",
                isReturnClientSecretInResponse(), isReturnEncryptedClientSecretInResponse(), clients);
        if (clients == null || clients.isEmpty()) {
            return clients;
        }
        for (Client client : clients) {
            applyResponsePolicy(client);
        }

        return clients;
    }

    private Client applyResponsePolicy(Client client) {
        logger.debug(
                " ApplyResponsePolicy - isReturnClientSecretInResponse():{}, isReturnEncryptedClientSecretInResponse():{}, client:{}",
                isReturnClientSecretInResponse(), isReturnEncryptedClientSecretInResponse(), client);
        if (client == null) {
            return client;
        }

        if (isReturnClientSecretInResponse()) {
            if (!isReturnEncryptedClientSecretInResponse()) {
                getDecryptedClientSecret(client);
            }
        } else {
            client.setClientSecret(null);
        }

        return client;
    }

    private Client getDecryptedClientSecret(Client client) {
        if (client != null) {
            try {
                client.setClientSecret(encryptionService.decrypt(client.getClientSecret()));
            } catch (Exception ex) {
                logger.error(" Error while decrypting ClientSecret for '{}', exception is - ", client.getClientId(),
                        ex);
                client.setClientSecret(null);
            }
        }
        return client;
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
            applyResponsePolicy(clients);
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
            throwBadRequestException("Invalid scope in request -> " + invalidScopes.toString());
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
            JansAttribute jansAttribute = null;
            if (authUtil.isValidDn(claim)) {
                jansAttribute = attributeService.getAttributeUsingDn(claim);
            } else {
                jansAttribute = attributeService.getAttributeUsingName(claim);
            }
            logger.debug("Attribute from DB - {}'", jansAttribute);
            if (jansAttribute != null) {
                validClaims.add(jansAttribute.getDn());
            } else {
                invalidClaims.add(claim);
            }
        }
        logger.debug("Claim validation result - validClaims:{}, invalidClaims:{} ", validClaims, invalidClaims);

        if (!invalidClaims.isEmpty()) {
            throwBadRequestException("Invalid claim in request -> " + invalidClaims.toString());
        }

        // reset Claims
        if (!validClaims.isEmpty()) {
            String[] scopeArr = validClaims.stream().toArray(String[]::new);
            client.setClaims(scopeArr);
        }

        return client;
    }

    private boolean isReturnEncryptedClientSecretInResponse() {
        logger.debug("appConfiguration.isReturnEncryptedClientSecretInResponse():{} ",
                appConfiguration.isReturnEncryptedClientSecretInResponse());
        return this.appConfiguration.isReturnEncryptedClientSecretInResponse();
    }

    private boolean isReturnClientSecretInResponse() {
        logger.debug("appConfiguration.isReturnClientSecretInResponse():{} ",
                appConfiguration.isReturnClientSecretInResponse());
        return this.appConfiguration.isReturnClientSecretInResponse();
    }

    private String encryptPassword(String clientName, String clientPassword) throws EncryptionException {
        String encryptedPassword = clientPassword;
        if (StringUtils.isBlank(clientPassword)) {
            return encryptedPassword;
        }
        logger.error("Check for clientName:{}, isPasswordEncrypted(clientPassword):{}", clientName,
                isPasswordEncrypted(clientName, clientPassword));

        if (!isPasswordEncrypted(clientName, clientPassword)) {
            encryptedPassword = encryptionService.encrypt(clientPassword);
        }
        return encryptedPassword;
    }

    private boolean isPasswordEncrypted(String clientName, String clientPassword) {
        boolean isPasswordEncrypted = true;
        if (StringUtils.isBlank(clientPassword)) {
            return isPasswordEncrypted;
        }
        logger.error("Check for clientName:{}, clientPassword:{}", clientName, clientPassword);
        try {
            encryptionService.decrypt(clientPassword);
        } catch (EncryptionException ex) {
            logger.error("Password of {} is not encrypted !!!", clientName);
            isPasswordEncrypted = false;
        }

        return isPasswordEncrypted;
    }
}
