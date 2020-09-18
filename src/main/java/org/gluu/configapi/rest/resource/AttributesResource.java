/**
 *
 */
package org.gluu.configapi.rest.resource;

import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.AttributeNames;
import org.gluu.configapi.util.Jackson;
import org.gluu.model.GluuAttribute;
import org.gluu.oxauth.service.AttributeService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.ATTRIBUTES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttributesResource extends BaseResource {

    /**
     *
     */
    private static final String GLUU_ATTRIBUTE = "gluu attribute";
    @Inject
    AttributeService attributeService;
    @Inject
    Logger logger;

    @GET
    @ProtectedApi(scopes = {READ_ACCESS})
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
    @ProtectedApi(scopes = {READ_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response getAttributeByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        return Response.ok(attribute).build();
    }

    @POST
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response createAttribute(@Valid GluuAttribute attribute) {
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
        String inum = attributeService.generateInumForNewAttribute();
        attribute.setInum(inum);
        attribute.setDn(attributeService.getDnForAttribute(inum));
        attributeService.addAttribute(attribute);
        GluuAttribute result = attributeService.getAttributeByInum(inum);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @PUT
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response updateAttribute(@Valid GluuAttribute attribute) {
        String inum = attribute.getInum();
        checkResourceNotNull(inum, GLUU_ATTRIBUTE);
        checkNotNull(attribute.getName(), AttributeNames.NAME);
        checkNotNull(attribute.getDisplayName(), AttributeNames.DISPLAY_NAME);
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
    @ProtectedApi(scopes = {WRITE_ACCESS})
    @Path(ApiConstants.INUM_PATH)
    public Response patchAtribute(@PathParam(ApiConstants.INUM) @NotNull String inum, @NotNull String pathString) {
        GluuAttribute existingAttribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(existingAttribute, GLUU_ATTRIBUTE);
        try {
            existingAttribute = Jackson.applyPatch(pathString, existingAttribute);
            attributeService.updateAttribute(existingAttribute);
            return Response.ok(existingAttribute).build();
        } catch (Exception e) {
            logger.error("", e);
            throw new WebApplicationException(e.getMessage());
        }
    }

    @DELETE
    @Path(ApiConstants.INUM_PATH)
    @ProtectedApi(scopes = {WRITE_ACCESS})
    public Response deleteAttribute(@PathParam(ApiConstants.INUM) @NotNull String inum) {
        GluuAttribute attribute = attributeService.getAttributeByInum(inum);
        checkResourceNotNull(attribute, GLUU_ATTRIBUTE);
        attributeService.removeAttribute(attribute);
        return Response.noContent().build();
    }

}
