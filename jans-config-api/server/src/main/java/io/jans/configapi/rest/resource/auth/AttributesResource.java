/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

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

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response getAttributeByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        return Response.ok(attribute).build();
    }

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

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchAtribute(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString)
            throws JsonPatchException, IOException {
        log.debug(" GluuAttribute details to patch - inum:{} , pathString:{}", inum, pathString);
        GluuAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, GLUU_ATTRIBUTE);

        existingAttribute = Jackson.applyPatch(pathString, existingAttribute);
        attributeService.updateAttribute(existingAttribute);
        return Response.ok(existingAttribute).build();
    }

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
