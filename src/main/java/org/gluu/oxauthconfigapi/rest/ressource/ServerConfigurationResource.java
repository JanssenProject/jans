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
import org.gluu.oxauthconfigapi.rest.model.ServerConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.SERVER_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServerConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getServerConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		ServerConfiguration serverConfiguration = new ServerConfiguration();
		serverConfiguration.setOxId(appConfiguration.getOxId());
		serverConfiguration.setCssLocation(appConfiguration.getCssLocation());
		serverConfiguration.setJsLocation(appConfiguration.getJsLocation());
		serverConfiguration.setImgLocation(appConfiguration.getImgLocation());
		serverConfiguration.setConfigurationUpdateInterval(appConfiguration.getConfigurationUpdateInterval());
		serverConfiguration.setPersonCustomObjectClassList(appConfiguration.getPersonCustomObjectClassList());
		serverConfiguration.setAuthorizationRequestCustomAllowedParameters(
				appConfiguration.getAuthorizationRequestCustomAllowedParameters());
		serverConfiguration.setErrorHandlingMethod(appConfiguration.getErrorHandlingMethod());
		serverConfiguration.setIntrospectionScriptBackwardCompatibility(
				appConfiguration.getIntrospectionScriptBackwardCompatibility());
		serverConfiguration.setFapiCompatibility(appConfiguration.getFapiCompatibility());
		serverConfiguration.setConsentGatheringScriptBackwardCompatibility(
				appConfiguration.getConsentGatheringScriptBackwardCompatibility());
		serverConfiguration.setErrorReasonEnabled(appConfiguration.getErrorReasonEnabled());
		serverConfiguration
				.setClientRegDefaultToCodeFlowWithRefresh(appConfiguration.getClientRegDefaultToCodeFlowWithRefresh());
		serverConfiguration
				.setLogClientIdOnClientAuthentication(appConfiguration.getLogClientIdOnClientAuthentication());
		serverConfiguration
				.setLogClientNameOnClientAuthentication(appConfiguration.getLogClientNameOnClientAuthentication());
		serverConfiguration.setDisableU2fEndpoint(appConfiguration.getDisableU2fEndpoint());
		serverConfiguration.setUseLocalCache(appConfiguration.getUseLocalCache());
		serverConfiguration.setCibaEnabled(appConfiguration.getCibaEnabled());
		return Response.ok(serverConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateServerConfiguration(@Valid ServerConfiguration serverConfiguration) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setOxId(serverConfiguration.getOxId());
		appConfiguration.setCssLocation(serverConfiguration.getCssLocation());
		appConfiguration.setJsLocation(serverConfiguration.getJsLocation());
		appConfiguration.setImgLocation(serverConfiguration.getImgLocation());
		appConfiguration.setConfigurationUpdateInterval(serverConfiguration.getConfigurationUpdateInterval());
		appConfiguration.setPersonCustomObjectClassList(serverConfiguration.getPersonCustomObjectClassList());
		appConfiguration.setAuthorizationRequestCustomAllowedParameters(
				appConfiguration.getAuthorizationRequestCustomAllowedParameters());
		appConfiguration.setErrorHandlingMethod(serverConfiguration.getErrorHandlingMethod());
		appConfiguration.setIntrospectionScriptBackwardCompatibility(
				serverConfiguration.getIntrospectionScriptBackwardCompatibility());
		appConfiguration.setFapiCompatibility(serverConfiguration.getFapiCompatibility());
		appConfiguration.setConsentGatheringScriptBackwardCompatibility(
				serverConfiguration.getConsentGatheringScriptBackwardCompatibility());
		appConfiguration.setErrorReasonEnabled(serverConfiguration.getErrorReasonEnabled());
		appConfiguration.setClientRegDefaultToCodeFlowWithRefresh(
				serverConfiguration.getClientRegDefaultToCodeFlowWithRefresh());
		appConfiguration
				.setLogClientIdOnClientAuthentication(serverConfiguration.getLogClientIdOnClientAuthentication());
		appConfiguration
				.setLogClientNameOnClientAuthentication(serverConfiguration.getLogClientNameOnClientAuthentication());
		appConfiguration.setDisableU2fEndpoint(serverConfiguration.getDisableU2fEndpoint());
		appConfiguration.setUseLocalCache(serverConfiguration.getUseLocalCache());
		appConfiguration.setCibaEnabled(serverConfiguration.getCibaEnabled());

		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
