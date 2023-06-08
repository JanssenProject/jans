package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;

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
import org.slf4j.Logger;

import java.util.List;

@Path(Constants.TRUST_RELATIONSHIP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SamlTrustRelationshipResource extends BaseResource {

    @Inject
    Logger logger;


    @Operation(summary = "Get details of of Trust Relationship", description = "Get details of of Trust Relationship", operationId = "get-saml-trust-relationship", tags = {
    "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
            "https://jans.io/oauth/config/saml.readonly" }))
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = samlRegistrationEntry.class)))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(Constants.ENTRIES + ApiConstants.USERNAME_PATH)
    @ProtectedApi(scopes = {ApiAccessConstants.saml_CONFIG_READ_ACCESS})
    public Response findAllRegistered() {
        List entries = new ArrayList();
        return Response.ok(entries).build();
    }
}
