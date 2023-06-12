package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.plugin.saml.model.config.SAMLTrustRelationship;
import io.jans.configapi.plugin.saml.model.config.KeycloakConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;


import jakarta.inject.Inject;
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


    @Operation(summary = "Get details of of Trust Relationship", description = "Get details of of Trust Relationship", operationId = "get-saml-trust-relationship", tags = {
    "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLTrustRelationship.class)))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = {Constants.SAML_READ_ACCESS})
    public Response findAllRegistered() {
        List entries = new ArrayList();
        
        
        return Response.ok(entries).build();
    }
    
    @Operation(summary = "Get details of of Trust Relationship by username", description = "Get details of of Trust Relationship", operationId = "get-saml-trust-relationship", tags = {
    "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SAMLTrustRelationship.class)))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = {Constants.SAML_READ_ACCESS})
    public Response findAllRegistered(String username, boolean exact) {
        List entries = new ArrayList();
        
        logger.info("Searching by username: {} (exact {})", username, exact);
        List<UserRepresentation> users = keycloakConfig.getInstance().realm("master")
          .users()
          .searchByUsername(username, exact);

      
        logger.info("Users found by username:{}, users:{}", username, users);

        return Response.ok(users).build();
    }
    

        

}
