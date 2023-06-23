package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.plugin.saml.service.SamlService;

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
import org.keycloak.representations.idm.UserRepresentation;

import org.slf4j.Logger;

@Path(Constants.SAML_CLIENT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlClientResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    SamlService samlService;

    @Operation(summary = "Get all Clients", description = "Get all Clients.", operationId = "get-saml-client", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    public Response getAllClients() {

        List<ClientRepresentation> clientList = samlService.getAllClients();

        logger.info("All clientList:{}", clientList);
        return Response.ok(clientList).build();
    }

    @Operation(summary = "Get all users", description = "Get all users", operationId = "get-saml-user", tags = {
            "SAML - User" }, security = @SecurityRequirement(name = "oauth2", scopes = { Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path("/user")
    public Response getAllUsers() {

        logger.info("Searching users()");
        // to get only SAML use "protocol": "saml",
        List<UserRepresentation> userList = samlService.getAllUsers();

        logger.info("All userList:{}", userList);
        return Response.ok(userList).build();
    }

    @Operation(summary = "Get client by clientId", description = "Get client by clientId", operationId = "get-saml-client-by-clientId", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.CLIENTID_PATH)
    public Response getClientByClientId(
            @Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId) {
        logger.info("Searching client by clientId: {}", clientId);

        List<ClientRepresentation> clients = samlService.getClientByClientId(clientId);

        logger.info("Clients found by name:{}, clients:{}", clientId, clients);

        return Response.ok(clients).build();
    }

    @Operation(summary = "Get client by Id", description = "Get client by Id", operationId = "get-saml-client-by-id", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ClientRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.ID_PATH + Constants.ID_PATH_PARAM)
    public Response searchClient(
            @Parameter(description = "Unique identifier of Client - Id") @PathParam(Constants.ID) @NotNull String id) {
        logger.info("Searching client by id: {}", id);

        ClientRepresentation client = samlService.getClientById(id);

        logger.info("Client found by id:{}, client:{}", id, client);

        return Response.ok(client).build();
    }

    @Operation(summary = "Create Client", description = "Create Client", operationId = "post-client", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "clientList", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response createClient(@Valid ClientRepresentation clientRepresentation) {

        logger.info("Create client clientRepresentation:{}", clientRepresentation);

        // TO-DO validation of client
        ClientRepresentation client = samlService.createClient(clientRepresentation);

        logger.info("Create created by client:{}", client);
        return Response.status(Response.Status.CREATED).entity(client).build();
    }

    @Operation(summary = "Update client", description = "Update client", operationId = "put-client", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response updateClient(@Valid ClientRepresentation clientRepresentation) {

        logger.info("Update client:{}", clientRepresentation);

        // TO-DO validation of client
        ClientRepresentation client = samlService.updateClient(clientRepresentation);

        logger.info("Post update client:{}", client);

        return Response.ok(client).build();
    }

    @Operation(summary = "Delete client", description = "Delete client", operationId = "put-client", tags = {
            "SAML - Client" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ClientRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response deleteClient(
            @Parameter(description = "Unique Id of client") @PathParam(Constants.ID) @NotNull String id) {

        logger.info("Delete client identified by id:{}", id);

        // TO-DO validation of client
        samlService.deleteClient(id);

        return Response.noContent().build();
    }

}
