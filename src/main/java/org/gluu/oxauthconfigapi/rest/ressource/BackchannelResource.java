/**
 * Endpoint to configure oxAuth global back-channel configuration.
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
import org.gluu.oxauthconfigapi.rest.model.Backchannel;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.BACKCHANNEL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackchannelResource extends BaseResource {

	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth Backchannel configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Backchannel.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getBackchannelConfiguration() {
		try {
			log.debug("BackchannelResource::getBackchannelConfiguration() - Retrieve oxAuth Backchannel configuration");
			Backchannel backchannel = new Backchannel();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			backchannel.setBackchannelClientId(appConfiguration.getBackchannelClientId());
			backchannel.setBackchannelRedirectUri(appConfiguration.getBackchannelRedirectUri());
			backchannel.setBackchannelAuthenticationEndpoint(appConfiguration.getBackchannelAuthenticationEndpoint());
			backchannel.setBackchannelDeviceRegistrationEndpoint(appConfiguration.getBackchannelDeviceRegistrationEndpoint());
			backchannel.setBackchannelTokenDeliveryModesSupported(appConfiguration.getBackchannelTokenDeliveryModesSupported());
			backchannel.setBackchannelAuthenticationRequestSigningAlgValuesSupported(appConfiguration.getBackchannelAuthenticationRequestSigningAlgValuesSupported());
			backchannel.setBackchannelUserCodeParameterSupported(appConfiguration.getBackchannelUserCodeParameterSupported());
			backchannel.setBackchannelBindingMessagePattern(appConfiguration.getBackchannelBindingMessagePattern());
			backchannel.setBackchannelAuthenticationResponseExpiresIn(appConfiguration.getBackchannelAuthenticationResponseExpiresIn());
			backchannel.setBackchannelAuthenticationResponseInterval(appConfiguration.getBackchannelAuthenticationResponseInterval());
			backchannel.setBackchannelLoginHintClaims(appConfiguration.getBackchannelLoginHintClaims());
			
			return Response.ok(backchannel).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth Backchannel configuration", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth Backchannel configuration")
	@APIResponses( value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response  updateBackchannelConfiguration(@Valid Backchannel backchannel ) {
		try {
			log.debug("BackchannelResource::updateBackchannelConfiguration() - Update oxAuth Backchannel configuration");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setBackchannelClientId(backchannel.getBackchannelClientId());
			appConfiguration.setBackchannelRedirectUri(backchannel.getBackchannelRedirectUri());
			appConfiguration.setBackchannelAuthenticationEndpoint(backchannel.getBackchannelAuthenticationEndpoint());
			appConfiguration.setBackchannelDeviceRegistrationEndpoint(backchannel.getBackchannelDeviceRegistrationEndpoint());
			appConfiguration.setBackchannelTokenDeliveryModesSupported(backchannel.getBackchannelTokenDeliveryModesSupported());
			appConfiguration.setBackchannelAuthenticationRequestSigningAlgValuesSupported(backchannel.getBackchannelAuthenticationRequestSigningAlgValuesSupported());
			appConfiguration.setBackchannelUserCodeParameterSupported(backchannel.getBackchannelUserCodeParameterSupported());
			appConfiguration.setBackchannelBindingMessagePattern(backchannel.getBackchannelBindingMessagePattern());
			appConfiguration.setBackchannelAuthenticationResponseExpiresIn(backchannel.getBackchannelAuthenticationResponseExpiresIn());
			appConfiguration.setBackchannelAuthenticationResponseInterval(backchannel.getBackchannelAuthenticationResponseInterval());
			appConfiguration.setBackchannelLoginHintClaims(backchannel.getBackchannelLoginHintClaims());
			
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
			
		}catch(Exception ex) {
			log.error("Failed to update oxAuth Backchannel configuration", ex);
			return getInternalServerError(ex);
			
		}
	}
	
}
