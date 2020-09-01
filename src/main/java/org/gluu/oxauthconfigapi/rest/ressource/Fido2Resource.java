package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.Fido2Configuration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Path(ApiConstants.FIDO2 + ApiConstants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2Resource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getFido2Configuration() {
		Fido2Configuration fido2Configuration = new Fido2Configuration();
		String fido2ConfigJson = null;
		DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
		if (dbApplicationConfiguration != null) {
			fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
			Gson gson = new Gson();
			JsonElement jsonElement = gson.fromJson(fido2ConfigJson, JsonElement.class);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			JsonElement fido2ConfigurationElement = jsonObject.get("fido2Configuration");
			fido2Configuration = gson.fromJson(fido2ConfigurationElement, Fido2Configuration.class);
		}
		return Response.ok(fido2Configuration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateFido2Configuration(@Valid Fido2Configuration fido2Configuration) {
		DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
		if (dbApplicationConfiguration != null) {
			String fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
			Gson gson = new Gson();
			JsonElement jsonElement = gson.fromJson(fido2ConfigJson, JsonElement.class);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			JsonElement updatedElement = gson.toJsonTree(fido2Configuration);
			jsonObject.add("fido2Configuration", updatedElement);
			this.jsonConfigurationService.saveFido2Configuration(jsonObject.toString());
		}
		return Response.ok(fido2Configuration).build();
	}

}
