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
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.slf4j.Logger;

@Path(Constants.SAML_CLIENT_SCOPE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlScopeResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    SamlService samlService;

    @Operation(summary = "Get Client Scope", description = "Get Client Scope", operationId = "get-saml-client-scope", tags = {
            "SAML - Client Scope" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_CLIENT_SCOPE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProtocolMapperRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.CLIENTID_PATH)
    @ProtectedApi(scopes = { Constants.SAML_CLIENT_SCOPE_READ_ACCESS })
    public Response getClientScope(@Parameter(description = "Client Id") @PathParam(Constants.CLIENTID) @NotNull String clientId) {
        List<ProtocolMapperRepresentation> protocolMappers = null; 
        List<ClientRepresentation> clients = samlService.serachClients(clientId);
        logger.info("Clients found by clientId:{}, clients:{}", clientId, clients);
        if(clients!=null && !clients.isEmpty()) {
            ClientRepresentation client = clients.get(0);
            logger.info(" clientId:{}, client:{}", clientId, client);
            protocolMappers = client.getProtocolMappers();
            
        }
        
        logger.info("protocolMappers:{}", protocolMappers);
        return Response.ok(protocolMappers).build();
    }

 
}
