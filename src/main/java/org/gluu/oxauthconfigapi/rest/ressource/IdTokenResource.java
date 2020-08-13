/**
 * Endpoint to configure id token settings.
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
import org.gluu.oxauthconfigapi.rest.model.IdToken;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES +  ApiConstants.IDTOKEN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdTokenResource extends BaseResource {

	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve idToken config settings")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = IdToken.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getIdTokenConfiguration() {
		try {
			log.debug("IdTokenResource::getIdTokenConfiguration() - Retrieve idToken config settings");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			IdToken idToken = new IdToken();
			idToken.setIdTokenSigningAlgValuesSupported(appConfiguration.getIdTokenSigningAlgValuesSupported());
			idToken.setIdTokenEncryptionAlgValuesSupported(appConfiguration.getIdTokenEncryptionAlgValuesSupported());
			idToken.setIdTokenEncryptionEncValuesSupported(appConfiguration.getIdTokenEncryptionEncValuesSupported());
			
			return Response.ok(idToken).build();			
			
		}catch(Exception ex) {
			log.error("Failed to retrieve idToken config settings", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update idToken config settings")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateIdTokenConfiguration(@Valid IdToken idToken) {
		try {
			log.debug("IdTokenResource::getIdTokenConfiguration() - Update idToken config settings");
						
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setIdTokenSigningAlgValuesSupported(idToken.getIdTokenSigningAlgValuesSupported());
			appConfiguration.setIdTokenEncryptionAlgValuesSupported(idToken.getIdTokenEncryptionAlgValuesSupported());
			appConfiguration.setIdTokenEncryptionEncValuesSupported(idToken.getIdTokenEncryptionEncValuesSupported());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
		}catch(Exception ex) {
			log.error("Failed to update idToken config settings", ex);
			return getInternalServerError(ex);				
		}
	}

}
