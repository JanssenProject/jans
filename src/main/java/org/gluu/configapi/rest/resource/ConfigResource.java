package org.gluu.configapi.rest.resource;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;
import org.gluu.configapi.util.Jackson;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.OXAUTH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@Counted(name = "fetchAppConfigurationInvocations", description = "Counting the invocations of the application configuration endpoint.", displayName = "fetchAppConfigurationInvocations")
	@Metered(name = "applicationConfigurationRetrieve", unit = MetricUnits.SECONDS, description = "Metrics to monitor application configuration retrieval.", absolute = true)
	@Timed(name = "fetchApplicationConfiguration-time", description = "Metrics to monitor time to change application configuration.", unit = MetricUnits.MINUTES, absolute = true)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAppConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		return Response.ok(appConfiguration).build();

	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	@Counted(name = "patchAppConfigurationInvocations", description = "Counting the patch invocations of the application configuration change endpoint.", displayName = "patchAppConfigurationInvocations")
	@Metered(name = "applicationConfigurationChanges", unit = MetricUnits.SECONDS, description = "Metrics to monitor application configuration changes.", absolute = true)
	@Timed(name = "patchApplicationConfiguration-time", description = "Metrics to monitor time to change application configuration.", unit = MetricUnits.MINUTES, absolute = true)
	public Response patchAppConfigurationProperty(@NotNull String requestString) throws Exception {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration = Jackson.applyPatch(requestString, appConfiguration);
		jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(appConfiguration).build();

	}
}
