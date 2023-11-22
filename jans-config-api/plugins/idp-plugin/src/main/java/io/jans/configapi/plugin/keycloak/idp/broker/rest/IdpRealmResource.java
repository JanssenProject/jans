package io.jans.configapi.plugin.keycloak.idp.broker.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.keycloak.idp.broker.util.Constants;
import io.jans.configapi.plugin.keycloak.idp.broker.service.RealmService;

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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;

import org.keycloak.representations.idm.RealmRepresentation;

@Path(Constants.REALM_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdpRealmResource extends BaseResource {

    private static final String JANS_REALM_DETAILS = "Jans Realm Details";

    @Inject
    Logger logger;

    @Inject
    RealmService realmService;

    @Operation(summary = "Get all realm", description = "Get all realm", operationId = "get-realm", tags = {
            "Jans - SAML Identity Broker Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RealmRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_REALM_READ_ACCESS })
    public Response getAllRealms() {
        List<RealmRepresentation> realms = realmService.getAllRealmDetails();
        logger.info("All realms:{}", realms);
        return Response.ok(realms).build();
    }

    @Operation(summary = "Get realm by name", description = "Get realm by name", operationId = "get-realm-by-name", tags = {
            "Jans - SAML Identity Broker Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_REALM_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.JANS_IDP_REALM_READ_ACCESS })
    @Path(Constants.NAME_PATH + Constants.NAME_PATH_PARAM)
    public Response getRealmByName(
            @Parameter(description = "name") @PathParam(Constants.NAME) @NotNull String name) {
        logger.info("Searching Realm by name: {}", escapeLog(name));

        RealmRepresentation realmRepresentation = realmService.getRealmByName(name);

        logger.info("Realm found by name:{}, realmRepresentation:{}", name, realmRepresentation);

        return Response.ok(realmRepresentation).build();
    }

    @Operation(summary = "Create realm", description = "Create realm", operationId = "post-realm", tags = {
            "Jans - SAML Identity Broker Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_REALM_WRITE_ACCESS }))
    @RequestBody(description = "Realm details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class), examples = @ExampleObject(name = "Request example", value = "example/idp/post-realm.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created realm", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @ProtectedApi(scopes = { Constants.JANS_IDP_REALM_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.JANS_IDP_REALM_WRITE_ACCESS })
    @POST
    public Response createNewKCRealm(@NotNull RealmRepresentation realmRepresentation)
            throws IOException {
        logger.info(" Create new KC realm - realmRepresentation:{} ", realmRepresentation);
        checkResourceNotNull(realmRepresentation, JANS_REALM_DETAILS);
        realmRepresentation = this.realmService.createNewRealm(realmRepresentation);
        logger.info("Created new KC realm - realmRepresentation:{}", realmRepresentation);
        return Response.status(Response.Status.CREATED).entity(realmRepresentation).build();
    }

    @Operation(summary = "Update realm", description = "Update realm", operationId = "put-realm", tags = {
            "Jans - SAML Identity Broker Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_REALM_WRITE_ACCESS }))
    @RequestBody(description = "Realm details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class), examples = @ExampleObject(name = "Request example", value = "example/idp/put-realm.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Jans realm object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RealmRepresentation.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @ProtectedApi(scopes = { Constants.JANS_IDP_REALM_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.JANS_IDP_REALM_WRITE_ACCESS })
    @PUT
    public Response updateRealm(@NotNull RealmRepresentation realmRepresentation)
            throws IOException {
        logger.info(" Update KC realm - realmRepresentation:{} ", realmRepresentation);
        checkResourceNotNull(realmRepresentation, JANS_REALM_DETAILS);
        realmRepresentation = this.realmService.updateRealm(realmRepresentation);
        logger.info("Updated KC realm - realmRepresentation:{}", realmRepresentation);
        return Response.status(Response.Status.OK).entity(realmRepresentation).build();
    }

    @Operation(summary = "Delete realm ", description = "Delete realm", operationId = "delete-realm", tags = {
            "Jans - SAML Identity Broker Realm" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.JANS_IDP_REALM_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Path(Constants.NAME_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.JANS_IDP_REALM_WRITE_ACCESS })
    @DELETE
    public Response deleteRealm(
            @Parameter(description = "Unique name of KC realm") @PathParam(Constants.NAME) @NotNull String name) {

        logger.info("Delete realm by name:{}", escapeLog(name));

        RealmRepresentation realmRepresentation = realmService.getRealmByName(name);
        if (realmRepresentation == null) {
            checkResourceNotNull(realmRepresentation, "Relam does not exists by name - " + name);
        }
        realmService.deleteRealm(name);

        return Response.noContent().build();
    }

}
