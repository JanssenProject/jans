/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AttributeNames;
import io.jans.configapi.util.Jackson;
import io.jans.model.GluuAttribute;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.ATTRIBUTES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttributesResource extends BaseResource {

    private static final String GLUU_ATTRIBUTE = "gluu attribute";
    
    @Inject
    Logger log;

    @Inject
    AttributeService attributeService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_READ_ACCESS })
    public Response getAttributes(@DefaultValue(DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status) throws Exception {
        List<GluuAttribute> attributes = new ArrayList<GluuAttribute>();

        if (status.equalsIgnoreCase(ApiConstants.ALL)) {
            if (!pattern.isEmpty() && pattern.length() >= 2) {
                attributes = attributeService.searchAttributes(pattern, limit);
            } else {
                attributes = attributeService.searchAttributes(limit);
            }
        } else if (status.equalsIgnoreCase(ApiConstants.ACTIVE)) {
            if (!pattern.isEmpty() && pattern.length() >= 2) {
                attributes = attributeService.findAttributes(pattern, limit, true);
            } else {
                attributes = attributeService.searchAttributes(limit, true);
            }

        } else if (status.equalsIgnoreCase(ApiConstants.INACTIVE)) {
            if (!pattern.isEmpty() && pattern.length() >= 2) {
                attributes = attributeService.findAttributes(pattern, limit, false);
            } else {
                attributes = attributeService.searchAttributes(limit, false);
            }
        }

        return Response.ok(attributes).build();
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
        log.debug(" GluuAttribute details to add - attribute = "+attribute );
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
        log.debug(" GluuAttribute details to update - attribute = "+attribute );
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
        log.debug(" GluuAttribute details to patch - inum = "+inum+" , pathString = "+pathString);
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
        log.debug(" GluuAttribute details to delete - inum = "+inum);
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        attributeService.removeAttribute(attribute);
        return Response.noContent().build();
    }

}
