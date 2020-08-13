package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.configuration.CorsConfigurationFilter;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.util.ApiConstants;


@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.CORS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CorsConfigurationFilterResource extends BaseResource {
	
	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieves list of oxAuth Cors configuration filters.")
	@APIResponses( value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = List.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getCorsConfigurationFilters() {
		
		log.debug("CorsConfigurationFilterResource::getCorsConfigurationFilters() - Retrieves list of oxAuth Cors configuration filters.");
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			List<CorsConfigurationFilter> corsConfigurationFilters = appConfiguration.getCorsConfigurationFilters();
			
			return Response.ok(corsConfigurationFilters).build();
					
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth Cors configuration filters.", ex);
			return getInternalServerError(ex);	
		}
	}
	
	
	@PUT
	@Operation(summary = "Updates list of oxAuth Cors configuration filters.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = List.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateCorsConfigurationFilters(@Valid List<CorsConfigurationFilter> corsConfigurationFilters) {
		
		log.debug("CorsConfigurationFilterResource::updateCorsConfigurationFilters() - Updates list of oxAuth Cors configuration filters. - corsConfigurationFilters = "+corsConfigurationFilters);
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.getCorsConfigurationFilters().clear();
			appConfiguration.getCorsConfigurationFilters().addAll(corsConfigurationFilters);
			
			return Response.ok(corsConfigurationFilters).build();
					
		}catch(Exception ex) {
			log.error("Failed to update oxAuth Cors configuration filters.", ex);
			return getInternalServerError(ex);	
		}
	}
}
