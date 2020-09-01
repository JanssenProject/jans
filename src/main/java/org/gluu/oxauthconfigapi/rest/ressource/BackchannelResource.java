/**
 * Endpoint to configure oxAuth global back-channel configuration.
 */
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
import org.gluu.oxauthconfigapi.rest.model.Backchannel;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

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
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getBackchannelConfiguration() throws IOException {
		Backchannel backchannel = new Backchannel();
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		backchannel.setBackchannelClientId(appConfiguration.getBackchannelClientId());
		backchannel.setBackchannelRedirectUri(appConfiguration.getBackchannelRedirectUri());
		backchannel.setBackchannelAuthenticationEndpoint(appConfiguration.getBackchannelAuthenticationEndpoint());
		backchannel
				.setBackchannelDeviceRegistrationEndpoint(appConfiguration.getBackchannelDeviceRegistrationEndpoint());
		backchannel.setBackchannelTokenDeliveryModesSupported(
				appConfiguration.getBackchannelTokenDeliveryModesSupported());
		backchannel.setBackchannelAuthenticationRequestSigningAlgValuesSupported(
				appConfiguration.getBackchannelAuthenticationRequestSigningAlgValuesSupported());
		backchannel
				.setBackchannelUserCodeParameterSupported(appConfiguration.getBackchannelUserCodeParameterSupported());
		backchannel.setBackchannelBindingMessagePattern(appConfiguration.getBackchannelBindingMessagePattern());
		backchannel.setBackchannelAuthenticationResponseExpiresIn(
				appConfiguration.getBackchannelAuthenticationResponseExpiresIn());
		backchannel.setBackchannelAuthenticationResponseInterval(
				appConfiguration.getBackchannelAuthenticationResponseInterval());
		backchannel.setBackchannelLoginHintClaims(appConfiguration.getBackchannelLoginHintClaims());
		return Response.ok(backchannel).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateBackchannelConfiguration(@Valid Backchannel backchannel) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setBackchannelClientId(backchannel.getBackchannelClientId());
		appConfiguration.setBackchannelRedirectUri(backchannel.getBackchannelRedirectUri());
		appConfiguration.setBackchannelAuthenticationEndpoint(backchannel.getBackchannelAuthenticationEndpoint());
		appConfiguration
				.setBackchannelDeviceRegistrationEndpoint(backchannel.getBackchannelDeviceRegistrationEndpoint());
		appConfiguration
				.setBackchannelTokenDeliveryModesSupported(backchannel.getBackchannelTokenDeliveryModesSupported());
		appConfiguration.setBackchannelAuthenticationRequestSigningAlgValuesSupported(
				backchannel.getBackchannelAuthenticationRequestSigningAlgValuesSupported());
		appConfiguration
				.setBackchannelUserCodeParameterSupported(backchannel.getBackchannelUserCodeParameterSupported());
		appConfiguration.setBackchannelBindingMessagePattern(backchannel.getBackchannelBindingMessagePattern());
		appConfiguration.setBackchannelAuthenticationResponseExpiresIn(
				backchannel.getBackchannelAuthenticationResponseExpiresIn());
		appConfiguration.setBackchannelAuthenticationResponseInterval(
				backchannel.getBackchannelAuthenticationResponseInterval());
		appConfiguration.setBackchannelLoginHintClaims(backchannel.getBackchannelLoginHintClaims());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
