package org.gluu.configapi.rest.resource;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
import org.gluu.service.custom.CustomScriptService;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.SCRIPTS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomScriptResource extends BaseResource {

	/**
	 * 
	 */
	private static final String CUSTOM_SCRIPT = "custom script";

	/**
	 * 
	 */
	private static final String PATH_SEPARATOR = "/";

	@Inject
	Logger logger;

	@Inject
	CustomScriptService customScriptService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAllCustomScripts() {
		List<CustomScript> customScripts = customScriptService.findAllCustomScripts(null);
		return Response.ok(customScripts).build();
	}

	@GET
	@Path(PATH_SEPARATOR + ApiConstants.TYPE + ApiConstants.TYPE_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getCustomScriptsByTypePattern(@PathParam(ApiConstants.TYPE) @NotNull String type,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
			@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit) {
		List<CustomScript> customScripts = this.customScriptService.findScriptByPatternAndType(pattern,
				CustomScriptType.getByValue(type), limit);
		if (customScripts != null && !customScripts.isEmpty())
			return Response.ok(customScripts).build();
		else
			return Response.status(Response.Status.NOT_FOUND).build();
	}

	@GET
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getCustomScriptByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		CustomScript script = null;
		try {
			script = this.customScriptService.getScriptByInum(inum);
		} catch (Exception ex) {
			if (ex.getMessage().contains("Failed to find entry")) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		}
		return Response.ok(script).build();
	}

	@POST
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response createPersonScript(@Valid CustomScript customScript) {
		Objects.requireNonNull(customScript, "Attempt to create null custom script");
		String inum = customScript.getInum();
		if (StringHelper.isEmpty(inum)) {
			inum = UUID.randomUUID().toString();
		}
		customScript.setDn(customScriptService.buildDn(inum));
		customScript.setInum(inum);
		customScriptService.add(customScript);
		return Response.status(Response.Status.CREATED).entity(customScript).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updatePersonScript(@Valid @NotNull CustomScript customScript) {
		CustomScript existingScript = customScriptService.getScriptByInum(customScript.getInum());
		if (existingScript != null) {
			customScript.setInum(existingScript.getInum());
			customScriptService.update(customScript);
			return Response.ok(customScript).build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deletePersonScript(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		try {
			CustomScript existingScript = customScriptService.getScriptByInum(inum);
			customScriptService.remove(existingScript);
			return Response.noContent().build();
		} catch (Exception ex) {
			logger.info("Error deleting script by inum " + inum, ex);
			throw new NotFoundException(getNotFoundError(CUSTOM_SCRIPT));
		}

	}

}
