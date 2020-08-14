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

import org.gluu.oxtrust.service.ConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.AuthenticationMethod;
import org.gluu.oxauthconfigapi.util.ApiConstants;


/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.ACRS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationMethodResource extends BaseResource {

	@Inject
	Logger log;
	
	@Inject 
	private ConfigurationService configurationService;
	
	@GET
	@Operation(summary = "Returns default authentication method.")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AuthenticationMethod.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
    public Response getDefaultAuthenticationMethod() {
		log.info("AuthenticationMethodResource::getDefaultAuthenticationMethod() - Retrieve oxAuth default authentication configuration.");
		try {
			
			AuthenticationMethod authenticationMethod = new AuthenticationMethod();
			authenticationMethod.setDefaultAcr(this.configurationService.getConfiguration().getAuthenticationMode());
			authenticationMethod.setOxtrustAcr(this.configurationService.getConfiguration().getOxTrustAuthenticationMode());
		
			return Response.ok(authenticationMethod).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth default authentication configuration.", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update default authentication method.")
	@APIResponses( value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	 public Response updateDefaultAuthenticationMethod(@Valid AuthenticationMethod authenticationMethod) {
		log.info("AuthenticationMethodResource::updateDefaultAuthenticationMethod() - Update default authentication method configuration.");
			try {
								
				this.configurationService.getConfiguration().setAuthenticationMode(authenticationMethod.getDefaultAcr());
				this.configurationService.getConfiguration().setOxTrustAuthenticationMode(authenticationMethod.getOxtrustAcr());
				
				return Response.ok(ResponseStatus.SUCCESS).build();
				
			}catch(Exception ex) {
				log.error("Failed to update default authentication method configuration", ex);
				return getInternalServerError(ex);		
			}
		}
	
}