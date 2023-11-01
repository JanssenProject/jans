/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.plugin.keycloak.idp.broker.service.IdpService;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;

import io.jans.configapi.util.ApiAccessConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

import org.slf4j.Logger;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

@Path(Constants.KEYCLOAK + Constants.SAML_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpResource extends BaseResource {

   @Inject
    Logger log;

    @Inject
    IdpService idpService;

    @Operation(summary = "Retrieves KC SAML Identity Provider", description = "Retrieves SAML Identity Provider", operationId = "get-saml-identity-provider", tags = {
            "Jans - Keycloak Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_SAML_IDP_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdentityProviderRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = {Constants.KC_SAML_IDP_READ_ACCESS})
    public Response getAllKcSamlIdentityProvider() {
       
        
        return Response.ok("OK").build();
    }

    @Operation(summary = "Create SAML Identity Provider", description = "Create SAML Identity Provider", operationId = "postt-saml-identity-provider", tags = {
            "Jans - Keycloak Identity Broker" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_SAML_IDP_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = IdentityProviderRepresentation.class), examples = {
            @ExampleObject(value = "") }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created TrustKeycloak realm", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = IdentityProviderRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.KC_SAML_IDP_WRITE_ACCESS })
    public Response createKcSamlIdentityProvider(@Valid IdentityProviderRepresentation identityProviderRepresentation)
            throws IOException, JsonPatchException {
        log.debug("Create identityProviderRepresentation:{}", identityProviderRepresentation);
       
        log.debug("Created identityProviderRepresentation:{}", identityProviderRepresentation);
        return Response.ok(identityProviderRepresentation).build();
    }
}