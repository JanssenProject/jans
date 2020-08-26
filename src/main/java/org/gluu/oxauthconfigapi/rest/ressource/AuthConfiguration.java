package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.*;

import java.io.*;

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

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import javax.json.Json;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPatch;
import javax.json.JsonReader;
import javax.json.JsonObject;

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
		JsonObject jsonObject =  createAppConfigurationJson(appConfiguration);
		return Response.ok(jsonObject).build();
		
		}catch(Exception ex) {
			log.error("Failed to retrieve Auth application configuration.", ex);
			return getInternalServerError(ex);		
		}			
	}
	
		
	@PATCH
	@Path(ApiConstants.JSON_KEY_PATH)
	public Response patchAppConfigurationProperty(@NotNull @PathParam(ApiConstants.JSON_KEY) String jsonKey, @NotNull JsonObject object) {
		log.info("=======================================================================");
		log.info("\n\n jsonKey = "+jsonKey+" , object = "+object+"\n\n");
		try {
		
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		JsonObject jsonObject =  createAppConfigurationJson(appConfiguration);
		log.info("\n\n jsonObject = "+jsonObject+"\n\n");
		boolean isPresent = jsonObject.containsKey(jsonKey);
		log.info("\n\n isPresent = "+isPresent+"\n\n");
		if(isPresent) {
			JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder();
			JsonPatch jsonPatch = jsonPatchBuilder
					.replace("/"+jsonKey,object)
					.build();
			jsonObject = jsonPatch.apply(jsonObject);
		}
		log.info("\n\n jsonObject_2 = "+jsonObject+"\n\n");
		log.info("=======================================================================");
		//Update
		//Convert jsonPatch to 
		//this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration)
		
			return Response.ok(jsonObject).build();
		}catch(Exception ex) {
			log.error("Failed to update Auth application configuration property.", ex);
			return getInternalServerError(ex);		
			
		}
	}
	
	/* ---------------------------------------------------------------------------------------------------------*/
	private JsonObject createAppConfigurationJson(AppConfiguration appConfiguration) throws Exception {
		JSONObject json = new JSONObject(appConfiguration);
		String jsonStr = json.toString();
		InputStream inputStream = new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8));
	    JsonReader jsonReader = Json.createReader(inputStream);
	    JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        inputStream.close();	        
		return jsonObject;
	}
	
	
}
