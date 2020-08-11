/**
 * User Info configuration endpoint 
 */
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
import org.gluu.oxauthconfigapi.rest.model.UserInfo;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.USER_INFO)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserInfoResource extends BaseResource {

	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve user info configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserInfo.class, required = true))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUserInfoConfiguration() {
		try {		
			
			log.debug("UserInfoResource::getUserInfoConfiguration() - Retrieve user info configuration");
			UserInfo userInfo = new UserInfo();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			userInfo.setUserInfoSigningAlgValuesSupported(appConfiguration.getUserInfoSigningAlgValuesSupported());
			userInfo.setUserInfoEncryptionAlgValuesSupported(appConfiguration.getUserInfoEncryptionAlgValuesSupported());
			userInfo.setUserInfoEncryptionEncValuesSupported(appConfiguration.getUserInfoEncryptionEncValuesSupported());
			
			return Response.ok(userInfo).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve user info configuration", ex);
			return getInternalServerError(ex);					
		}
	}
	
	
	@PUT
	@Operation(summary = "Update user info configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })	
	public Response updateUserInfoConfiguration(@Valid UserInfo userInfo) {
		try {
			log.debug("UserInfoResource::updateUserInfoConfiguration() - Update user info configuration");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setUserInfoSigningAlgValuesSupported(userInfo.getUserInfoSigningAlgValuesSupported());
			appConfiguration.setUserInfoEncryptionAlgValuesSupported(userInfo.getUserInfoEncryptionAlgValuesSupported());;
			appConfiguration.setUserInfoEncryptionEncValuesSupported(userInfo.getUserInfoEncryptionEncValuesSupported());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();			
			
		}catch(Exception ex) {
			log.error("Failed to update user info configuration", ex);
			return getInternalServerError(ex);				
		}
	}
		
}
