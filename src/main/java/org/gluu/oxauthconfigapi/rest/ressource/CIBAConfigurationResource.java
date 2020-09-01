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
import org.gluu.oxauthconfigapi.rest.model.CIBAConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.CIBA)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CIBAConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getCIBAConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		CIBAConfiguration cibaConfiguration = new CIBAConfiguration();
		cibaConfiguration.setApiKey(appConfiguration.getCibaEndUserNotificationConfig().getApiKey());
		cibaConfiguration.setAuthDomain(appConfiguration.getCibaEndUserNotificationConfig().getAuthDomain());
		cibaConfiguration.setDatabaseURL(appConfiguration.getCibaEndUserNotificationConfig().getDatabaseURL());
		cibaConfiguration.setProjectId(appConfiguration.getCibaEndUserNotificationConfig().getProjectId());
		cibaConfiguration.setStorageBucket(appConfiguration.getCibaEndUserNotificationConfig().getStorageBucket());
		cibaConfiguration
				.setMessagingSenderId(appConfiguration.getCibaEndUserNotificationConfig().getMessagingSenderId());
		cibaConfiguration.setAppId(appConfiguration.getCibaEndUserNotificationConfig().getAppId());
		cibaConfiguration.setNotificationUrl(appConfiguration.getCibaEndUserNotificationConfig().getNotificationUrl());
		cibaConfiguration.setNotificationKey(appConfiguration.getCibaEndUserNotificationConfig().getNotificationKey());
		cibaConfiguration.setPublicVapidKey(appConfiguration.getCibaEndUserNotificationConfig().getPublicVapidKey());
		cibaConfiguration.setCibaGrantLifeExtraTimeSec(appConfiguration.getCibaGrantLifeExtraTimeSec());
		cibaConfiguration.setCibaMaxExpirationTimeAllowedSec(appConfiguration.getCibaMaxExpirationTimeAllowedSec());
		return Response.ok(cibaConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateCIBAConfiguration(@Valid CIBAConfiguration cibaConfiguration) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.getCibaEndUserNotificationConfig().setApiKey(cibaConfiguration.getApiKey());
		appConfiguration.getCibaEndUserNotificationConfig().setAuthDomain(cibaConfiguration.getAuthDomain());
		appConfiguration.getCibaEndUserNotificationConfig().setDatabaseURL(cibaConfiguration.getDatabaseURL());
		appConfiguration.getCibaEndUserNotificationConfig().setProjectId(cibaConfiguration.getProjectId());
		appConfiguration.getCibaEndUserNotificationConfig().setStorageBucket(cibaConfiguration.getStorageBucket());
		appConfiguration.getCibaEndUserNotificationConfig()
				.setMessagingSenderId(cibaConfiguration.getMessagingSenderId());
		appConfiguration.getCibaEndUserNotificationConfig().setAppId(cibaConfiguration.getAppId());
		appConfiguration.getCibaEndUserNotificationConfig().setNotificationUrl(cibaConfiguration.getNotificationUrl());
		appConfiguration.getCibaEndUserNotificationConfig().setNotificationKey(cibaConfiguration.getNotificationKey());
		appConfiguration.getCibaEndUserNotificationConfig().setPublicVapidKey(cibaConfiguration.getPublicVapidKey());
		appConfiguration.setCibaGrantLifeExtraTimeSec(cibaConfiguration.getCibaGrantLifeExtraTimeSec());
		appConfiguration.setCibaMaxExpirationTimeAllowedSec(cibaConfiguration.getCibaMaxExpirationTimeAllowedSec());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}
}
