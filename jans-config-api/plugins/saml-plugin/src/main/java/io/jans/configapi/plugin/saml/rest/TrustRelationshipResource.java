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

import java.io.IOException;
import java.util.*;

import io.jans.configapi.plugin.saml.model.TrustRelationship;

import org.slf4j.Logger;

@Path(Constants.SAML_CLIENT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TrustRelationshipResource extends BaseResource {

    private static final String SAML_TRUST_RELATIONSHIP= "Trus tRelationship";
    
    @Inject
    Logger logger;

    @Inject
    SamlService samlService;

    @Operation(summary = "Get all Trust Relationship", description = "Get all TrustRelationship.", operationId = "get-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    public Response getAllTrustRelationship() {
        List<TrustRelationship> trustRelationshipList = samlService.getAllTrustRelationship();
        logger.info("All trustRelationshipList:{}", trustRelationshipList);
        return Response.ok(trustRelationshipList).build();
    }


    @Operation(summary = "Get TrustRelationship by name", description = "Get TrustRelationship by name", operationId = "get-trust-relationship-by-name", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.CLIENTID_PATH)
    public Response getTrustRelationshipByName(
            @Parameter(description = "Name") @PathParam(Constants.CLIENTID) @NotNull String clientName) {
        logger.info("Searching client by name: {}", clientName);

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(clientName);

        logger.info("TrustRelationship found by clientName:{}, trustRelationship:{}", clientName, trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Get TrustRelationship by Id", description = "Get TrustRelationship by Id", operationId = "get-trust-relationship-by-id", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.ID_PATH + Constants.ID_PATH_PARAM)
    public Response getTrustRelationshipById(
            @Parameter(description = "Unique identifier - Id") @PathParam(Constants.ID) @NotNull String id) {
        logger.info("Searching client by id: {}", id);

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);

        logger.info("TrustRelationship found by id:{}, trustRelationship:{}", id, trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Create Trust Relationship", description = "Create Trust Relationship", operationId = "post-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "clientList", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response createTrustRelationship(@Valid TrustRelationship trustRelationship) throws IOException {

        logger.info("Create TrustRelationship:{}", trustRelationship);

        // TO-DO validation of client
        String inum = samlService.generateInumForNewRelationship();
        trustRelationship.setInum(inum);
        trustRelationship.setDn(samlService.getDnForTrustRelationship(inum));
        trustRelationship = samlService.addTrustRelationship(trustRelationship);

        logger.info("Create created by client:{}", trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Update TrustRelationship", description = "Update TrustRelationship", operationId = "put-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response updateTrustRelationship(@Valid TrustRelationship TrustRelationship) {

        logger.info("Update TrustRelationship:{}", TrustRelationship);

        // TO-DO validation of TrustRelationship
        TrustRelationship = samlService.updateTrustRelationship(TrustRelationship);

        logger.info("Post update client:{}", TrustRelationship);

        return Response.ok(TrustRelationship).build();
    }

    @Operation(summary = "Delete client", description = "Delete client", operationId = "put-client", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response deleteClient(
            @Parameter(description = "Unique Id of client") @PathParam(Constants.ID) @NotNull String id) {

        logger.info("Delete client identified by id:{}", id);
        
        TrustRelationship TrustRelationship = samlService.getTrustRelationshipByInum(id);
        if(TrustRelationship==null) {
            //throw error;
        }
        samlService.removeTrustRelationship(TrustRelationship);

        return Response.noContent().build();
    }
    
    private TrustRelationship saveTrustRelationship(TrustRelationship trustRelationship, boolean update) {
        logger.info("Delete client identified by trustRelationship:{}, update:{}", trustRelationship, update);
        
        //Validations
        checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        
        // If relationship exists and action is add then throw error
        List<TrustRelationship> existingRelationships = samlService.getAllTrustRelationshipByName(trustRelationship.getDisplayName());

        //If update check by inum
        
        //Save metadatfile
       /* String resultInitMetadataFilters = metadataFiltersAction.initMetadataFilters(this.trustRelationship);
        if (!StringHelper.equalsIgnoreCase(OxTrustConstants.RESULT_SUCCESS, resultInitMetadataFilters)) {
            return false;
        }
       */
        return trustRelationship;
    }

}
