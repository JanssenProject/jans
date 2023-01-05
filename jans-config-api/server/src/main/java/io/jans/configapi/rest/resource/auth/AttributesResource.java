/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.model.PatchRequest;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;

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
                    ApiAccessConstants.ATTRIBUTES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute-get-all.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getAttributes(
            @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Search Attribute filters with limit:{}, pattern:{}, status:{}, startIndex:{}, sortBy:{}, sortOrder:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(status), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder));
        }

        SearchRequest searchReq = createSearchRequest(attributeService.getDnForAttribute(null), pattern, sortBy,
                sortOrder, startIndex, limit, null, null, this.getMaxCount());

        return Response.ok(doSearch(searchReq, status)).build();
    }

    @Operation(summary = "Gets an attribute based on inum", description = "Gets an attribute based on inum", operationId = "get-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAttributeByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        return Response.ok(attribute).build();
    }

    @Operation(summary = "Adds a new attribute", description = "Adds a new attribute", operationId = "post-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Request example", value = "example/attribute/attribute.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "GluuAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Request example", value = "example/attribute/attribute.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = PatchRequest.class)), examples = @ExampleObject(name = "Patch request example", value = "example/attribute/attribute-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated GluuAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GluuAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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
                    ApiAccessConstants.ATTRIBUTES_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_DELETE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_DELETE_ACCESS })
    public Response deleteAttribute(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug(" GluuAttribute details to delete - inum:{}", inum);
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        attributeService.removeAttribute(attribute);
        return Response.noContent().build();
    }

    private PagedResult<GluuAttribute> doSearch(SearchRequest searchReq, String status) {

        logger.debug("GluuAttribute search params - searchReq:{} , status:{} ", searchReq, status);

        PagedResult<GluuAttribute> pagedResult = attributeService.searchGluuAttributes(searchReq, status);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "GluuAttributes fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }

        logger.debug("GluuAttributes pagedResult:{} ", pagedResult);
        return pagedResult;
    }

}
