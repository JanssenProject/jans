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
import org.gluu.oxauthconfigapi.rest.model.ServerConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

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
	@Operation(summary = "Retrieve Server's common configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ServerConfiguration.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getServerConfiguration() {
		try {
			log.debug("ServerConfigurationResource::getServerConfiguration() - Retrieve Server's common configuration properties.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			ServerConfiguration serverConfiguration = new ServerConfiguration();
			serverConfiguration.setOxId(appConfiguration.getOxId());
			serverConfiguration.setCssLocation(appConfiguration.getCssLocation());
			serverConfiguration.setJsLocation(appConfiguration.getJsLocation());
			serverConfiguration.setImgLocation(appConfiguration.getImgLocation());
			serverConfiguration.setConfigurationUpdateInterval(appConfiguration.getConfigurationUpdateInterval());
			serverConfiguration.setPersonCustomObjectClassList(appConfiguration.getPersonCustomObjectClassList());
			serverConfiguration.setAuthorizationRequestCustomAllowedParameters(appConfiguration.getAuthorizationRequestCustomAllowedParameters());
			serverConfiguration.setErrorHandlingMethod(appConfiguration.getErrorHandlingMethod());
			serverConfiguration.setIntrospectionScriptBackwardCompatibility(appConfiguration.getIntrospectionScriptBackwardCompatibility());
			serverConfiguration.setFapiCompatibility(appConfiguration.getFapiCompatibility());
			serverConfiguration.setConsentGatheringScriptBackwardCompatibility(appConfiguration.getConsentGatheringScriptBackwardCompatibility());
			serverConfiguration.setErrorReasonEnabled(appConfiguration.getErrorReasonEnabled());
			serverConfiguration.setClientRegDefaultToCodeFlowWithRefresh(appConfiguration.getClientRegDefaultToCodeFlowWithRefresh());
			serverConfiguration.setLogClientIdOnClientAuthentication(appConfiguration.getLogClientIdOnClientAuthentication());
			serverConfiguration.setLogClientNameOnClientAuthentication(appConfiguration.getLogClientNameOnClientAuthentication());
			serverConfiguration.setDisableU2fEndpoint(appConfiguration.getDisableU2fEndpoint());
			serverConfiguration.setUseLocalCache(appConfiguration.getUseLocalCache());
			serverConfiguration.setCibaEnabled(appConfiguration.getCibaEnabled());
			
			return Response.ok(serverConfiguration).build();
	
		}catch(Exception ex) {
			log.error("Failed to retrieve Server's common configuration properties.", ex);
			return getInternalServerError(ex);		
		}
	}
	
	
	@PUT
	@Operation(summary = "Update Server's common configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateServerConfiguration(@Valid ServerConfiguration serverConfiguration) {
		try {
			log.debug("ServerConfigurationResource::updateServerConfiguration() - Update Server's common configuration properties.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setOxId(serverConfiguration.getOxId());
			appConfiguration.setCssLocation(serverConfiguration.getCssLocation());
			appConfiguration.setJsLocation(serverConfiguration.getJsLocation());
			appConfiguration.setImgLocation(serverConfiguration.getImgLocation());
			appConfiguration.setConfigurationUpdateInterval(serverConfiguration.getConfigurationUpdateInterval());
			appConfiguration.setPersonCustomObjectClassList(serverConfiguration.getPersonCustomObjectClassList());
			appConfiguration.setAuthorizationRequestCustomAllowedParameters(appConfiguration.getAuthorizationRequestCustomAllowedParameters());
			appConfiguration.setErrorHandlingMethod(serverConfiguration.getErrorHandlingMethod());
			appConfiguration.setIntrospectionScriptBackwardCompatibility(serverConfiguration.getIntrospectionScriptBackwardCompatibility());
			appConfiguration.setFapiCompatibility(serverConfiguration.getFapiCompatibility());
			appConfiguration.setConsentGatheringScriptBackwardCompatibility(serverConfiguration.getConsentGatheringScriptBackwardCompatibility());
			appConfiguration.setErrorReasonEnabled(serverConfiguration.getErrorReasonEnabled());
			appConfiguration.setClientRegDefaultToCodeFlowWithRefresh(serverConfiguration.getClientRegDefaultToCodeFlowWithRefresh());
			appConfiguration.setLogClientIdOnClientAuthentication(serverConfiguration.getLogClientIdOnClientAuthentication());
			appConfiguration.setLogClientNameOnClientAuthentication(serverConfiguration.getLogClientNameOnClientAuthentication());
			appConfiguration.setDisableU2fEndpoint(serverConfiguration.getDisableU2fEndpoint());
			appConfiguration.setUseLocalCache(serverConfiguration.getUseLocalCache());
			appConfiguration.setCibaEnabled(serverConfiguration.getCibaEnabled());
			
			return Response.ok(ResponseStatus.SUCCESS).build();
	
		}catch(Exception ex) {
			log.error("Failed to update Server's common configuration properties.", ex);
			return getInternalServerError(ex);		
		}
	}

}
