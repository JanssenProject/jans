/**
 * Endpoint to configure the desired response_type values with authorization server.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.Set;
import java.util.HashSet;

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
import org.gluu.oxauthconfigapi.util.ApiConstants;
//import org.gluu.oxauthconfigapi.rest.model.ResponseType;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.RESPONSES_TYPES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResponseTypeResource {
	
	@Inject
	Logger log;
	
	@Inject 
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth supported response types")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = org.gluu.oxauthconfigapi.rest.model.ResponseType.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response getSupportedResponseTypes() {
		Set<Set<String>> responseTypesSupportedSet = Sets.newHashSet();
		Set<String> responseTypes = null;
		try {
			log.info("ResponseTypeResource::getSupportedResponseTypes() - Retrieve oxAuth supported response types");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		
			for (Set<ResponseType> typeSet : appConfiguration.getResponseTypesSupported()) {
				 responseTypes = Sets.newHashSet();
				for(ResponseType type : typeSet) {
					if(type != null)
					  	responseTypes.add(type.name());
				}
	           	
				responseTypesSupportedSet.add(responseTypes);
			}

			return Response.ok(responseTypesSupportedSet).build();
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth supported response types", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth supported response types")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response updateSupportedResponseTypes(@Valid Set<Set<org.gluu.oxauthconfigapi.rest.model.ResponseType>> responseTypeSet) {
		Set<Set<ResponseType>> responseTypesSupportedSet = Sets.newHashSet();
		Set<ResponseType> responseTypes = null;
		try {
			log.info("ResponseTypeResource::updateSupportedResponseTypes() - Update oxAuth supported response types");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			for (Set<org.gluu.oxauthconfigapi.rest.model.ResponseType> types : responseTypeSet) {
				responseTypes = new HashSet<ResponseType>();
				for(org.gluu.oxauthconfigapi.rest.model.ResponseType type : types) {
					ResponseType responseType = ResponseType.fromString(type.getCode());
					responseTypes.add(responseType);
				}
	
				responseTypesSupportedSet.add(responseTypes);
				
				//Update
				appConfiguration.setResponseTypesSupported(responseTypesSupportedSet);
				this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			}
			
			return Response.ok(ResponseStatus.SUCCESS).build();
		}catch(Exception ex) {
			log.error("Failed to update oxAuth supported response types", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	
}
