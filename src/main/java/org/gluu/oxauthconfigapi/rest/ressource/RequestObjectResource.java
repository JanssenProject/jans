/**
 * 
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
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.rest.model.RequestObject;
import org.gluu.oxauthconfigapi.util.ApiConstants;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.REQUEST_OBJECT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RequestObjectResource {

	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve request object configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RequestObject.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response getRequestObjectConfiguration() {
		try {
			log.info("RequestObjectResource::getRequestObjectConfiguration() - Retrieve request object settings");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			RequestObject requestObject = new RequestObject();
			requestObject.setRequestObjectSigningAlgValuesSupported(appConfiguration.getRequestObjectSigningAlgValuesSupported());
			requestObject.setRequestObjectEncryptionAlgValuesSupported(appConfiguration.getRequestObjectEncryptionAlgValuesSupported());
			requestObject.setRequestObjectEncryptionEncValuesSupported(appConfiguration.getRequestObjectEncryptionEncValuesSupported());

			return Response.ok(requestObject).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve request object settings", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();			
		}
	}
	
	@Path("/updateTest")
	@GET
	@Operation(summary = "Update request object configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response updateRequestObjectConfiguration() {
		try {
			log.info("RequestObjectResource::updateRequestObjectConfiguration() - Update request object settings");
			RequestObject requestObject = getRequestObject();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setRequestObjectSigningAlgValuesSupported(requestObject.getRequestObjectSigningAlgValuesSupported());
			appConfiguration.setRequestObjectEncryptionAlgValuesSupported(requestObject.getRequestObjectEncryptionAlgValuesSupported());
			appConfiguration.setRequestObjectEncryptionEncValuesSupported(requestObject.getRequestObjectEncryptionEncValuesSupported());

			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			return Response.ok(requestObject).build();
			
		}catch(Exception ex) {
			log.error("Failed to update request object settings", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();			
		}
	}
	
	
	private RequestObject getRequestObject() throws Exception{
		RequestObject requestObject = new RequestObject();
		String[] signingAlgValuesSupported = {"none","HS256","HS384","HS512","RS256","RS384","RS512","ES256","ES384","ES512"};
		String[] encryptionAlgValuesSupported = {"RSA1_5","RSA-OAEP","A128KW","A256KW"};
		String[] encryptionEncValuesSupported = {"A128CBC+HS256","A256CBC+HS512","A128GCM","A256GCM"};
				
		requestObject.setRequestObjectSigningAlgValuesSupported(java.util.Arrays.asList(signingAlgValuesSupported));
		requestObject.setRequestObjectEncryptionAlgValuesSupported(java.util.Arrays.asList(encryptionAlgValuesSupported));
		requestObject.setRequestObjectEncryptionEncValuesSupported(java.util.Arrays.asList(encryptionEncValuesSupported));
		
		return requestObject;
	}
}
