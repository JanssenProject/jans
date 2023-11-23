package io.jans.configapi.plugin.saml.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.form.TrustRelationshipForm;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.util.Constants;
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

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.SAML_TRUST_RELATIONSHIP)
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
        List<TrustRelationship> trustRelationshipList = samlService.getAllTrustRelationships();
        logger.info("All trustRelationshipList:{}", trustRelationshipList);
        return Response.ok(trustRelationshipList).build();
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
        if (logger.isInfoEnabled()) {
            logger.info("Searching TrustRelationship by id: {}", escapeLog(id));
        }

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);

        logger.info("TrustRelationship found by id:{}, trustRelationship:{}", id, trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Create Trust Relationship with Metadata File", description = "Create Trust Relationship with Metadata File", operationId = "post-trust-relationship-metadata-file", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload")
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SAML_WRITE_ACCESS })
    @POST
    public Response createTrustRelationshipWithFile(@MultipartForm TrustRelationshipForm trustRelationshipForm,
            InputStream metadatafile) throws IOException {
        logger.info(" Create trustRelationshipForm:{} ", trustRelationshipForm);
        checkResourceNotNull(trustRelationshipForm, SAML_TRUST_RELATIONSHIP_FORM);

        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        logger.debug(" Create trustRelationship:{} ", trustRelationship);
        checkResourceNotNull(trustRelationshipForm.getTrustRelationship(), SAML_TRUST_RELATIONSHIP);
        
        InputStream metaDataFile = trustRelationshipForm.getMetaDataFile();
        logger.debug(" Create metaDataFile:{} ", metaDataFile);
        if (metaDataFile != null) {
            logger.debug(" Create metaDataFile.available():{}", metaDataFile.available());
        }

        // TO-DO validation of TrustRelationship
        String inum = samlService.generateInumForNewRelationship();
        trustRelationship.setInum(inum);
        trustRelationship.setDn(samlService.getDnForTrustRelationship(inum));

        trustRelationship = samlService.addTrustRelationship(trustRelationship, metaDataFile);

        logger.info("Create created by TrustRelationship:{}", trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Update TrustRelationship", description = "Update TrustRelationship", operationId = "put-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
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
        trustRelationship = samlService.updateTrustRelationship(trustRelationship);

        logger.info("Post update trustRelationship:{}", trustRelationship);

        return Response.ok(trustRelationship).build();
    }

    @Operation(summary = "Delete TrustRelationship", description = "Delete TrustRelationship", operationId = "delete-trust-relationship", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Path(Constants.ID_PATH_PARAM)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    @DELETE
    public Response deleteTrustRelationship(
            @Parameter(description = "Unique Id of Trust Relationship") @PathParam(Constants.ID) @NotNull String id) {

        if (logger.isInfoEnabled()) {
            logger.info("Delete client identified by id:{}", escapeLog(id));
        }

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);
        if (trustRelationship == null) {
            checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        }
        samlService.removeTrustRelationship(trustRelationship);

        return Response.noContent().build();
    }

    @Operation(summary = "Process unprocessed metadata files", description = "Process unprocessed metadata files", operationId = "post-metadata-files", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Path(Constants.PROCESS_META_FILE)
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    @POST
    public Response processMetadataFiles() {

        logger.info("process metadata files");

        samlService.processUnprocessedMetadataFiles();

        return Response.ok().build();
    }

}
