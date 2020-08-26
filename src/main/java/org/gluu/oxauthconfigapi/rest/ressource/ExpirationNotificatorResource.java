package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.ExpirationNotificator;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.EXPIRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExpirationNotificatorResource extends BaseResource {
	
	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve Expiration Notificator configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ExpirationNotificator.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getExpirationNotificatorConfiguration() {
		try {
			log.debug("ExpirationNotificatorResource::getExpirationNotificatorConfiguration() - Retrieve Expiration Notificator configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			ExpirationNotificator expirationNotificator = new ExpirationNotificator();
			expirationNotificator.setExpirationNotificatorEnabled(appConfiguration.getExpirationNotificatorEnabled());
			expirationNotificator.setExpirationNotificatorMapSizeLimit(appConfiguration.getExpirationNotificatorMapSizeLimit());
			expirationNotificator.setExpirationNotificatorIntervalInSeconds(appConfiguration.getExpirationNotificatorIntervalInSeconds());
			
			return Response.ok(expirationNotificator).build();
	
		}catch(Exception ex) {
			log.error("Failed to retrieve Expiration Notificator configuration.", ex);
			return getInternalServerError(ex);		
		}
	}	
	
	@PUT
	@Operation(summary = "Update Expiration Notificator configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateExpirationNotificatorConfiguration(@Valid ExpirationNotificator expirationNotificator) {
		try {
			log.debug("ExpirationNotificatorResource::updateExpirationNotificatorConfiguration() - Update Expiration Notificator configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setExpirationNotificatorEnabled(expirationNotificator.getExpirationNotificatorEnabled());
			appConfiguration.setExpirationNotificatorMapSizeLimit(expirationNotificator.getExpirationNotificatorMapSizeLimit());
			appConfiguration.setExpirationNotificatorIntervalInSeconds(expirationNotificator.getExpirationNotificatorIntervalInSeconds());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
	
		}catch(Exception ex) {
			log.error("Failed to update Expiration Notificator configuration.", ex);
			return getInternalServerError(ex);		
		}
	}

}
