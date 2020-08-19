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
import org.gluu.oxauthconfigapi.rest.model.GrantTypes;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES +  ApiConstants.GRANT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public class GrantTypeResource extends BaseResource {


	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth supported Grant Type configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = GrantTypes.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getGrantTypeConfiguration() {
		try {
			log.debug("GrantTypeResource::getGrantTypeConfiguration() - Retrieve oxAuth supported Grant Type.");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			GrantTypes grantTypes = new GrantTypes();
			grantTypes.setGrantTypesSupported(appConfiguration.getGrantTypesSupported());
			grantTypes.setDynamicGrantTypeDefault(appConfiguration.getDynamicGrantTypeDefault());
			
			
			return Response.ok(grantTypes).build();			
			
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth supported Grant Type configuration.", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth supported Grant Type configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateGrantTypeConfiguration(@Valid GrantTypes grantTypes) {
		try {
			log.debug("GrantTypeResource::updateGrantTypeConfiguration() - Update oxAuth supported Grant Type configuration.");
						
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setGrantTypesSupported(grantTypes.getGrantTypesSupported());
			appConfiguration.setDynamicGrantTypeDefault(grantTypes.getDynamicGrantTypeDefault());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
		}catch(Exception ex) {
			log.error("Failed to update oxAuth supported Grant Type configuration", ex);
			return getInternalServerError(ex);				
		}
	}

}
