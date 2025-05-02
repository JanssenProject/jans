package io.jans.configapi.plugin.saml.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.saml.model.MetadataSourceType;
import io.jans.configapi.plugin.saml.model.TrustRelationship;
import io.jans.configapi.plugin.saml.form.TrustRelationshipForm;
import io.jans.configapi.plugin.saml.util.Constants;
import io.jans.configapi.util.AttributeNames;
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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.SAML_PATH + Constants.TRUST_RELATIONSHIP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TrustRelationshipResource extends BaseResource {

    private static final String SAML_TRUST_RELATIONSHIP = "Trust Relationship";
    private static final String SAML_TRUST_RELATIONSHIP_FORM = "Trust Relationship From";
    private static final String SAML_TRUST_RELATIONSHIP_CHECK_STR = "Trust Relationship identified by '";
    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "Trust Relationship with same name `%s` already exists!";
    private static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    private static final String DATA_NULL_MSG = "`%s` should not be null!";
    
    @Inject
    Logger logger;

    @Inject
    SamlService samlService;

    @Operation(summary = "Get all Trust Relationship", description = "Get all TrustRelationship.", operationId = "get-trust-relationships", tags = {
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
            @ApiResponse(responseCode = "404", description = "Trust relationship not found",content=@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { Constants.SAML_READ_ACCESS })
    @Path(Constants.ID_PATH + Constants.ID_PATH_PARAM)
    public Response getTrustRelationshipById(
            @Parameter(description = "Unique identifier - Id") @PathParam(Constants.ID) @NotNull String id) {
        if (logger.isInfoEnabled()) {
            logger.info("Searching TrustRelationship by id: {}", escapeLog(id));
        }

        TrustRelationship trustrelationship = samlService.getTrustRelationshipByInum(id);
        if(trustrelationship != null) {
            logger.info("TrustRelationship found by id:{}, trustRelationship:{}", id, trustrelationship);
            return Response.ok(trustrelationship).build();
        }else {
            logger.info("TrustRelationship with id {} not found",id);
            ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()))
                .withMessage("Trust relationship not found")
                .andDescription(String.format("The TrustRelationship with id '%s' was not found",id))
                .build();
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    @Operation(summary = "Create Trust Relationship with Metadata File", description = "Create Trust Relationship with Metadata File", operationId = "post-trust-relationship-metadata-file", tags = {
            "SAML - Trust Relationship" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    Constants.SAML_WRITE_ACCESS }))
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request" , content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found" , content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))),
            })
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
                
        //validation
        checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        checkNotNull(trustRelationship.getName(), "Name");

        // check if TrustRelationship with same name already exists
        List<TrustRelationship> existingTrustRelationship = samlService.getAllTrustRelationshipByName(trustRelationship.getName());
        logger.debug(" existingTrustRelationship:{} ", existingTrustRelationship);
        if (existingTrustRelationship != null && !existingTrustRelationship.isEmpty()) {
            throwBadRequestException(NAME_CONFLICT,String.format(NAME_CONFLICT_MSG, trustRelationship.getName()));
        }

        InputStream metaDataFile = trustRelationshipForm.getMetaDataFile();
        logger.debug(" Create metaDataFile:{} ", metaDataFile);
        if (metaDataFile != null) {
            logger.debug(" Create metaDataFile.available():{}", metaDataFile.available());
        }

        validateSpMetaDataSourceType(trustRelationship, metaDataFile, false);
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
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/trust-relationship/trust-relationship-put.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request" , content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found" , content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))),
            })
    @ProtectedApi(scopes = { Constants.SAML_WRITE_ACCESS })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload")
    @PUT
    public Response updateTrustRelationship(@MultipartForm TrustRelationshipForm trustRelationshipForm,
            InputStream metadatafile) throws IOException {

        logger.info("Update trustRelationshipForm:{}", trustRelationshipForm);
        checkResourceNotNull(trustRelationshipForm, SAML_TRUST_RELATIONSHIP_FORM);

        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        logger.debug(" Create trustRelationship:{} ", trustRelationship);
        
        //validation
        checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        checkNotNull(trustRelationship.getName(), "Name");
        checkNotNull(trustRelationship.getInum(), AttributeNames.INUM);
               
        // check if TrustRelationship exists
        TrustRelationship existingTrustRelationship = samlService
                .getTrustRelationshipByInum(trustRelationship.getInum());
        logger.info("TrustRelationship found by trustRelationship.getInum():{}, existingTrustRelationship:{}",
                trustRelationship.getInum(), existingTrustRelationship);
        checkResourceNotNull(existingTrustRelationship,
                SAML_TRUST_RELATIONSHIP_CHECK_STR + trustRelationship.getInum() + "'");

        // check if another TrustRelationship with same name already exists
        final String inum = trustRelationship.getInum();
        List<TrustRelationship> trustRelationshipList = samlService
                .getAllTrustRelationshipByName(trustRelationship.getName());
        logger.info(" trustRelationshipList:{} ", trustRelationshipList);
        if (trustRelationshipList != null && !trustRelationshipList.isEmpty()) {
            List<String> inumList = trustRelationshipList.stream().map(TrustRelationship::getInum)
                    .collect(Collectors.toList());
            logger.info("TrustRelationship's with name:{}, inumList:{}", trustRelationship.getName(), inumList);
            List<TrustRelationship> list = trustRelationshipList.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                    .collect(Collectors.toList());
            logger.info("Other TrustRelationship's with same name:{} list:{}", trustRelationship.getName(), list);
            if (list != null && !list.isEmpty()) {
                throwBadRequestException(NAME_CONFLICT, String.format(NAME_CONFLICT_MSG, trustRelationship.getName()));
            }
        }
        
        InputStream metaDataFile = trustRelationshipForm.getMetaDataFile();
        logger.debug("metaDataFile for update is:{} ", metaDataFile);
        if (metaDataFile != null && metaDataFile.available() > 0) {
            logger.debug("For update metaDataFile.available():{}", metaDataFile.available());
            
        }else  if(trustRelationship.getSpMetaDataSourceType().equals(MetadataSourceType.FILE)) {

            trustRelationship.setSpMetaDataFN(existingTrustRelationship.getSpMetaDataFN());
        }
        
        validateSpMetaDataSourceType(trustRelationship, metaDataFile, true);
        // Update
        trustRelationship = samlService.updateTrustRelationship(trustRelationship, metaDataFile);

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
            logger.info("Delete TrustRelationship identified by id:{}", escapeLog(id));
        }

        TrustRelationship trustRelationship = samlService.getTrustRelationshipByInum(id);
        if (trustRelationship == null) {
            checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        }
        samlService.removeTrustRelationship(trustRelationship);

        return Response.noContent().build();
    }

    @Operation(summary="Get TrustRelationship file metadata", description="Get TrustRelationship file metadata",
        operationId = "get-trust-relationship-file-metadata", tags = {"SAML - Trust Relationship"},
        security = @SecurityRequirement(name = "oauth2", scopes= {Constants.SAML_READ_ACCESS}),
        responses = {
          @ApiResponse(responseCode="200",description="OK",content= @Content(mediaType = MediaType.APPLICATION_XML,schema = @Schema(type="string",format="binary"))),
          @ApiResponse(responseCode="400",description="Bad Request",content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
          @ApiResponse(responseCode="401",description="Unauthorized"),
          @ApiResponse(responseCode="404",description="Not Found",content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
          @ApiResponse(responseCode="500",description="Internal Server Error")
        }
    )
    @Path(Constants.SP_METADATA_FILE_PATH+Constants.ID_PATH_PARAM)
    @GET
    @ProtectedApi(scopes = {Constants.SAML_READ_ACCESS})
    public Response gettrustRelationshipFileMetadata(
        @Parameter(description="TrustRelationship inum") @PathParam(Constants.ID) @NotNull String id) {
        
        logger.info("getTrustRelationshipFileMeta()");
        TrustRelationship trustrelationship = samlService.getTrustRelationshipByInum(id);
        checkResourceNotNull(trustrelationship,SAML_TRUST_RELATIONSHIP);
        if(trustrelationship.getSpMetaDataSourceType() != MetadataSourceType.FILE) {
           throwBadRequestException("TrustRelationship metadatasource type isn't a FILE");
        }
        InputStream fs = samlService.getTrustRelationshipMetadataFile(trustrelationship);
        if(fs == null) {
            return getNotFoundError(String.format("metadata file for tr '%s' ",id));
        }
        return Response.ok(fs,MediaType.APPLICATION_XML).build();
    }

    
    private void validateSpMetaDataSourceType(TrustRelationship trustRelationship, InputStream metaDataFile, boolean isUpdate)
            throws IOException {
        logger.info("Validate SP MetaDataSourceType trustRelationship:{}, metaDataFile:{}, isUpdate:{}", trustRelationship,
                metaDataFile, isUpdate);

        checkResourceNotNull(trustRelationship.getSpMetaDataSourceType(), "SP MetaData Source Type");

        logger.info("Validate trustRelationship.getSpMetaDataSourceType():{}",
                trustRelationship.getSpMetaDataSourceType());
        
        if (trustRelationship.getSpMetaDataSourceType().equals(MetadataSourceType.FILE)) {

            //If MetaDataSourceType==FILE and it is not Update flow
            if ( (metaDataFile == null || metaDataFile.available() <= 0) && !isUpdate ) {
                throwBadRequestException(DATA_NULL_CHK, String.format(DATA_NULL_MSG, "SP MetaData File"));
            }

            // Since SP Metadata source is File set SamlMetadata manual elements to null
            trustRelationship.setSamlMetadata(null);

        } else if (trustRelationship.getSpMetaDataSourceType().equals(MetadataSourceType.MANUAL)) {

            if (metaDataFile != null && metaDataFile.available() > 0) {
                throwBadRequestException("SP MetaData File should not be provided!");
            }

            checkResourceNotNull(trustRelationship.getSamlMetadata(), "'SamlMetadata manual elements'");
            checkNotNull(trustRelationship.getSamlMetadata().getEntityId(), "'EntityId'");
            checkNotNull(trustRelationship.getSamlMetadata().getNameIDPolicyFormat(),
                    "'NameIDPolicyFormat'");
            checkNotNull(trustRelationship.getSamlMetadata().getSingleLogoutServiceUrl(),
                    "'SingleLogoutServiceUrl'");
            if (StringUtils.isBlank(trustRelationship.getSamlMetadata().getJansAssertionConsumerServiceGetURL())
                    && (StringUtils
                            .isBlank(trustRelationship.getSamlMetadata().getJansAssertionConsumerServiceGetURL()))) {
                throwBadRequestException("Either of AssertionConsumerService GET or POST URL should be provided!");
            }
        }

    }

}
