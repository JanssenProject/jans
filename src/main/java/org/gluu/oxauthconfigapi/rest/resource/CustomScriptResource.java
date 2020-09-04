package org.gluu.oxauthconfigapi.rest.resource;

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
import org.gluu.service.ScriptService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.SCRIPTS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomScriptResource extends BaseResource {
	
	@Inject
	Logger logger;
	
	@Inject
	ScriptService customScriptService;
	
	private static final String CUSTOM_SCRIPT = "custom script";
	
	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAllCustomScripts()
	{
		System.out.println(" CustomScriptResource::getAllCustomScripts()");
		List<CustomScript> customScripts = customScriptService.findAllCustomScripts(null);
		System.out.println(" CustomScriptResource::getAllCustomScripts() - customScripts = "+customScripts);
		return Response.ok(customScripts).build();
	}
	
	@GET
	@Path(ApiConstants.TYPE_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getScriptByType(@PathParam(ApiConstants.TYPE) @NotNull String type)
	{
		System.out.println(" CustomScriptResource::getScriptByType() - type = "+type);
		System.out.println(" CustomScriptResource::getScriptByType() - CustomScriptType.getByValue(type) = "+CustomScriptType.getByValue(type));
		List<CustomScript> customScripts = this.customScriptService.findScriptByType(CustomScriptType.getByValue(type));
		System.out.println(" CustomScriptResource::getScriptByType() - customScripts = "+customScripts);	
		if (customScripts!=null && !customScripts.isEmpty()) 
			return Response.ok(customScripts).build();
		else
			return Response.status(Response.Status.NOT_FOUND).build();
		
	}	
	
	@GET
	@Path(ApiConstants.TYPE_PATH)
	@ProtectedApi(scopes = { READ_ACCESS })	
	public Response getCustomScriptsBygetCustomScriptsByTypePattern(@PathParam(ApiConstants.TYPE) @NotNull String type,
			@DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
			@DefaultValue("50") @QueryParam(value = ApiConstants.LIMIT) int limit) {
		System.out.println(" CustomScriptResource::getCustomScriptsBygetCustomScriptsByTypePattern() - type = "+type+" , pattern = "+pattern+" , limit = "+limit);
		List<CustomScript> customScripts = this.customScriptService.findScriptByPatternAndType(pattern,CustomScriptType.getByValue(type),limit);
		System.out.println(" CustomScriptResource::getCustomScriptsBygetCustomScriptsByTypePattern() - customScripts = "+customScripts);
		if (customScripts!=null && !customScripts.isEmpty()) 
			return Response.ok(customScripts).build();
		else
			return Response.status(Response.Status.NOT_FOUND).build();
		
	}

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Path(ApiConstants.INUM_PATH)
	public Response getCustomScriptByInum(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		CustomScript script = customScriptService.getScriptByInum(inum);
		checkResourceNotNull(script, CUSTOM_SCRIPT);
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
		return Response.ok(customScriptService.getScriptByInum(inum)).build();

	}	

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updatePersonScript(@Valid CustomScript customScript) {
		Objects.requireNonNull(customScript, "Attempt to update null custom script");
		String inum = customScript.getInum();
		logger.info("Update custom script " + inum);
		CustomScript existingScript = customScriptService.getScriptByInum(customScript.getInum());
		if (existingScript != null) {
			customScript.setInum(existingScript.getInum());
			customScriptService.update(customScript);
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@DELETE
	@Path(ApiConstants.INUM_PATH)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response deletePersonScript(@PathParam(ApiConstants.INUM) @NotNull String inum) {
		Objects.requireNonNull(inum);
		CustomScript existingScript = customScriptService.getScriptByInum(inum);
		if (existingScript != null) {
			customScriptService.remove(existingScript);
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

}
