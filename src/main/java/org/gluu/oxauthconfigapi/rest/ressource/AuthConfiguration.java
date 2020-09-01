package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.StringReader;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.json.JSONObject;
import org.slf4j.Logger;

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
	public Response getAppConfiguration() throws Exception {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		JsonObject jsonObject = createJsonObject(appConfiguration);
		return Response.ok(jsonObject).build();
	}

	@PATCH
	@Path(ApiConstants.JSON_KEY_PATH)
	public Response patchAppConfigurationProperty(@NotNull @PathParam(ApiConstants.JSON_KEY) String jsonKey,
			@NotNull JsonObject jsonObject) throws Exception {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		JsonObject appConfigJsonObject = createJsonObject(appConfiguration);
		JsonValue jsonValue = getJsonValue(jsonKey, jsonObject);
		// Apply patch
		JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder();
		JsonPatch jsonPatch = jsonPatchBuilder.replace("/" + jsonKey, jsonValue).build();
		appConfigJsonObject = jsonPatch.apply(appConfigJsonObject);
		return Response.ok(jsonObject).build();
	}

	@SuppressWarnings("resource")
	private JsonObject createJsonObject(AppConfiguration appConfiguration) throws Exception {
		JSONObject json = new JSONObject(appConfiguration);
		String jsonStr = json.toString();
		JsonReader reader = Json.createReader(new StringReader(jsonStr));
		return reader.readObject();
	}

	private JsonValue getJsonValue(String jsonKey, JsonObject jsonObject) throws Exception {
		JsonValue jsonValue = jsonObject.get(jsonKey);
		return jsonValue;
	}

}
