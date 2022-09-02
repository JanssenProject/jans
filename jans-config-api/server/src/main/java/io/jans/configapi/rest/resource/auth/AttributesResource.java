/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.model.SearchRequest;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.core.util.Jackson;
import io.jans.model.GluuAttribute;
import io.jans.orm.model.PagedResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.ATTRIBUTES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttributesResource extends ConfigBaseResource {

    private static final String GLUU_ATTRIBUTE = "gluu attribute";

    @Inject
    Logger log;

    @Inject
    AttributeService attributeService;

    @Operation(summary = "Gets a list of Gluu attributes.", description = "Gets a list of Gluu attributes.", operationId = "get-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.readonly" }), parameters = {
                            @Parameter(in = ParameterIn.QUERY, name = "limit", required = false, schema = @Schema(name = "limit", type = "integer", defaultValue = "50", description = "Search size - max size of the results to return")),
                            @Parameter(in = ParameterIn.QUERY, name = "pattern", required = false, schema = @Schema(name = "pattern", type = "string", description = "Search pattern")),
                            @Parameter(in = ParameterIn.QUERY, name = "status", required = false, schema = @Schema(name = "status", type = "string", defaultValue = "all", description = "Status of the attribute"))

    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GluuAttribute.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS })
    public Response getAttributes(@DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) {

        SearchRequest searchReq = createSearchRequest(attributeService.getDnForAttribute(null), pattern, sortBy,
                sortOrder, startIndex, limit, null, null, this.getMaxCount());

        return Response.ok(doSearch(searchReq, status)).build();
    }

    @Operation(summary = "Gets an attribute based on inum", description = "Gets an attribute based on inum", operationId = "get-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.readonly" }), parameters = {
                            @Parameter(in = ParameterIn.PATH, name = "inum", required = true, schema = @Schema(name = "inum", type = "string", description = "Attribute ID"))
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAttributeByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        return Response.ok(attribute).build();
    }

    @Operation(summary = "Adds a new attribute", description = "Adds a new attribute", operationId = "post-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.write" }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    public Response createAttribute(@Valid GluuAttribute attribute) {
        log.debug(" GluuAttribute details to add - attribute:{}", attribute);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkResourceNotNull(attribute.getDataType(), AttributeNames.DATA_TYPE);
        String inum = attributeService.generateInumForNewAttribute();
        attribute.setInum(inum);
        attribute.setDn(attributeService.getDnForAttribute(inum));
        attributeService.addAttribute(attribute);
        GluuAttribute result = attributeService.getAttributeByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @Operation(summary = "Updates an existing attribute", description = "Updates an existing attribute", operationId = "put-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.write" }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    public Response updateAttribute(@Valid GluuAttribute attribute) {
        log.debug(" GluuAttribute details to update - attribute:{}", attribute);
        String inum = attribute.getInum();
        checkResourceNotNull(inum, GLUU_ATTRIBUTE);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkResourceNotNull(attribute.getDataType(), AttributeNames.DATA_TYPE);
        GluuAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, GLUU_ATTRIBUTE);
        attribute.setInum(existingAttribute.getInum());
        attribute.setBaseDn(attributeService.getDnForAttribute(inum));
        attributeService.updateAttribute(attribute);
        GluuAttribute result = attributeService.getAttributeByInum(inum);
        return Response.ok(result).build();
    }

    @Operation(summary = "Partially modify a GluuAttribute", description = "Partially modify a GluuAttribute", operationId = "patch-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.write" }), parameters = {
                            @Parameter(in = ParameterIn.PATH, name = "inum", required = true, description = "Attribute ID", schema = @Schema(type = "string")) })
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchAtribute(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        log.debug(" GluuAttribute details to patch - inum:{}, pathString:{}", inum, pathString);
        GluuAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, GLUU_ATTRIBUTE);

        existingAttribute = Jackson.applyPatch(pathString, existingAttribute);
        attributeService.updateAttribute(existingAttribute);
        return Response.ok(existingAttribute).build();
    }

    @Operation(summary = "Deletes an attribute based on inum", description = "Deletes an attribute based on inum", operationId = "delete-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    "https://jans.io/oauth/config/attributes.delete" }), parameters = {
                            @Parameter(in = ParameterIn.PATH, name = "inum", required = true, schema = @Schema(name = "inum", type = "string", description = "Attribute ID"))

    })
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_DELETE_ACCESS })
    public Response deleteAttribute(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug(" GluuAttribute details to delete - inum:{}", inum);
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        attributeService.removeAttribute(attribute);
        return Response.noContent().build();
    }

    private Map<String, Object> doSearch(SearchRequest searchReq, String status) {

        logger.debug("GluuAttribute search params - searchReq:{} , status:{} ", searchReq, status);

        PagedResult<GluuAttribute> pagedResult = attributeService.searchGluuAttributes(searchReq, status);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        JSONObject dataJsonObject = new JSONObject();
        if (pagedResult != null) {
            logger.debug("GluuAttributes fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
            dataJsonObject.put(ApiConstants.TOTAL_ITEMS, pagedResult.getTotalEntriesCount());
            dataJsonObject.put(ApiConstants.ENTRIES_COUNT, pagedResult.getEntriesCount());
            dataJsonObject.put(ApiConstants.DATA, pagedResult.getEntries());
        }
        else {
            dataJsonObject.put(ApiConstants.TOTAL_ITEMS, 0);
            dataJsonObject.put(ApiConstants.ENTRIES_COUNT, 0);
            dataJsonObject.put(ApiConstants.DATA, Collections.emptyList());
        }
       
        logger.debug("GluuAttributes fetched new  - dataJsonObject:{}, data:{} ", dataJsonObject, dataJsonObject.toMap());
        return dataJsonObject.toMap();
     }

}
