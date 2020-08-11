package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.couchbase.client.core.message.ResponseStatus;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.Fido2Configuration;
import org.gluu.oxauthconfigapi.util.ApiConstants;


@Path(ApiConstants.BASE_API_URL + ApiConstants.FIDO2)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2Resource extends BaseResource {

	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Gets Fido2 configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Fido2Configuration.class, required = false)), description="Success"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getFido2Configuration() {
		log.debug("Fido2Resource::getFido2Configuration() - Retrieve oxAuth Fido2 configuration.");
		Fido2Configuration fido2Configuration = new Fido2Configuration();
		JsonElement entry= null;
		String fido2ConfigJson = null;
		try {
			DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
			
			if (dbApplicationConfiguration != null) {
				fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
			
				Gson gson = new GsonBuilder().create();
				JsonElement json = gson.fromJson(fido2ConfigJson, JsonElement.class);
				JsonObject job = gson.fromJson(fido2ConfigJson, JsonObject.class);
				//entry = job.getAsJsonObject("fido2Configuration");
				fido2Configuration = gson.fromJson(entry,Fido2Configuration.class);		

			}
			return Response.ok(fido2ConfigJson).build();
			
		} catch (Exception ex) {
			log.error("Failed to fetch oxAuth Fido2 configuration", ex);
			return getInternalServerError(ex);
		}
	}
	
	@PUT
	@Operation(summary = "Updates Fido2 configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateFido2Configuration(@Valid Fido2Configuration fido2Configuration) {
		log.debug("Fido2Resource::updateFido2Configuration()  - Updates Fido2 configuration properties.");		
		try {
			DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();			
			Gson gson = new Gson();
			String fido2ConfigJson = gson.toJson(fido2Configuration);
			this.jsonConfigurationService.saveFido2Configuration(fido2ConfigJson);			
			return Response.ok(ResponseStatus.SUCCESS).build();
		} catch (Exception ex) {
			log.error("Failed to update oxAuth Fido2 configuration", ex);
			return getInternalServerError(ex);
		}
	}

}
