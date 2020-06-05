/**
 * Service to configure Response Mode values that the OP supports.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;

import com.google.common.collect.Sets;

import com.couchbase.client.core.message.ResponseStatus;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.common.ResponseMode;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.util.ApiConstants;
//import org.gluu.oxauthconfigapi.rest.model.ResponseMode;


/*
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.RESPONSES_MODES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ResponseModeResource {
	
	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth supported response modes")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = org.gluu.oxauthconfigapi.rest.model.ResponseMode.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response getSupportedResponseMode() {
		Set<String> responseModesSupported = Sets.newHashSet();
		try {
			log.info("ResponseTypeResource::getSupportedResponseMode() - Retrieve oxAuth supported response modes");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		
			for(ResponseMode responseMode : appConfiguration.getResponseModesSupported())
				responseModesSupported.add(responseMode.name());				
				
			return Response.ok(responseModesSupported).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth supported response modes", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth supported response modes")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response updateSupportedResponseMode(@Valid Set<org.gluu.oxauthconfigapi.rest.model.ResponseMode> responseModes) {
		
		try {
			log.info("ResponseTypeResource::updateSupportedResponseMode() - Update oxAuth supported response modes");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			Set<ResponseMode> responseModesSupported = Sets.newHashSet();
			for(org.gluu.oxauthconfigapi.rest.model.ResponseMode mode : responseModes){
				ResponseMode responseMode = ResponseMode.getByValue(mode.getValue());
				responseModesSupported.add(responseMode);
			}
			
			//Update
			appConfiguration.setResponseModesSupported(responseModesSupported);
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
			
		}catch(Exception ex) {
			log.error("Failed to update oxAuth supported response modes", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	
}
