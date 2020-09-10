package org.gluu.configapi.rest.resource;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.rest.model.Fido2Configuration;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiConstants.FIDO2 + ApiConstants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ConfigResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	@Counted(name = "fetchFido2ConfigurationInvocations", description = "Counting the invocations of the fido2 configuration endpoint.")
	@Metered(name = "fido2ConfigurationRetrieve", unit = MetricUnits.SECONDS, description = "Metrics to monitor fido2 configuration retrieval.", absolute = true)
	@Timed(name = "fetchFido2Configuration-time", description = "Metrics to monitor time to fetch fido2 configuration.", unit = MetricUnits.MINUTES, absolute = true)
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
	@Counted(name = "updateFido2ConfigurationInvocations", description = "Counting the invocations of the fido2 update configuration endpoint.")
	@Metered(name = "fido2ConfigurationUpdate", unit = MetricUnits.SECONDS, description = "Metrics to monitor fido2 configuration change.s", absolute = true)
	@Timed(name = "updateFido2Configuration-time", description = "Metrics to monitor time to change fido2 configuration.", unit = MetricUnits.MINUTES, absolute = true)
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