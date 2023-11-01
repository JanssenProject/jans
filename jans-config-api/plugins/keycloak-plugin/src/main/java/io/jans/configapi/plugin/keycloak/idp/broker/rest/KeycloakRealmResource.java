package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.plugin.keycloak.idp.broker.service.KeycloakService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

import org.keycloak.representations.idm.RealmRepresentation;

@Path(Constants.KEYCLOAK + Constants.REALM_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeycloakRealmResource extends BaseResource {


    @Inject
    Logger logger;

    @Inject
    KeycloakService keycloakService;

    @Operation(summary = "Get all Keycloak realm", description = "Get all Keycloak realm.", operationId = "get-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RealmRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.KC_REALM_READ_ACCESS })
    public Response getAllKeycloakRealms() {
        List<RealmRepresentation> realms = keycloakService.getAllRealmRepresentation();
        logger.info("All realms:{}", realms);
        return Response.ok(realms).build();
    }

    @Operation(summary = "Get Keycloak realm by name", description = "Get Keycloak realm by name", operationId = "get-keycloak-realm-by-name", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.KC_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.KC_REALM_READ_ACCESS })
    @Path(Constants.NAME_PATH + Constants.NAME_PARAM_PATH)
    public Response getKeycloakRealmByName(
            @Parameter(description = "name") @PathParam(Constants.NAME) @NotNull String name) {
        logger.info("Searching Keycloak Realm by name: {}", escapeLog(name));

        RealmRepresentation realmRepresentation = keycloakService.getKeycloakRealmByName(name);

        logger.info("Keycloak realm found by name:{}, realmRepresentation:{}", name, realmRepresentation);

        return Response.ok(realmRepresentation).build();
    }

    @Operation(summary = "Create Keycloak realm", description = "Create Keycloak realm", operationId = "post-keycloak-realm", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @RequestBody(description = "Keycloak realm", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class), examples = @ExampleObject(name = "Request example", value = "example/keycloak/keycloak-realm-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created TrustKeycloak realm", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload")
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SAML_WRITE_ACCESS })
    @POST
    public Response createTrustRelationshipWithFile(@NotNull RealmRepresentation realmRepresentation,
            InputStream metadatafile) throws IOException {
        logger.info(" Create trustRelationshipForm:{} ", trustRelationshipForm);
        checkResourceNotNull(trustRelationshipForm, SAML_TRUST_RELATIONSHIP_FORM);

    

        trustRelationship = keycloakService.addTrustRelationship(trustRelationship, metaDataFile);

        logger.info("Create created by TrustRelationship:{}", trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Update TrustRelationship", description = "Update TrustRelationship", operationId = "put-trust-relationship", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class), examples = @ExampleObject(name = "Request example", value = "example/trust-relationship/trust-relationship-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    @PUT
    public Response updateTrustRelationship(@Valid TrustRelationship trustRelationship) throws IOException {

        logger.info("Update trustRelationship:{}", trustRelationship);

        // TO-DO validation of TrustRelationship
        trustRelationship = keycloakService.updateTrustRelationship(trustRelationship);

        logger.info("Post update trustRelationship:{}", trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Delete TrustRelationship", description = "Delete TrustRelationship", operationId = "put-trust-relationship", tags = {
            "Jans - Keycloak Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Path(Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    @DELETE
    public Response deleteTrustRelationship(
            @Parameter(description = "Unique Id of Trust Relationship") @PathParam(Constants.ID) @NotNull String id) {

        logger.info("Delete client identified by id:{}", escapeLog(id));

        TrustRelationship trustRelationship = keycloakService.getTrustRelationshipByInum(id);
        if (trustRelationship == null) {
            checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        }
        keycloakRealmService.removeTrustRelationship(trustRelationship);

        return Response.noContent().build();
    }

   

}
