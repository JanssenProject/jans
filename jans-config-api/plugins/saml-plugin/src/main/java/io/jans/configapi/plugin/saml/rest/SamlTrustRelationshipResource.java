package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.plugin.saml.model.config.SAMLTrustRelationship;
import io.jans.configapi.plugin.saml.service.SamlService;
import io.jans.configapi.plugin.saml.model.config.KeycloakConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


import java.util.*;
import java.util.stream.*;

import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;

@Path(Constants.TRUST_RELATIONSHIP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlTrustRelationshipResource extends BaseResource {

    @Inject
    Logger logger;

    @Inject
    KeycloakConfig keycloakConfig;
    
    @Inject
    SamlService samlService;

    @Operation(summary = "Get all Clients", description = "Get all Clients.", operationId = "get-saml-client", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLTrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    public Response getAllClients() {
        List<UserRepresentation> users = keycloakConfig.getInstance().realm("master").users();

        logger.info("All users:{}", users);
        return Response.ok(users).build();
    }

    @Operation(summary = "Get client by username", description = "Get client by username", operationId = "get-saml-client-by-name", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLTrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.NAME_PARAM_PATH)
    public Response searchClient(@Parameter(description = "Client name") @PathParam(Constants.NAME) @NotNull String name, boolean exact) {
        List entries = new ArrayList();

        logger.info("Searching by username: {} (exact {})", username, exact);
        List<UserRepresentation> users = keycloakConfig.getInstance().realm("master").users().searchByUsername(username,
                exact);

        logger.info("Users found by username:{}, users:{}", username, users);

        return Response.ok(users).build();
    }

    @Operation(summary = "Create Client", description = "Create Client", operationId = "get-saml-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLTrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response createClient(String name) {
    
        logger.info("Create user name:{}", name);
        UserRepresentation user = samlService.createUser(name,"user123");
        logger.info("Users created by name:{}, user:{}", name, user);

        return Response.ok(user).build();
    }

}
