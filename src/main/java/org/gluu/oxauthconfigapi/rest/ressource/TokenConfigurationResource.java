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
import org.gluu.oxauthconfigapi.rest.model.TokenConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.TOKEN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TokenConfigurationResource extends BaseResource {
	
	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve Token configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TokenConfiguration.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getTokenConfiguration() {
		try {
			log.debug("TokenConfigurationResource::getTokenConfiguration() - Retrieve Token configuration properties.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			TokenConfiguration tokenConfiguration = new TokenConfiguration();
			tokenConfiguration.setPersistRefreshTokenInLdap(appConfiguration.getPersistRefreshTokenInLdap());
			tokenConfiguration.setAuthorizationCodeLifetime(appConfiguration.getAuthorizationCodeLifetime());
			tokenConfiguration.setRefreshTokenLifetime(appConfiguration.getRefreshTokenLifetime());
			tokenConfiguration.setAccessTokenLifetime(appConfiguration.getAccessTokenLifetime());
			
			return Response.ok(tokenConfiguration).build();
	
		}catch(Exception ex) {
			log.error("Failed to retrieve Token configuration properties.", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update Token configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateTokenConfiguration(@Valid TokenConfiguration tokenConfiguration) {
		try {
			log.debug("TokenConfigurationResource::updateTokenConfiguration() - Update Token configuration properties.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			 
			appConfiguration.setPersistRefreshTokenInLdap(tokenConfiguration.getPersistRefreshTokenInLdap());
			appConfiguration.setAuthorizationCodeLifetime(tokenConfiguration.getAuthorizationCodeLifetime());
			appConfiguration.setRefreshTokenLifetime(tokenConfiguration.getRefreshTokenLifetime());
			appConfiguration.setAccessTokenLifetime(tokenConfiguration.getAccessTokenLifetime());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
	
		}catch(Exception ex) {
			log.error("Failed to update Token configuration properties.", ex);
			return getInternalServerError(ex);		
		}
	}

}
