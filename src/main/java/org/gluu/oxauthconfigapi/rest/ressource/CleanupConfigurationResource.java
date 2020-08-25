package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
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
import org.gluu.oxauthconfigapi.rest.model.CleanupConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.SERVER_CLEANUP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CleanupConfigurationResource extends BaseResource {
	
	@Inject
	Logger log;
		
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve Server clean-up configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CleanupConfiguration.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getServerCleanupConfiguration() {
		try {
			log.debug("CleanupConfigurationResource::getServerCleanupConfiguration() - Retrieve Server clean-up configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			CleanupConfiguration cleanupConfiguration = new CleanupConfiguration();
			cleanupConfiguration.setCleanServiceInterval(appConfiguration.getCleanServiceInterval());
			cleanupConfiguration.setCleanServiceBatchChunkSize(appConfiguration.getCleanServiceBatchChunkSize());
			cleanupConfiguration.setCleanServiceBaseDns(appConfiguration.getCleanServiceBaseDns());
			
			return Response.ok(cleanupConfiguration).build();
	
		}catch(Exception ex) {
			log.error("Failed to retrieve Server clean-up configuration.", ex);
			return getInternalServerError(ex);		
		}
	}	
	
	@PUT
	@Operation(summary = "Update Server clean-up configuration.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = true, description = "Unauthorized"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateServerCleanupConfiguration(@Valid CleanupConfiguration cleanupConfiguration) {
		try {
			log.debug("CleanupConfigurationResource::updateServerCleanupConfiguration() - Update Server clean-up configuration.");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();			
			appConfiguration.setCleanServiceInterval(cleanupConfiguration.getCleanServiceInterval());
			appConfiguration.setCleanServiceBatchChunkSize(cleanupConfiguration.getCleanServiceBatchChunkSize());
			appConfiguration.setCleanServiceBaseDns(cleanupConfiguration.getCleanServiceBaseDns());
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
	
		}catch(Exception ex) {
			log.error("Failed to update Server clean-up configuration.", ex);
			return getInternalServerError(ex);		
		}
	}
		

}
