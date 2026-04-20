package io.jans.configapi.plugin.shibboleth.rest;

import java.io.InputStream;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.shibboleth.form.TrustRelationshipForm;

import io.jans.configapi.plugin.shibboleth.model.EntityType;
import io.jans.configapi.plugin.shibboleth.model.MetadataSource;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;

import io.jans.configapi.plugin.shibboleth.service.ShibbolethService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.InvocationTargetException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Path(Constants.TRUST_RELATIONSHIP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethResource extends BaseResource {

    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_FORM = "Trust Relationship From";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP = "Trust Relationship";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_CHECK_STR = "Trust Relationship identified by '";
    private static final String SHIBBOLETH_TRUST_RELATIONSHIP_INUM = "Trust Relationship inum";
    private static final String DISPLAY_NAME = "Display Name";
    private static final String TRUST_NATURE = "Trust Nature";
    private static final String INVALID_TRUST_NATURE = "INVALID_TRUST_NATURE";
    private static final String INVALID_TRUST_NATURE_MSG = "Trust Nature is invalid.";

    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "Trust Relationship with same name `%s` already exists!";
    private static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    private static final String DATA_NULL_MSG = "`%s` should not be null!";

    private class TrustRelationshipPagedResult extends PagedResult<TrustRelationship> {
    };

    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;

    @Operation(summary = "Gets trusted service providers", description = "Gets list of trusted service providers", operationId = "get-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProviders(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Page number to be retrieved, the number of pages is the total number of records divided by the page size (rounded up)") @DefaultValue(ApiConstants.PAGE_INDEX) @QueryParam(value = ApiConstants.PAGE) int page,
            @Parameter(description = "Field and value pair for searching", examples = @ExampleObject(name = "Field value example", value = "applicationType=web,persistClientAuthorizations=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Shibboleth trust search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, page:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(page), escapeLog(fieldValuePair));
        }
        SearchRequest searchReq = createSearchRequest(shibbolethService.getDnForTrustRelationship(null), pattern,
                sortBy, sortOrder, startIndex, limit, null, null, shibbolethService.getRecordMaxCount(), fieldValuePair,
                TrustRelationship.class);
        searchReq.setPage(page);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Adds trusted service provider", description = "Adds a new trusted service provider", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationship.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @POST
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response addTrustRelationship(@MultipartForm TrustRelationship trustRelationship) throws IOException {
        logger.info("POST /shibboleth/trust");

        // validation
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);
        validateTrustRelationship(trustRelationship, false);

        shibbolethService.addTrustRelationship(trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    @Operation(summary = "Update trusted service Metadata file details", description = "Update trusted service Metadata file details", operationId = "put-shibboleth-trust-file", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    @PUT
    @Path(Constants.INUM_PATH_PARAM + Constants.SOURCE + Constants.FILE)
    public Response updateFileTrustRelationship(
            @Parameter(description = "TrustRelationship inum") @PathParam(Constants.INUM) @NotNull String inum,
            @MultipartForm TrustRelationshipForm trustRelationshipForm, InputStream metadatafile) throws IOException {
        logger.info("POST /shibboleth/trust/FILE");
        if (logger.isInfoEnabled()) {
            logger.info("Update TrustRelationship identified by inum:{}", escapeLog(inum));
        }
        // validation
        checkResourceNotNull(trustRelationshipForm, SHIBBOLETH_TRUST_RELATIONSHIP_FORM);

        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        validateTrustRelationship(trustRelationship, true);
        validateFileMetaDataSourceType(trustRelationshipForm, metadatafile);
        shibbolethService.updateTrustRelationship(trustRelationship);
        return Response.status(Response.Status.OK).entity(trustRelationship).build();
    }

    @Operation(summary = "Update Trust Relationship Manual Metadata", description = "Update Trust Relationship Manual Metadata", operationId = "put-shibboleth-trust-manual", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @Consumes(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    @PUT
    @Path(Constants.INUM_PATH_PARAM + Constants.SOURCE + Constants.MANUAL)
    public Response updateManualMetadataTrustRelationship(
            @Parameter(description = "TrustRelationship inum") @PathParam(Constants.INUM) @NotNull String inum,
            TrustRelationship trustRelationship, String metadataText) throws IOException {
        logger.info("POST /shibboleth/trust/MANUAL");
        if (logger.isInfoEnabled()) {
            logger.info("Update TrustRelationship identified by inum:{}", escapeLog(inum));
        }
        // validation
        validateTrustRelationship(trustRelationship, true);
        validateManualMetaDataSourceType(trustRelationship, metadataText);
        shibbolethService.updateTrustRelationship(trustRelationship);
        return Response.status(Response.Status.OK).entity(trustRelationship).build();
    }

    @Operation(summary = "Update Trust Relationship Metadata", description = "Update Trust Relationship Metadata", operationId = "put-shibboleth-trust-manual", tags = {
            "Shibboleth - Trust Relationship" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @Consumes(MediaType.APPLICATION_JSON)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    @PUT
    @Path(Constants.INUM_PATH_PARAM)
    public Response updateTrustRelationshipMetadata(
            @Parameter(description = "TrustRelationship inum") @PathParam(Constants.INUM) @NotNull String inum,
            @MultipartForm TrustRelationshipForm trustRelationshipForm,
            InputStream metadatafile) throws IOException {
        logger.info("POST /shibboleth/trust");
        if (logger.isInfoEnabled()) {
            logger.info("Update TrustRelationship identified by inum:{}", escapeLog(inum));
        }
        // validation - To do
        //validateTrustRelationship(trustRelationship, true);
        //validateManualMetaDataSourceType(trustRelationship, metadataText);
        //shibbolethService.updateTrustRelationship(trustRelationship);
        return Response.status(Response.Status.OK).entity("trustRelationship").build();
    }

    /* Helper methods */

    private TrustRelationshipPagedResult doSearch(SearchRequest searchReq) {

        if (logger.isInfoEnabled()) {
            logger.info("TrustRelationship search params - searchReq:{}", escapeLog(searchReq));
        }

        PagedResult<TrustRelationship> pagedResult = shibbolethService.getTrustRelationship(searchReq);
        if (logger.isTraceEnabled()) {
            logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        }

        TrustRelationshipPagedResult pagedTrustRelationship = new TrustRelationshipPagedResult();
        if (pagedResult != null) {
            logger.debug("TrustRelationship fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            List<TrustRelationship> trustRelationships = pagedResult.getEntries();

            pagedTrustRelationship.setStart(pagedResult.getStart());
            pagedTrustRelationship.setEntriesCount(pagedResult.getEntriesCount());
            pagedTrustRelationship.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedTrustRelationship.setEntries(trustRelationships);
        }

        logger.info("TrustRelationship pagedTrustRelationship:{}", pagedTrustRelationship);
        return pagedTrustRelationship;

    }

    private void validateTrustRelationship(TrustRelationship trustRelationship, boolean isUpdate) {
        // Null check
        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);

        // mandatory attributes
        List<String> missingAttributesList = new ArrayList<>();
        if (StringUtils.isBlank(trustRelationship.getDisplayName())) {
            missingAttributesList.add(DISPLAY_NAME);
        }
        if (trustRelationship.getEntityType() == null) {
            missingAttributesList.add(TRUST_NATURE);
        }

        if (!missingAttributesList.isEmpty()) {
            checkNotNull(missingAttributesList);
        }

        // check if Enity type is valid
        if (EntityType.getByValue(trustRelationship.getEntityType().getValue()) == null) {
            throwBadRequestException(INVALID_TRUST_NATURE,
                    String.format(INVALID_TRUST_NATURE_MSG, trustRelationship.getEntityType()));

        }

        // inum - if update
        final String inum = trustRelationship.getInum();
        if (isUpdate) {
            checkNotNull(inum, SHIBBOLETH_TRUST_RELATIONSHIP_INUM);
        }

        // check if TrustRelationship with same name already exists
        List<TrustRelationship> existingTrustRelationshipList = shibbolethService
                .getAllTrustRelationshipByDisplayName(trustRelationship.getDisplayName());
        logger.debug(" existingTrustRelationship:{} ", existingTrustRelationshipList);
        if (existingTrustRelationshipList != null && !existingTrustRelationshipList.isEmpty()) {
            List<TrustRelationship> list = null;
            if (isUpdate) {
                List<String> inumList = existingTrustRelationshipList.stream().map(TrustRelationship::getInum)
                        .collect(Collectors.toList());
                logger.info("TrustRelationship's with name:{}, inumList:{}", trustRelationship.getDisplayName(),
                        inumList);
                list = existingTrustRelationshipList.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                        .collect(Collectors.toList());
                logger.info("Other TrustRelationship's with same name:{} list:{}", trustRelationship.getDisplayName(),
                        list);

            }
            if ((!isUpdate) || (isUpdate && list != null && !list.isEmpty())) {
                throwBadRequestException(NAME_CONFLICT,
                        String.format(NAME_CONFLICT_MSG, trustRelationship.getDisplayName()));
            }

        }

    }

    private void validateFileMetaDataSourceType(TrustRelationshipForm trustRelationshipForm, InputStream metaDataFile)
            throws IOException {
        logger.info("validateSpMetaDataSourceType trustRelationshipForm:{}", trustRelationshipForm);

        checkResourceNotNull(trustRelationshipForm, SHIBBOLETH_TRUST_RELATIONSHIP_FORM);
        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        logger.info("Validate trustRelationship.getMetadataSource():{}", trustRelationship.getMetadataSource());

        if (!trustRelationship.getMetadataSource().equals(MetadataSource.FILE)) {
            throwBadRequestException("MetadataSource", "MetadataSource should be 'FILE'");
        }

        // If MetaDataSourceType==FILE and it is not Update flow
        if ((metaDataFile == null || metaDataFile.available() <= 0)) {
            throwBadRequestException(DATA_NULL_CHK, String.format(DATA_NULL_MSG, "MetaData File"));
        }
    }

    private void validateManualMetaDataSourceType(TrustRelationship trustRelationship, String metadataStr)
            throws IOException {
        logger.info("validateManualMetaDataSourceType trustRelationship:{}, metadataStr:{}", trustRelationship,
                metadataStr);

        checkResourceNotNull(trustRelationship, SHIBBOLETH_TRUST_RELATIONSHIP);
        checkNotNull(metadataStr, "'TrustRelationship SP MetaData String'");
        logger.info("Validate trustRelationship.getMetadataSource():{}", trustRelationship.getMetadataSource());

        if (!trustRelationship.getMetadataSource().equals(MetadataSource.MANUAL)) {
            throwBadRequestException("MetadataSource", "MetadataSource should be 'MANUAL'");

        }

        InputStream metaDataInputStream = new ByteArrayInputStream(metadataStr.getBytes());
        if ((metaDataInputStream == null || metaDataInputStream.available() <= 0)) {
            throwBadRequestException(DATA_NULL_CHK,
                    String.format(DATA_NULL_MSG, "SP MetaData String should be provided"));
        }

    }

}
