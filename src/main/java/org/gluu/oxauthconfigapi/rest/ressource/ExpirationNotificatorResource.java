package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ExpirationNotificator;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.EXPIRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExpirationNotificatorResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getExpirationNotificatorConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		ExpirationNotificator expirationNotificator = new ExpirationNotificator();
		expirationNotificator.setExpirationNotificatorEnabled(appConfiguration.getExpirationNotificatorEnabled());
		expirationNotificator
				.setExpirationNotificatorMapSizeLimit(appConfiguration.getExpirationNotificatorMapSizeLimit());
		expirationNotificator.setExpirationNotificatorIntervalInSeconds(
				appConfiguration.getExpirationNotificatorIntervalInSeconds());
		return Response.ok(expirationNotificator).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateExpirationNotificatorConfiguration(@Valid ExpirationNotificator expirationNotificator)
			throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setExpirationNotificatorEnabled(expirationNotificator.getExpirationNotificatorEnabled());
		appConfiguration
				.setExpirationNotificatorMapSizeLimit(expirationNotificator.getExpirationNotificatorMapSizeLimit());
		appConfiguration.setExpirationNotificatorIntervalInSeconds(
				expirationNotificator.getExpirationNotificatorIntervalInSeconds());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
