package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.plugin.saml.service.SamlKeycloakService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;

@Path(Constants.SAML_SCOPE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlScopeResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    SamlKeycloakService samlService;

    @Operation(summary = "Get Client Scope", description = "Get Client Scope", operationId = "get-saml-client-scope", tags = {
            "SAML - Client Scope" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_SCOPE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProtocolMapperRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.CLIENTID_PATH)
    @ProtectedApi(scopes = { Constants.SAML_SCOPE_READ_ACCESS })
    public Response getClientScope(
            @Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId) {
        List<ProtocolMapperRepresentation> protocolMappers = null;
        List<ClientRepresentation> clients = samlService.getClientByClientId(clientId);
        logger.info("Clients found by clientId:{}, clients:{}", clientId, clients);
        if (clients != null && !clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            logger.info(" Client search result based on clientId:{}, client:{}", clientId, client);
            protocolMappers = client.getProtocolMappers();

        }

        logger.info("protocolMappers:{}", protocolMappers);
        return Response.ok(protocolMappers).build();
    }

    @Operation(summary = "Add new Client Scope", description = "Add new Client Scope", operationId = "post-saml-client-scope", tags = {
            "SAML - Client Scope" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_SCOPE_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProtocolMapperRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path("test"+Constants.CLIENTID_PATH)
    @ProtectedApi(scopes = { Constants.SAML_SCOPE_WRITE_ACCESS })
    public Response addClientScopeNew(
            @Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId,
            @Valid @NotNull ProtocolMapperRepresentation protocolMapperRepresentation) {

        List<ClientRepresentation> clients = samlService.getClientByClientId(clientId);
        logger.info("clientId:{}, protocolMapperRepresentation:{}, clients:{}", clientId, protocolMapperRepresentation,
                clients);

        if (protocolMapperRepresentation != null && !clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            logger.info(" clientId:{}, client:{}", clientId, client);
            List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
            protocolMappers.add(protocolMapperRepresentation);
            client = samlService.updateClient(client);
            logger.info(" After update of protocolMappers - clientId:{}, client:{}", clientId, client);
        }

        logger.info("protocolMapperRepresentation:{}", protocolMapperRepresentation);
        return Response.ok(protocolMapperRepresentation).build();
    }
    
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProtocolMapperRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(Constants.CLIENTID_PATH)
    @ProtectedApi(scopes = { Constants.SAML_SCOPE_WRITE_ACCESS })
    public Response addClientScope(
            @Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId,
            @Valid @NotNull ProtocolMapperRepresentation protocolMapperRepresentation) {

        List<ClientRepresentation> clients = samlService.getClientByClientId(clientId);
        logger.info("clientId:{}, protocolMapperRepresentation:{}, clients:{}", clientId, protocolMapperRepresentation,
                clients);

        if (protocolMapperRepresentation != null && !clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            logger.info(" clientId:{}, client:{}", clientId, client);
            List<ProtocolMapperRepresentation> protocolMappers = client.getProtocolMappers();
            protocolMappers.add(protocolMapperRepresentation);
            client = samlService.updateClient(client);
            logger.info(" After update of protocolMappers - clientId:{}, client:{}", clientId, client);
        }

        logger.info("protocolMapperRepresentation:{}", protocolMapperRepresentation);
        return Response.ok(protocolMapperRepresentation).build();
    }

    @Operation(summary = "Update Client Scope", description = "Update Client Scope", operationId = "post-saml-client-scope", tags = {
            "SAML - Client Scope" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_SCOPE_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProtocolMapperRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(Constants.CLIENTID_PATH)
    @ProtectedApi(scopes = { Constants.SAML_SCOPE_WRITE_ACCESS })
    public Response updateClientScope(
            @Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId,
            @Valid List<ProtocolMapperRepresentation> protocolMapperRepresentationList) {
        List<ProtocolMapperRepresentation> protocolMappers = null;
        List<ClientRepresentation> clients = samlService.getClientByClientId(clientId);
        logger.info("Clients found by clientId:{}, clients:{}", clientId, clients);
        if (clients != null && !clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            logger.info(" clientId:{}, client:{}", clientId, client);
            client.setProtocolMappers(protocolMapperRepresentationList);
            client = samlService.updateClient(client);
            protocolMappers = client.getProtocolMappers();
        }

        logger.info("protocolMappers:{}", protocolMappers);
        return Response.ok(protocolMappers).build();
    }

}
