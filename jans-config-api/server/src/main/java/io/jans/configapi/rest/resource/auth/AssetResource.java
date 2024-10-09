/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.service.auth.AssetService;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.form.AssetForm;
import io.jans.configapi.model.configuration.AssetDirMapping;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.jans.service.document.store.model.Document;
import io.jans.util.exception.InvalidAttributeException;
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

import static io.jans.as.model.util.Util.escapeLog;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.*;

import org.slf4j.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path(ApiConstants.JANS_ASSETS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssetResource extends ConfigBaseResource {

    private static final String APPLICATION_ERROR = "APPLICATION_ERROR";
    private static final String NOT_FOUND_ERROR = "NOT_FOUND_ERROR";
    private static final String ASSET_DATA = "Asset Data";
    private static final String ASSET_DATA_FORM = "Asset Data From";
    private static final String ASSET_NAME_CONFLICT = "NAME_CONFLICT";
    private static final String ASSET_NAME_CONFLICT_MSG = "Asset with same name %s already exist!";
    private static final String ASSET_NOT_FOUND = "Asset identified by %s not found!";
    private static final String ASSET_INUM = "Asset Identifier Inum";
    private static final String RESOURCE_NULL = "RESOURCE_NULL";
    private static final String RESOURCE_NULL_MSG = "%s is null";

    private class DocumentPagedResult extends PagedResult<Document> {
    };

    @Inject
    Logger log;

    @Inject
    AssetService assetService;

    @Operation(summary = "Gets all Jans assets.", description = "Gets all Jans assets.", operationId = "get-all-assets", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DocumentPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/assets/get-all-asset.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
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
        SearchRequest searchReq = createSearchRequest(assetService.getDnForAsset(null), pattern, sortBy, sortOrder,
                startIndex, limit, null, null, this.getMaxCount(), fieldValuePair, JansAttribute.class);
        return Response.ok(doSearch(searchReq, status)).build();
    }

    @Operation(summary = "Gets an asset by inum - unique identifier", description = "Gets an asset by inum - unique identifier", operationId = "get-asset-by-inum", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/assets/get-asset-by-inum.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAssetByInum(
            @Parameter(description = "Asset Inum") @PathParam(ApiConstants.INUM) @NotNull String inum)
            throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Search Asset with inum:{}", escapeLog(inum));
        }

        Document asset = assetService.getAssetByInum(inum);
        if (asset == null) {
            log.error("No asset found with the inum:{}", inum);
            throwNotFoundException(NOT_FOUND_ERROR, String.format(ASSET_NOT_FOUND, inum));
        }
        logger.info("Asset fetched based on inum:{} is:{}", inum, asset);
        return Response.ok(asset).build();
    }

    @Operation(summary = "Fetch asset by name", description = "Fetch asset by name.", operationId = "get-asset-by-name", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DocumentPagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/assets/get-asset-by-name.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.NAME + ApiConstants.NAME_PARAM_PATH)
    public Response getAssetByName(
            @Parameter(description = "Asset Name") @PathParam(ApiConstants.NAME) @NotNull String name)
            throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Search Asset with name:{}", escapeLog(name));
        }

        SearchRequest searchReq = createSearchRequest(assetService.getDnForAsset(null), name, ApiConstants.INUM,
                ApiConstants.ASCENDING, Integer.parseInt(ApiConstants.DEFAULT_LIST_START_INDEX),
                Integer.parseInt(ApiConstants.DEFAULT_LIST_SIZE), null, null, this.getMaxCount(), null,
                JansAttribute.class);

        DocumentPagedResult documentPagedResult = searchByName(searchReq);

        if (documentPagedResult == null || documentPagedResult.getEntriesCount() <= 0) {
            log.error("No asset found with the name:{}", name);
            throwNotFoundException(NOT_FOUND_ERROR, String.format(ASSET_NOT_FOUND, name));
        }
        logger.info("Asset fetched based on name are:{}", documentPagedResult);
        return Response.ok(documentPagedResult).build();
    }

    @Operation(summary = "Gets asset services", description = "Gets asset services", operationId = "get-asset-services", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class, type = "enum")))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.SERVICES)
    public Response getJansServices() {

        List<String> services = assetService.getValidModuleName();
        if (services == null) {
            services = Collections.emptyList();
        }

        logger.info("Asset fetched based on services:{}", services);
        return Response.ok(services).build();
    }

    @Operation(summary = "Get valid asset types", description = "Get valid asset types", operationId = "get-asset-types", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = String.class, type = "enum")))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.ASSET_TYPE)
    public Response getValidAssetTypes() {

        List<String> validTypes = assetService.getValidFileExtension();

        logger.info("validTypes:{}", validTypes);
        return Response.ok(validTypes).build();
    }

    @Operation(summary = "Get valid asset types", description = "Get valid asset types", operationId = "get-asset-dir-mapping", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = AssetDirMapping.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.ASSET_DIR_MAPPING)
    public Response getAssetDirMapping() {

        List<AssetDirMapping> assetDirMappingList = assetService.getAssetDirMapping();

        logger.info("validTypes:{}", assetDirMappingList);
        return Response.ok(assetDirMappingList).build();
    }

    @Operation(summary = "Upload new asset", description = "Upload new asset", operationId = "post-new-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = AssetForm.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Asset", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = Document.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/post-asset.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(ApiConstants.UPLOAD)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_WRITE_ACCESS })
    public Response uploadAsset(@MultipartForm AssetForm assetForm) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Create Asset details assetForm:{}", assetForm);
        }

        // validation
        checkResourceNotNull(assetForm, ASSET_DATA_FORM);
        Document asset = assetForm.getDocument();
        log.info(" Create asset:{} ", asset);
        checkResourceNotNull(asset, ASSET_DATA);
        checkNotNull(asset.getFileName(), "fileName");

        // check if asset with same name already exist
        List<Document> assets = assetService.getAssetByName(asset.getFileName());
        if (assets != null && !assets.isEmpty()) {
            asset.setInum(assets.get(0).getInum());
            asset.setBaseDn(assets.get(0).getBaseDn());
        }

        InputStream assetStream = assetForm.getAssetFile();

        if (assetStream == null || assetStream.available() <= 0) {
            log.error("No asset file provided");
            throwBadRequestException(RESOURCE_NULL, String.format(RESOURCE_NULL_MSG, "Asset File"));
        }

        // save asset
        try {
            asset = assetService.saveAsset(asset, assetStream, false);
            log.debug("Saved asset:{} ", asset);
        } catch (Exception ex) {
            log.error("Application Error while creating asset is - {}", ex.getMessage());
            throwInternalServerException(APPLICATION_ERROR, ex);
        }

        log.info("Create IdentityProvider - asset:{}", asset);
        return Response.status(Response.Status.CREATED).entity(asset).build();
    }

    @Operation(summary = "Update existing asset", description = "Update existing asset", operationId = "put-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = AssetForm.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/put-asset.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified Asset", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, schema = @Schema(implementation = Document.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/put-asset.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "Unauthorized"))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @PUT
    @Path(ApiConstants.UPLOAD)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_WRITE_ACCESS })
    public Response updateAsset(@MultipartForm AssetForm assetForm) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Update Asset details assetForm:{}", assetForm);
        }

        // validation
        checkResourceNotNull(assetForm, ASSET_DATA_FORM);
        Document asset = assetForm.getDocument();
        final String inum = asset.getInum();
        log.debug(" Create asset:{} ", asset);
        checkResourceNotNull(asset, ASSET_DATA);
        checkResourceNotNull(inum, ASSET_INUM);
        checkNotNull(asset.getFileName(), "fileName");

        // validate if asset exist
        Document existingDoc = assetService.getAssetByInum(asset.getInum());
        if (existingDoc == null) {
            throw new InvalidAttributeException("Asset with inum '" + asset.getInum() + "' does not exist!!!");
        }

        // check if asset with same name already exist
        List<Document> assets = assetService.getAssetByName(asset.getFileName());
        log.info(
                "Check if asset with inum different then:{} but with same name exist - asset.getDisplayName():{}, assets:{}",
                inum, asset.getFileName(), assets);
        if (assets != null && !assets.isEmpty()) {
            List<Document> list = assets.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                    .collect(Collectors.toList());
            logger.info("Other asset with same name:{} are list:{}", asset.getFileName(), list);
            if (list != null && !list.isEmpty()) {
                log.error("Another asset with same name:{}", asset.getFileName());
                throwBadRequestException(ASSET_NAME_CONFLICT,
                        String.format(ASSET_NAME_CONFLICT_MSG, asset.getFileName()));
            }
        }

        InputStream assetFile = assetForm.getAssetFile();
        log.debug(" Update asset assetFile:{} ", assetFile);

        // update asset
        try {
            asset = assetService.saveAsset(asset, assetFile, true);
            log.debug(" Updated asset:{} ", asset);
        } catch (Exception ex) {
            log.error("Application Error while updated asset is:{}", ex.getMessage());
            throwInternalServerException(APPLICATION_ERROR, ex);
        }

        log.info("Updated asset:{}", asset);
        return Response.status(Response.Status.OK).entity(asset).build();
    }

    @Operation(summary = "Load assets on server for a service", description = "Load assets on server for a service", operationId = "load-service-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_WRITE_ACCESS }))
    @RequestBody(description = "String multipart form.", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = String.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset file loaded", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class), examples = @ExampleObject(name = "Response json example", value = "example/assets/load-service-assets.json"))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path(ApiConstants.SERVICE + ApiConstants.SERVICE_NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_WRITE_ACCESS })
    public Response loadServiceAsset(
            @Parameter(description = "Service Name") @PathParam(ApiConstants.SERVICE_NAME) @NotNull String serviceName)
            throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Create Asset details serviceName:{}", serviceName);
        }

        // validation
        checkResourceNotNull(serviceName, "Service Name");
        String result = null;
        // save asset
        try {
            result = assetService.loadServiceAsset(serviceName);

        } catch (Exception ex) {
            log.error("Application Error while loading asset is - {}", ex.getMessage());
            throwInternalServerException(APPLICATION_ERROR, ex);
        }

        log.debug("Load asset for:{}, result is:{} ", serviceName, result);
        return Response.status(Response.Status.OK).entity(result).build();
    }

    @Operation(summary = "Delete an asset", description = "Delete an asset", operationId = "delete-asset", tags = {
            "Jans Assets" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.JANS_ASSET_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))) })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.JANS_ASSET_DELETE_ACCESS })
    public Response deleteAsset(
            @Parameter(description = "Asset identifier") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        if (log.isInfoEnabled()) {
            log.info("Delete an Asset identified inum:{}", inum);
        }
        try {
            boolean status = assetService.removeAsset(inum);
            log.debug(" Delete asset status:{} ", status);
        } catch (Exception ex) {
            log.error("Error while asset deletion is:{}", ex.getMessage());
            if (ex instanceof NotFoundException) {
                throwNotFoundException(NOT_FOUND_ERROR, ex.getMessage());
            }
            throwInternalServerException(APPLICATION_ERROR, ex);
        }
        return Response.noContent().build();

    }

    private DocumentPagedResult doSearch(SearchRequest searchReq, String status) throws Exception {

        logger.debug("Asset search params - searchReq:{} , status:{} ", searchReq, status);
        DocumentPagedResult documentPagedResult = null;
        PagedResult<Document> pagedResult = assetService.searchAsset(searchReq, status);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Asset fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            documentPagedResult = getDocumentPagedResult(pagedResult);
        }

        logger.debug("Asset documentPagedResult:{} ", documentPagedResult);
        return documentPagedResult;
    }

    private DocumentPagedResult searchByName(SearchRequest searchReq) throws Exception {

        logger.debug("Search asset by name params - searchReq:{} ", searchReq);
        DocumentPagedResult documentPagedResult = null;
        PagedResult<Document> pagedResult = assetService.searchAssetByName(searchReq);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "Asset fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            documentPagedResult = getDocumentPagedResult(pagedResult);
        }

        logger.debug("Asset documentPagedResult:{} ", documentPagedResult);
        return documentPagedResult;
    }

    private DocumentPagedResult getDocumentPagedResult(PagedResult<Document> pagedResult) {
        DocumentPagedResult documentPagedResult = null;
        if (pagedResult != null) {
            List<Document> identityProviderList = pagedResult.getEntries();
            documentPagedResult = new DocumentPagedResult();
            documentPagedResult.setStart(pagedResult.getStart());
            documentPagedResult.setEntriesCount(pagedResult.getEntriesCount());
            documentPagedResult.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            documentPagedResult.setEntries(identityProviderList);
        }
        return documentPagedResult;
    }

}
