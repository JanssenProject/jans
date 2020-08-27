package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.StringReader;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import javax.json.Json;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPatch;
import javax.json.JsonReader;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;


@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthConfiguration extends BaseResource {


	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	
	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAppConfiguration() {		
		try {
		
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		JsonObject jsonObject =  createJsonObject(appConfiguration);
		return Response.ok(jsonObject).build();
		
		}catch(Exception ex) {
			log.error("Failed to retrieve Auth application configuration.", ex);
			return getInternalServerError(ex);		
		}			
	}
	
		
	@PATCH
	@Path(ApiConstants.JSON_KEY_PATH)
	public Response patchAppConfigurationProperty(@NotNull @PathParam(ApiConstants.JSON_KEY) String jsonKey, @NotNull JsonObject jsonObject) {
		log.info("=======================================================================");
		log.info("\n\n jsonKey = "+jsonKey+" , jsonObject = "+jsonObject+"\n\n");
		try {
		
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		JsonObject appConfigJsonObject =  createJsonObject(appConfiguration);
		JsonValue jsonValue = getJsonValue(jsonKey,jsonObject);
		
		log.info("\n\n appConfigJsonObject_before = "+appConfigJsonObject+"\n\n");
		log.info("\n\n jsonValue = "+jsonValue+"\n\n");
		
		//Apply patch 
		JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder();
		JsonPatch jsonPatch = jsonPatchBuilder
				.replace("/"+jsonKey,jsonValue)
				.build();
		appConfigJsonObject = jsonPatch.apply(appConfigJsonObject);
		
		log.info("\n\n appConfigJsonObject_after = "+appConfigJsonObject+"\n\n");
		log.info("=======================================================================");
		
		
		//Update App Configuration
		//this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration)
		
			return Response.ok(jsonObject).build();
		}catch(Exception ex) {
			log.error("Failed to update Auth application configuration property.", ex);
			return getInternalServerError(ex);		
			
		}
	}
	
	/* ---------------------------------------------------------------------------------------------------------*/
	private JsonObject createJsonObject(AppConfiguration appConfiguration) throws Exception {
		JSONObject json = new JSONObject(appConfiguration);
		String jsonStr = json.toString();
		JsonReader reader = Json.createReader(new StringReader(jsonStr));
		JsonObject jsonObject = reader.readObject();
		return jsonObject;

	}
	
	private JsonValue getJsonValue(String jsonKey, JsonObject jsonObject) throws Exception {
		JsonValue jsonValue = jsonObject.get(jsonKey);
		return jsonValue;

	}
	
	
	
}
