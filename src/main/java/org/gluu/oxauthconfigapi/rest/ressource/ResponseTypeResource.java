/**
 * Endpoint to configure the desired response_type values with authorization server.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
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
import org.gluu.oxauth.model.common.ResponseType;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.RESPONSES_TYPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResponseTypeResource extends BaseResource {
	
	@Inject
	Logger log;
	
	@Inject 
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth supported response types")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ResponseType.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSupportedResponseTypes() {
		try {
			log.debug("ResponseTypeResource::getSupportedResponseTypes() - Retrieve oxAuth supported response types");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();			
			Set<Set<ResponseType>> responseTypesSupported = appConfiguration.getResponseTypesSupported();
		  
			return Response.ok(responseTypesSupported).build();
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth supported response types", ex);
			return getInternalServerError(ex);	
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth supported response types")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSupportedResponseTypes(@Valid Set<Set<ResponseType>> responseTypeSet) {
		try {
			log.debug("ResponseTypeResource::updateSupportedResponseTypes() - Update oxAuth supported response types");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			Set<Set<ResponseType>> responseTypesSupported = Sets.newHashSet();
			responseTypesSupported = responseTypeSet;
			
			//Update
			appConfiguration.setResponseTypesSupported(responseTypesSupported);
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
		}catch(Exception ex) {
			log.error("Failed to update oxAuth supported response types", ex);
			return getInternalServerError(ex);	
		}
	}

	
}
