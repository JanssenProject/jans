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
import org.gluu.oxauthconfigapi.rest.model.SessionId;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.SESSIONID)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionIdResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSessionIdConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		SessionId sessionId = new SessionId();
		sessionId.setSessionIdUnusedLifetime(appConfiguration.getSessionIdUnusedLifetime());
		sessionId.setSessionIdUnauthenticatedUnusedLifetime(
				appConfiguration.getSessionIdUnauthenticatedUnusedLifetime());
		sessionId.setSessionIdLifetime(appConfiguration.getSessionIdLifetime());
		sessionId.setSessionIdEnabled(appConfiguration.getSessionIdEnabled());
		sessionId.setChangeSessionIdOnAuthentication(appConfiguration.getChangeSessionIdOnAuthentication());
		sessionId.setSessionIdRequestParameterEnabled(appConfiguration.getSessionIdRequestParameterEnabled());
		sessionId.setSessionIdPersistOnPromptNone(appConfiguration.getSessionIdPersistOnPromptNone());
		sessionId.setServerSessionIdLifetime(appConfiguration.getServerSessionIdLifetime());
		return Response.ok(sessionId).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSessionIdConfiguration(@Valid SessionId sessionId) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setSessionIdUnusedLifetime(sessionId.getSessionIdUnusedLifetime());
		appConfiguration
				.setSessionIdUnauthenticatedUnusedLifetime(sessionId.getSessionIdUnauthenticatedUnusedLifetime());
		appConfiguration.setSessionIdLifetime(sessionId.getSessionIdLifetime());
		appConfiguration.setSessionIdEnabled(sessionId.getSessionIdEnabled());
		appConfiguration.setChangeSessionIdOnAuthentication(sessionId.getChangeSessionIdOnAuthentication());
		appConfiguration.setSessionIdRequestParameterEnabled(sessionId.getSessionIdRequestParameterEnabled());
		appConfiguration.setSessionIdPersistOnPromptNone(sessionId.getSessionIdPersistOnPromptNone());
		appConfiguration.setServerSessionIdLifetime(sessionId.getServerSessionIdLifetime());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}
}
