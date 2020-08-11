/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.ArrayList;
import java.util.List;

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
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxauthconfigapi.util.AttributeNames;
import org.gluu.oxtrust.service.custom.CustomScriptService;
import org.gluu.util.INumGenerator;
import org.slf4j.Logger;

/**
 * @author Mougang T.Gasmyr
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.SCRIPTS + ApiConstants.PERSON_AUTH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonAuthResource extends BaseResource {

	@Inject
	Logger logger;

	@Inject
	CustomScriptService customScriptService;

	@GET
	@Operation(summary = "List of person authentication scripts")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomScript[].class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAttributes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		try {
			List<CustomScript> customScripts = new ArrayList<CustomScript>();
			if (!pattern.isEmpty() && pattern.length() >= 2) {
				customScripts = customScriptService.findCustomAuthScripts(pattern, limit);
			} else {
				customScripts = customScriptService.findCustomAuthScripts(limit);
			}
			return Response.ok(customScripts).build();
		} catch (Exception e) {
			logger.error("Failed to fetch attributes " + e);
			return getInternalServerError(e);
		}
	}

	@GET
	@Operation(summary = "Get Person Authentication Script by Inum")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomScript.class, required = false))),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getAuthScriptByInum(@PathParam(ApiConstants.INUM) String inum) {
		try {
			CustomScript attribute = customScriptService.getScriptByInum(inum);
			if (attribute == null) {
				return getResourceNotFoundError();
			}
			return Response.ok(attribute).build();
		} catch (Exception ex) {
			logger.error("Failed to fetch  Person Authentication Script by inum " + inum, ex);
			return getInternalServerError(ex);
		}
	}

	@POST
	@Operation(summary = "Create Person Authentication Script")
	@APIResponses(value = {
			@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CustomScript.class, required = true))),
			@APIResponse(responseCode = "500", description = "Internal Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createPersonScript(@Valid CustomScript customScript) {
		try {
			if (customScript.getName() == null) {
				return getMissingAttributeError(AttributeNames.NAME);
			}
			if (customScript.getDescription() == null) {
				return getMissingAttributeError(AttributeNames.DESCRIPTION);
			}
			String inum = INumGenerator.generate(2);
			customScript.setInum(inum);
			customScript.setDn(customScriptService.buildDn(inum));
			customScriptService.add(customScript);
			CustomScript result = customScriptService.getScriptByInum(inum);
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			logger.error("Failed to create person script", e);
			return getInternalServerError(e);
		}

	}

	@PUT
	@Operation(summary = "Update existing person script")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CustomScript.class)), description = "Success"),
			@APIResponse(responseCode = "400", description = "Bad Request"),
			@APIResponse(responseCode = "404", description = "Not Found"),
			@APIResponse(responseCode = "500", description = "Server Error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updatePersonScript(@Valid CustomScript customScript) {
		try {
			String inum = customScript.getInum();
			if (inum == null) {
				return getResourceNotFoundError();
			}
			if (customScript.getName() == null) {
				return getMissingAttributeError(AttributeNames.NAME);
			}
			if (customScript.getDescription() == null) {
				return getMissingAttributeError(AttributeNames.DESCRIPTION);
			}
			CustomScript existingScript = customScriptService.getScriptByInum(inum);
			if (existingScript == null) {
				return getResourceNotFoundError();
			}
			customScript.setInum(existingScript.getInum());
			customScript.setDn(existingScript.getDn());
			customScript.setBaseDn(existingScript.getBaseDn());
			customScriptService.update(customScript);
			CustomScript result = customScriptService.getScriptByInum(inum);
			return Response.ok(result).build();
		} catch (Exception e) {
			return getInternalServerError(e);
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@Operation(summary = "Delete Person Authentication Script", description = "Delete Person Authentication Script")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "Success"),
			@APIResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class, required = false))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deletePersonScript(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		try {
			CustomScript customScript = customScriptService.getScriptByInum(inum);
			if (customScript != null) {
				customScriptService.remove(customScript);
				return Response.noContent().build();
			} else {
				return getResourceNotFoundError();
			}
		} catch (Exception ex) {
			logger.error("Failed to delete person script", ex);
			return getInternalServerError(ex);
		}
	}
}
