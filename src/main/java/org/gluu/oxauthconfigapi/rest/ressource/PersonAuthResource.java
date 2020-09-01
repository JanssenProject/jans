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

import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.model.CustomScript;
import org.gluu.oxauthconfigapi.exception.ApiException;
import org.gluu.oxauthconfigapi.exception.ApiExceptionType;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
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
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAttributes(@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern) {
		List<CustomScript> customScripts = new ArrayList<CustomScript>();
		if (!pattern.isEmpty() && pattern.length() >= 2) {
			customScripts = customScriptService.findCustomAuthScripts(pattern, limit);
		} else {
			customScripts = customScriptService.findCustomAuthScripts(limit);
		}
		return Response.ok(customScripts).build();
	}

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getAuthScriptByInum(@PathParam(ApiConstants.INUM) String inum) throws ApiException {
		CustomScript attribute = customScriptService.getScriptByInum(inum);
		if (attribute == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		return Response.ok(attribute).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createPersonScript(@Valid CustomScript customScript) throws ApiException {
		if (customScript.getName() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.NAME);
		}
		if (customScript.getDescription() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.DESCRIPTION);
		}
		String inum = INumGenerator.generate(2);
		customScript.setInum(inum);
		customScript.setDn(customScriptService.buildDn(inum));
		customScript.setScriptType(CustomScriptType.PERSON_AUTHENTICATION);
		customScriptService.add(customScript);
		CustomScript result = customScriptService.getScriptByInum(inum);
		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updatePersonScript(@Valid CustomScript customScript) throws ApiException {
		String inum = customScript.getInum();
		if (inum == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		if (customScript.getName() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.NAME);
		}
		if (customScript.getDescription() == null) {
			throw new ApiException(ApiExceptionType.MISSING_ATTRIBUTE, AttributeNames.DESCRIPTION);
		}
		CustomScript existingScript = customScriptService.getScriptByInum(inum);
		if (existingScript == null) {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
		customScript.setInum(existingScript.getInum());
		customScript.setDn(existingScript.getDn());
		customScript.setBaseDn(existingScript.getBaseDn());
		customScript.setScriptType(CustomScriptType.PERSON_AUTHENTICATION);
		customScriptService.update(customScript);
		CustomScript result = customScriptService.getScriptByInum(inum);
		return Response.ok(result).build();
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deletePersonScript(@PathParam(ApiConstants.INUM) @NotNull String inum) throws ApiException {
		CustomScript customScript = customScriptService.getScriptByInum(inum);
		if (customScript != null) {
			customScriptService.remove(customScript);
			return Response.noContent().build();
		} else {
			throw new ApiException(ApiExceptionType.NOT_FOUND, inum);
		}
	}
}
