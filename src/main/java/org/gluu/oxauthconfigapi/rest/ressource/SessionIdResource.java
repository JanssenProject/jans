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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.SessionId;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.SESSIONID)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionIdResource extends BaseResource {
	
	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve session id config settings")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SessionId.class, required = true))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSessionIdConfiguration() {
		try {
			log.debug("SessionIdResource::getSessionIdConfiguration() - Retrieve session id config settings");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			SessionId sessionId = new SessionId();
			sessionId.setSessionIdUnusedLifetime(appConfiguration.getSessionIdUnusedLifetime());
			sessionId.setSessionIdUnauthenticatedUnusedLifetime(appConfiguration.getSessionIdUnauthenticatedUnusedLifetime());
			sessionId.setSessionIdLifetime(appConfiguration.getSessionIdLifetime());
			sessionId.setSessionIdEnabled(appConfiguration.getSessionIdEnabled());
			sessionId.setChangeSessionIdOnAuthentication(appConfiguration.getChangeSessionIdOnAuthentication());
			sessionId.setSessionIdRequestParameterEnabled(appConfiguration.getSessionIdRequestParameterEnabled());
			sessionId.setSessionIdPersistOnPromptNone(appConfiguration.getSessionIdPersistOnPromptNone());
			sessionId.setServerSessionIdLifetime(appConfiguration.getServerSessionIdLifetime());
			
	       return Response.ok(sessionId).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve session id config settings", ex);
			return getInternalServerError(ex);				
		}
	}
	
	
	@PUT
	@Operation(summary = "Retrieve session id config settings")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSessionIdConfiguration(@Valid SessionId sessionId) {
		try {
			log.debug("SessionIdResource::Update() - Update session id config settings");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setSessionIdUnusedLifetime(sessionId.getSessionIdUnusedLifetime());
			appConfiguration.setSessionIdUnauthenticatedUnusedLifetime(sessionId.getSessionIdUnauthenticatedUnusedLifetime());
			appConfiguration.setSessionIdLifetime(sessionId.getSessionIdLifetime());
			appConfiguration.setSessionIdEnabled(sessionId.getSessionIdEnabled());
			appConfiguration.setChangeSessionIdOnAuthentication(sessionId.getChangeSessionIdOnAuthentication());
			appConfiguration.setSessionIdRequestParameterEnabled(sessionId.getSessionIdRequestParameterEnabled());
			appConfiguration.setSessionIdPersistOnPromptNone(sessionId.getSessionIdPersistOnPromptNone());
			appConfiguration.setServerSessionIdLifetime(sessionId.getServerSessionIdLifetime());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
						
		}catch(Exception ex) {
			log.error("Failed to retrieve session id config settings", ex);
			return getInternalServerError(ex);				
		}
	}
}
