package io.jans.configapi.plugin.saml.rest;

import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.model.GluuAttribute;
import io.jans.configapi.plugin.saml.service.SamlService;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.form.TrustRelationshipForm;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.SAML_CLIENT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TrustRelationshipResource extends BaseResource {

    private static final String SAML_TRUST_RELATIONSHIP = "Trust Relationship";
    private static final String SAML_TRUST_RELATIONSHIP_FORM = "Trust Relationship From";

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
        logger.error("All trustRelationshipList:{}", trustRelationshipList);
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
        logger.error("Searching client by name: {}", clientName);

        List<TrustRelationship> trustRelationships = samlService.getAllTrustRelationshipByName(clientName);

        logger.error("TrustRelationships found by clientName:{}, trustRelationship:{}", clientName, trustRelationships);

        return Response.ok(trustRelationships).build();
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
        logger.error("Searching TrustRelationship by id: {}", id);

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);

        logger.error("TrustRelationship found by id:{}, trustRelationship:{}", id, trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Create Trust Relationship without File", description = "Create Trust Relationship without File", operationId = "post-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "clientList", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SAML_WRITE_ACCESS })
    public Response createTrustRelationship(@Valid TrustRelationship trustRelationship) throws Exception {
        logger.debug(" Create trustRelationship:{}", trustRelationship);
        checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        checkNotNull(trustRelationship.getClientId(), AttributeNames.NAME);
        // TO-DO validation of TrustRelationship
        String inum = samlService.generateInumForNewRelationship();
        trustRelationship.setInum(inum);
        trustRelationship.setDn(samlService.getDnForTrustRelationship(inum));
        trustRelationship = samlService.addTrustRelationship(trustRelationship, null);

        logger.error("Create created by TrustRelationship:{}", trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Create Trust Relationship with File", description = "Create Trust Relationship with File", operationId = "post-trust-relationship-file", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "clientList", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload")
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SAML_WRITE_ACCESS })
    public Response createTrustRelationshipWithFile(@MultipartForm TrustRelationshipForm trustRelationshipForm,
            InputStream metadatafile) throws IOException {
        logger.debug(" Create trustRelationshipForm:{} ", trustRelationshipForm);
        checkResourceNotNull(trustRelationshipForm, SAML_TRUST_RELATIONSHIP_FORM);
       
        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        logger.debug(" Create trustRelationship:{} ", trustRelationship);
        checkResourceNotNull(trustRelationshipForm.getTrustRelationship(), SAML_TRUST_RELATIONSHIP);
        checkNotNull(trustRelationshipForm.getTrustRelationship().getClientId(), AttributeNames.NAME);
        
        InputStream inputStream = null;
        File metaDataFile = trustRelationshipForm.getMetaDataFile();
        logger.debug(" Create metaDataFile:{} ", metaDataFile);        
        if(metaDataFile!=null) {
            logger.debug(" Create metaDataFile.getName():{} , metaDataFile.length():{}", metaDataFile.getName(), metaDataFile.length());
            inputStream = new FileInputStream(metaDataFile);
        }
        
        // TO-DO validation of TrustRelationship
        String inum = samlService.generateInumForNewRelationship();
        trustRelationship.setInum(inum);
        trustRelationship.setDn(samlService.getDnForTrustRelationship(inum));
        trustRelationship = samlService.addTrustRelationship(trustRelationship, inputStream);

        logger.error("Create created by TrustRelationship:{}", trustRelationship);
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

        logger.error("Update TrustRelationship:{}", TrustRelationship);

        // TO-DO validation of TrustRelationship
        TrustRelationship = samlService.updateTrustRelationship(TrustRelationship);

        logger.error("Post update TrustRelationship:{}", TrustRelationship);

        return Response.ok(TrustRelationship).build();
    }

    @Operation(summary = "Delete TrustRelationship", description = "Delete TrustRelationship", operationId = "put-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = TrustRelationship.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    public Response deleteTrustRelationship(
            @Parameter(description = "Unique Id of client") @PathParam(Constants.ID) @NotNull String id) {

        logger.error("Delete client identified by id:{}", id);

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);
        if (trustRelationship == null) {
            checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        }
        samlService.removeTrustRelationship(trustRelationship);

        return Response.noContent().build();
    }

}
