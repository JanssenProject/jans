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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.couchbase.client.core.message.ResponseStatus;

import org.slf4j.Logger;

import org.gluu.config.oxtrust.DbApplicationConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.Fido2Configuration;
import org.gluu.oxauthconfigapi.util.ApiConstants;


@Path(ApiConstants.BASE_API_URL + ApiConstants.FIDO2)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2Resource extends BaseResource {

	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Gets Fido2 configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Fido2Configuration.class, required = false)), description="Success"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getPairwiseConfiguration() {
		log.debug("PairwiseResource::getPairwiseConfiguration() - Retrieve oxAuth Pairwise configuration.");
		try {
			DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
			
			Fido2Configuration fido2Configuration = new Fido2Configuration();
			String fido2ConfigJson = null;
			if (dbApplicationConfiguration != null) {
				fido2ConfigJson = dbApplicationConfiguration.getDynamicConf();
			}
			//fido2Configuration.setAuthenticatorCertsFolder(dbApplicationConfiguration.);
			
			return Response.ok(fido2ConfigJson).build();
			
		} catch (Exception ex) {
			log.error("Failed to fetch oxAuth Pairwise configuration", ex);
			return getInternalServerError(ex);
		}
	}
	
	@PUT
	@Operation(summary = "Updates Fido2 configuration properties.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updatePairwiseConfiguration(@Valid Fido2Configuration fido2Configuration) {
		log.debug("PairwiseResource::updatePairwiseConfiguration() - Updates Fido2 configuration properties.");
		try {
			DbApplicationConfiguration dbApplicationConfiguration = this.jsonConfigurationService.loadFido2Configuration();
			
			
			//this.jsonConfigurationService.saveFido2Configuration("");
			
			return Response.ok(ResponseStatus.SUCCESS).build();

			
		} catch (Exception ex) {
			log.error("Failed to update oxAuth Pairwise configuration", ex);
			return getInternalServerError(ex);
		}
	}

}
