/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.gluu.model.GluuAttribute;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.service.AttributeService;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.ATTRIBUTES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AttributeResource extends BaseResource {

	@Inject
	AttributeService attributeService;
	@Inject
	Logger logger;

	@GET
	@Operation(summary = "List of attributes")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = GluuAttribute.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAttributes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
			@DefaultValue(ApiConstants.ALL) @QueryParam(value = ApiConstants.STATUS) String status) {
		try {
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
		} catch (Exception e) {
			logger.error("Failed to fetch attributes " + e);
			return getInternalServerError(e);
		}
	}

	@GET
	@Operation(summary = "Get attribute by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = GluuAttribute.class, required = false))),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getAttributeByInum(@PathParam(ApiConstants.INUM) String inum) {
		try {
			GluuAttribute attribute = attributeService.getAttributeByInum(inum);
			if (attribute == null) {
				return getResourceNotFoundError();
			}
			return Response.ok(attribute).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch  attribute by inum " + inum, ex);
			return getInternalServerError(ex);
		}
	}

	@POST
	@Operation(summary = "Create attribute")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = GluuAttribute.class, required = true))),
			@APIResponse(responseCode = "500", description = "Internal Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createAttribute(@Valid GluuAttribute attribute) {
		try {
			if (attribute.getName() == null) {
				return getMissingAttributeError(AttributeNames.NAME);
			}
			if (attribute.getDisplayName() == null) {
				return getMissingAttributeError(AttributeNames.DISPLAY_NAME);
			}
			String inum = attributeService.generateInumForNewAttribute();
			attribute.setInum(inum);
			attribute.setDn(attributeService.getDnForAttribute(inum));
			attributeService.addAttribute(attribute);
			GluuAttribute result = attributeService.getAttributeByInum(inum);
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			logger.error("Failed to create attribute", e);
			return getInternalServerError(e);
		}

	}

	@PUT
	@Operation(summary = "Update existing attribute")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = GluuAttribute.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateAttribute(@Valid GluuAttribute attribute) {
		try {
			String inum = attribute.getInum();
			if (inum == null) {
				return getResourceNotFoundError();
			}
			GluuAttribute existingAttribute = attributeService.getAttributeByInum(inum);
			if (existingAttribute == null) {
				return getResourceNotFoundError();
			}
			if (attribute.getName() == null) {
				return getMissingAttributeError(AttributeNames.NAME);
			}
			if (attribute.getDisplayName() == null) {
				return getMissingAttributeError(AttributeNames.DISPLAY_NAME);
			}
			attribute.setInum(existingAttribute.getInum());
			attribute.setBaseDn(attributeService.getDnForAttribute(inum));
			attributeService.updateAttribute(attribute);
			GluuAttribute result = attributeService.getAttributeByInum(inum);
			return Response.ok(result).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete attribute ", description = "Delete attribute")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deleteAttribute(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		try {
			GluuAttribute attribute = attributeService.getAttributeByInum(inum);
			if (attribute != null) {
				attributeService.removeAttribute(attribute);
				return Response.noContent().build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to delete attribute", ex);
			return getInternalServerError(ex);
		}
	}

}
