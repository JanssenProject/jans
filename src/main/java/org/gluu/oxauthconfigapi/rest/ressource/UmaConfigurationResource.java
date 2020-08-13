/**
 * UMA configuration endpoint
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
import org.gluu.oxauthconfigapi.rest.model.UmaConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.UMA)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UmaConfigurationResource extends BaseResource {
	
	@Inject 
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve UMA configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UmaConfiguration.class, required = true))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getUMAConfiguration() {
		try {
			log.debug("UmaConfigurationResource::getUMAConfiguration() - Retrieve UMA configuration");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			UmaConfiguration umaConfiguration = new UmaConfiguration();
			umaConfiguration.setUmaConfigurationEndpoint(appConfiguration.getUmaConfigurationEndpoint());
			umaConfiguration.setUmaRptLifetime(appConfiguration.getUmaRptLifetime());
			umaConfiguration.setUmaTicketLifetime(appConfiguration.getUmaTicketLifetime());
			umaConfiguration.setUmaPctLifetime(appConfiguration.getUmaPctLifetime());
			umaConfiguration.setUmaResourceLifetime(appConfiguration.getUmaResourceLifetime());
			umaConfiguration.setUmaAddScopesAutomatically(appConfiguration.getUmaAddScopesAutomatically());
			umaConfiguration.setUmaValidateClaimToken(appConfiguration.getUmaValidateClaimToken());
			umaConfiguration.setUmaGrantAccessIfNoPolicies(appConfiguration.getUmaGrantAccessIfNoPolicies());
			umaConfiguration.setUmaRestrictResourceToAssociatedClient(appConfiguration.getUmaRestrictResourceToAssociatedClient());
			umaConfiguration.setUmaRptAsJwt(appConfiguration.getUmaRptAsJwt());
			
			return Response.ok(umaConfiguration).build();					
			
		}catch(Exception ex) {
			log.error("Failed to retrieve UMA configuration", ex);
			return getInternalServerError(ex);				
		}
	}
	
	
	@PUT
	@Operation(summary = "Update UMA configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateUMAConfiguration(@Valid UmaConfiguration umaConfiguration) {
		try {
			log.debug("UmaConfigurationResource::updateUMAConfiguration() - Update UMA configuration");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setUmaConfigurationEndpoint(umaConfiguration.getUmaConfigurationEndpoint());
			appConfiguration.setUmaRptLifetime(umaConfiguration.getUmaRptLifetime());
			appConfiguration.setUmaTicketLifetime(umaConfiguration.getUmaTicketLifetime());
			appConfiguration.setUmaPctLifetime(umaConfiguration.getUmaPctLifetime());
			appConfiguration.setUmaResourceLifetime(umaConfiguration.getUmaResourceLifetime());
			appConfiguration.setUmaAddScopesAutomatically(umaConfiguration.getUmaAddScopesAutomatically());
			appConfiguration.setUmaValidateClaimToken(umaConfiguration.getUmaValidateClaimToken());
			appConfiguration.setUmaGrantAccessIfNoPolicies(umaConfiguration.getUmaGrantAccessIfNoPolicies());
			appConfiguration.setUmaRestrictResourceToAssociatedClient(umaConfiguration.getUmaRestrictResourceToAssociatedClient());
			appConfiguration.setUmaRptAsJwt(umaConfiguration.getUmaRptAsJwt());
						
			//Save
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);

			return Response.ok(ResponseStatus.SUCCESS).build();
			
		}catch(Exception ex) {
			log.error("Failed to update UMA configuration", ex);
			return getInternalServerError(ex);				
		}
	}
}
