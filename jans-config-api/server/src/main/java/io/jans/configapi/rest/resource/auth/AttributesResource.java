/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.configapi.core.model.PatchRequest;
import io.jans.model.SearchRequest;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.core.util.Jackson;
import io.jans.model.JansAttribute;
import io.jans.orm.model.PagedResult;

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

import java.util.*;
import java.util.stream.*;
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

    private static final String JANS_ATTRIBUTE = "jans attribute";
	    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "Attribute with same name `%s` already exists!";

    @Inject
    Logger log;

    @Inject
    AttributeService attributeService;

    @Operation(summary = "Gets a list of Jans attributes.", description = "Gets a list of Jans attributes.", operationId = "get-attributes", tags = {
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
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "Status of the attribute") @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Field and value pair for seraching", examples = @ExampleObject(name = "Field value example", value = "adminCanEdit=true,dataType=string")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Search Attribute filters with limit:{}, pattern:{}, status:{}, startIndex:{}, sortBy:{}, sortOrder:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(status), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(fieldValuePair));
        }
              
        SearchRequest searchReq = createSearchRequest(attributeService.getDnForAttribute(null), pattern, sortBy,
                sortOrder, startIndex, limit, null, null, this.getMaxCount(),fieldValuePair, JansAttribute.class);

        return Response.ok(doSearch(searchReq, status)).build();
    }

    @Operation(summary = "Gets an attribute based on inum", description = "Gets an attribute based on inum", operationId = "get-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute-get.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAttributeByInum(@Parameter(description = "Attribute Id") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        JansAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, JANS_ATTRIBUTE);
        return Response.ok(attribute).build();
    }

    @Operation(summary = "Adds a new attribute", description = "Adds a new attribute", operationId = "post-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "JansAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Request example", value = "example/attribute/attribute.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "400", description = "BadRequest"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "NotAcceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response createAttribute(@Valid JansAttribute attribute) {
        log.debug(" JansAttribute details to add - attribute:{}", attribute);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkResourceNotNull(attribute.getDataType(), AttributeNames.DATA_TYPE);
        
        // check if attribute with same name
        List<JansAttribute> jansAttributes = attributeService.getAttributeWithName(attribute.getName());
        log.info("Check if attribute with same name exists - attribute.getName():{}, jansAttributes:{}",
                attribute.getName(), jansAttributes);
        if (jansAttributes != null && !jansAttributes.isEmpty()) {
            throw new WebApplicationException(getNotAcceptableException(
                    "Attribute with same name '" + attribute.getName() + "' already exists!"));
        }

        // check if attribute exists in schema
        boolean attributeValidation = attributeService.validateAttributeDefinition(attribute.getName());
        log.info("** Validate attribute while creation - attribute.getName():{}, attributeValidation:{}",
                attribute.getName(), attributeValidation);
        if (!attributeValidation) {
            throw new WebApplicationException(
                    getNotAcceptableException("The attribute '" + attribute.getName() + "' not defined in DB schema"));
        }
        
        
        String inum = attributeService.generateInumForNewAttribute();
        attribute.setInum(inum);
        attribute.setDn(attributeService.getDnForAttribute(inum));
        attributeService.addAttribute(attribute);
        JansAttribute result = attributeService.getAttributeByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @Operation(summary = "Updates an existing attribute", description = "Updates an existing attribute", operationId = "put-attributes", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "JansAttribute object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Request example", value = "example/attribute/attribute.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "406", description = "NotAcceptable"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateAttribute(@Valid JansAttribute attribute) {
        log.debug(" JansAttribute details to update - attribute:{}", attribute);
        final String inum = attribute.getInum();
        checkResourceNotNull(inum, JANS_ATTRIBUTE);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        checkResourceNotNull(attribute.getDataType(), AttributeNames.DATA_TYPE);
        
        // check if attribute with same name
        List<JansAttribute> jansAttributes = attributeService.getAttributeWithName(attribute.getName());
        log.info(
                "Check if attribute with inum different then:{} but with same name exists - attribute.getName():{}, jansAttributes:{}",
                inum, attribute.getName(), jansAttributes);
        if (jansAttributes != null && !jansAttributes.isEmpty()) {
            List<JansAttribute> list = jansAttributes.stream().filter(e -> !e.getInum().equalsIgnoreCase(inum))
                    .collect(Collectors.toList());
            logger.info("Other JansAttribute's with same name:{} are list:{}", attribute.getName(), list);
            if (list != null && !list.isEmpty()) {
                throwBadRequestException(NAME_CONFLICT, String.format(NAME_CONFLICT_MSG, attribute.getName()));
            }
        }
        else {
            // check if attribute exists in schema
            boolean attributeValidation = attributeService.validateAttributeDefinition(attribute.getName());
            log.info(" ** Validate attribute - attribute.getName():{}, attributeValidation:{}", attribute.getName(),
                    attributeValidation);
            if (!attributeValidation) {
                throw new WebApplicationException(getNotAcceptableException(
                        "The attribute type '" + attribute.getName() + "' not defined in DB schema"));
            }
        }
        
        JansAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, JANS_ATTRIBUTE);
        attribute.setInum(existingAttribute.getInum());
        attribute.setBaseDn(attributeService.getDnForAttribute(inum));
        attributeService.updateAttribute(attribute);
        JansAttribute result = attributeService.getAttributeByInum(inum);
        return Response.ok(result).build();
    }

    @Operation(summary = "Partially modify a JansAttribute", description = "Partially modify a JansAttribute", operationId = "patch-attributes-by-inum", tags = {
            "Attribute" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = PatchRequest.class)), examples = @ExampleObject(name = "Patch request example", value = "example/attribute/attribute-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated JansAttribute", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JansAttribute.class), examples = @ExampleObject(name = "Response example", value = "example/attribute/attribute.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchAtribute(@Parameter(description = "Attribute Id") @PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        log.debug(" JansAttribute details to patch - inum:{}, pathString:{}", inum, pathString);
        JansAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, JANS_ATTRIBUTE);

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
    public Response deleteAttribute(@Parameter(description = "Attribute Id") @PathParam(ApiConstants.INUM) @NotNull String inum) {
        log.debug(" JansAttribute details to delete - inum:{}", inum);
        JansAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, JANS_ATTRIBUTE);
        attributeService.removeAttribute(attribute);
        return Response.noContent().build();
    }

    private PagedResult<JansAttribute> doSearch(SearchRequest searchReq, String status) {

        logger.debug("JansAttribute search params - searchReq:{} , status:{} ", searchReq, status);

        PagedResult<JansAttribute> pagedResult = attributeService.searchJansAttributes(searchReq, status);

        logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        if (pagedResult != null) {
            logger.debug(
                    "JansAttributes fetched  - pagedResult.getTotalEntriesCount():{}, pagedResult.getEntriesCount():{}, pagedResult.getEntries():{}",
                    pagedResult.getTotalEntriesCount(), pagedResult.getEntriesCount(), pagedResult.getEntries());
        }

        logger.debug("JansAttributes pagedResult:{} ", pagedResult);
        return pagedResult;
    }

}
