/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.service.auth.AssetService;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.form.AssetForm;
import io.jans.configapi.core.util.Jackson;

import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.service.document.store.service.Document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.*;

import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path(ApiConstants.JANS_ASSETS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssetResource extends ConfigBaseResource {

    private static final String APPLICATION_ERROR = "Application Error";
    private static final String DOCUMENT_DATA = "Document Data";
    private static final String DOCUMENT_DATA_FORM = "Document Data From";
    private static final String DOCUMENT_CHECK_STR = "Document identified by '";
    private static final String DOCUMENT_NAME_CONFLICT = "NAME_CONFLICT";
    private static final String DOCUMENT_NAME_CONFLICT_MSG = "Document with same name %s already exists!";
    private static final String DOCUMENT_INUM = "Document Identifier Inum";

    @Inject
    Logger log;

    @Inject
    AssetService assetService;

    @Operation(summary = "Gets all Jans assets.", description = "Gets all Jans assets.", operationId = "get-all-assets", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/assets/get-all-asset.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAssets(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "Status of the attribute") @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "adminCanEdit=true,dataType=string")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair)
            throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Search Asset filters with limit:{}, pattern:{}, status:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(status), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }
        SearchRequest searchReq = createSearchRequest(assetService.getDnForDocument(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount(), fieldValuePair, JansAttribute.class);
        return Response.ok(doSearch(searchReq, status)).build();
    }

    @Operation(summary = "Gets Jans asset by inum - unique identifier", description = "Gets Jans asset by inum - unique identifier", operationId = "get-asset-by-inum", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/assets/get-all-asset.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAssetByInum(
            @Parameter(description = "Asset Id") @PathParam(ApiConstants.INUM) @NotNull String inum) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Search Asset with inum:{}", escapeLog(inum));
        }

        Document document = assetService.getDocumentByInum(inum);
        logger.info(" Docucment fetched based on inme:{} is:{}", inum, document);
        return Response.ok(document).build();

    }

    @Operation(summary = "Upload new asset", description = "Upload new asset", operationId = "post-new-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = AssetForm.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = Document.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(ApiConstants.UPLOAD_ASSET)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_WRITE_ACCESS })
    public Response uploadAsset(@MultipartForm AssetForm assetForm) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Create Asset details assetForm:{}", assetForm);
        }

        // validation
        checkResourceNotNull(assetForm, DOCUMENT_DATA_FORM);
        Document document = assetForm.getDocument();
        log.debug(" Create document:{} ", document);
        checkResourceNotNull(document, DOCUMENT_DATA);
        checkNotNull(document.getDisplayName(), AttributeNames.DISPLAY_NAME);

        // check if asset with same name already exists
        List<Document> documents = assetService.getDocumentByName(document.getDisplayName());
        if (documents != null && !documents.isEmpty()) {
            throwBadRequestException(DOCUMENT_NAME_CONFLICT,
                    String.format(DOCUMENT_NAME_CONFLICT_MSG, document.getDisplayName()));
        }

        // check if IDP with same name already exists
        InputStream existingAssetStream = assetService.readAssetStream(document.getDisplayName());
        log.debug("Check if asset with same name exists - existingAssetStream?:{} ", existingAssetStream);
        if (existingAssetStream != null && existingAssetStream.available() > 0) {
            throwBadRequestException(DOCUMENT_NAME_CONFLICT,
                    String.format(DOCUMENT_NAME_CONFLICT_MSG, document.getDisplayName()));
        }

        InputStream assetFile = assetForm.getAssetFile();
        log.debug(" Upload assetFile:{} ", assetFile);

        // upload document
        try {
            boolean status = assetService.saveAsset(document, assetFile);
            log.debug(" Upload asset status:{} ", status);
        } catch (WebApplicationException wex) {
            log.error("Application Error while creating document is - status:{}, message:{}",
                    wex.getResponse().getStatus(), wex.getMessage());
            throwInternalServerException(APPLICATION_ERROR, wex.getMessage());
        }

        log.info("Create IdentityProvider - document:{}", document);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @Operation(summary = "Update existing asset", description = "Upload new asset", operationId = "put-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = AssetForm.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/put-asset.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = Document.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PUT
    @Path(ApiConstants.UPLOAD_ASSET)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_WRITE_ACCESS })
    public Response updateAsset(@MultipartForm AssetForm assetForm) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Create Asset details assetForm:{}", assetForm);
        }

        // validation
        checkResourceNotNull(assetForm, DOCUMENT_DATA_FORM);
        Document document = assetForm.getDocument();
        final String inum = document.getInum();
        log.debug(" Create document:{} ", document);
        checkResourceNotNull(document, DOCUMENT_DATA);
        checkResourceNotNull(inum, DOCUMENT_INUM);
        checkNotNull(document.getDisplayName(), AttributeNames.DISPLAY_NAME);

        // check if asset with same name already exists
        List<Document> documents = assetService.getDocumentByName(document.getDisplayName());
        log.info(
                "Check if document with inum different then:{} but with same name exists - document.getDisplayName():{}, documents:{}",
                inum, document.getDisplayName(), documents);
        if (documents != null && !documents.isEmpty()) {
            List<Document> list = documents.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                    .collect(Collectors.toList());
            logger.info("Other document with same name:{} are list:{}", document.getDisplayName(), list);
            throwBadRequestException(DOCUMENT_NAME_CONFLICT,
                    String.format(DOCUMENT_NAME_CONFLICT_MSG, document.getDisplayName()));
        }

        InputStream assetFile = assetForm.getAssetFile();
        log.debug(" Upload assetFile:{} ", assetFile);

        // upload document
        try {
            boolean status = assetService.saveAsset(document, assetFile);
            log.debug(" Upload asset status:{} ", status);
        } catch (WebApplicationException wex) {
            log.error("Application Error while creating document is - status:{}, message:{}",
                    wex.getResponse().getStatus(), wex.getMessage());
            throwInternalServerException(APPLICATION_ERROR, wex.getMessage());
        }

        log.info("Create IdentityProvider - document:{}", document);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @Operation(summary = "Delete an asset", description = "Delete an asset", operationId = "delete-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_DELETE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = AssetForm.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/put-asset.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust IDP", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = Document.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_DELETE_ACCESS })
    public Response deleteAsset(
            @Parameter(description = "Asset identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isInfoEnabled()) {
            log.info("Delete an Asset identified inum:{}", inum);
        }

        boolean status = assetService.removeAsset(inum);
        log.debug(" Delete asset status:{} ", status);
        return Response.noContent().build();

    }

    private PagedResult<Document> doSearch(SearchRequest searchReq, String status) throws Exception {

        logger.debug("Asset search params - searchReq:{} , status:{} ", searchReq, status);

        PagedResult<Document> pagedResult = assetService.searchAsset(searchReq, status);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Asset fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }

        logger.debug("Asset pagedResult:{} ", pagedResult);
        return pagedResult;
    }

}
