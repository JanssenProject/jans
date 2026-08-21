package io.jans.configapi.plugin.shibboleth.rest;



import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipQuery;
import io.jans.shibboleth.trust.persistence.config.TrustRelationshipSummaryEntry;

import io.jans.staging.FileStagingService;

import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.TRUST_RELATIONSHIP_PATH + Constants.FILE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethFileStagingResource extends BaseResource {


    private static final String METADATA_FILE = "METADATA_FILE";
    private static final String METADATA_FILE_ERR = "METADATA_FILE_ERR";
    
    
    @Inject
    private Logger logger;

    @Inject
    private FileStagingService fileStagingService;
    
    
    @Operation(summary = "Uplaod ", description = "Uploads Metadata file for TrustRelationship", operationId = "post-upload-trust-metadata-file", tags = {
    "Shibboleth - Trust Relationship" }, security = {
            @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
            @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
@RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CreateTrustRelationshipRequest.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Uploaded Trust Relationship Metadata", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
    @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
@POST
@ProtectedApi(scopes = { Constants.SHIBBOLETH_FILES_UPLOAD }, groupScopes = {}, superScopes = {
    Constants.SHIBBOLETH_FILES_ADMIN })
public Response uploadTrustRelationshipMetadata(InputStream metaDataFile) throws IOException {
logger.info("POST TrustRelationship metaDataFile");
if (logger.isInfoEnabled()) {
    logger.info("Upload TrustRelationship  metaDataFile:{}",
            escapeLog(metaDataFile));
}
// validation
if ( metaDataFile == null || metaDataFile.available() <= 0) {
    throwBadRequestException(Constants.DATA_NULL_CHK, String.format(Constants.DATA_NULL_MSG, "Trust Metadata File"));
}

return Response.status(Response.Status.CREATED).entity("OK").build();
}
    
}
