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
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.KeyRegenerationConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.KEY_REGENERATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyRegenerationResource extends BaseResource {
	
	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve Key regeneration configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = KeyRegenerationConfiguration.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getKeyRegenerationConfiguration() {
		try {
			log.debug("KeyRegenerationResource::getKeyRegenerationConfiguration() - Retrieve Key regeneration configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			KeyRegenerationConfiguration keyRegenerationConfiguration = new KeyRegenerationConfiguration();
			keyRegenerationConfiguration.setKeyRegenerationEnabled(appConfiguration.getKeyRegenerationEnabled());
			keyRegenerationConfiguration.setKeyRegenerationInterval(appConfiguration.getKeyRegenerationInterval());
			keyRegenerationConfiguration.setDefaultSignatureAlgorithm(appConfiguration.getDefaultSignatureAlgorithm());
			
			return Response.ok(keyRegenerationConfiguration).build();
	
		}catch(Exception ex) {
			log.error("Failed to retrieve Key regeneration configuration.", ex);
			return getInternalServerError(ex);		
		}
	}	
		
	@PUT
	@Operation(summary = "Update Key regeneration configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateKeyRegenerationConfiguration(@Valid KeyRegenerationConfiguration keyRegenerationConfiguration) {
		try {
			log.debug("KeyRegenerationResource::updateKeyRegenerationConfiguration() - Update Key regeneration configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setKeyRegenerationEnabled(keyRegenerationConfiguration.getKeyRegenerationEnabled());
			appConfiguration.setKeyRegenerationInterval(keyRegenerationConfiguration.getKeyRegenerationInterval());
			appConfiguration.setDefaultSignatureAlgorithm(keyRegenerationConfiguration.getDefaultSignatureAlgorithm());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
	
		}catch(Exception ex) {
			log.error("Failed to update Key regeneration configuration.", ex);
			return getInternalServerError(ex);		
		}
	}	

}
