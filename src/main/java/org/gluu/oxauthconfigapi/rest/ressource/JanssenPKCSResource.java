/**
 * Endpoint to configure oxAuth PKCS #11 configuration.
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
import org.gluu.oxauthconfigapi.rest.model.JanssenPKCS;
import org.gluu.oxauthconfigapi.util.ApiConstants;


/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.JANSSENPKCS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JanssenPKCSResource {
	
	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve oxAuth PKCS #11 configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JanssenPKCS.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response getJanssenPKCSConfiguration() {
		try {		
			
			log.info("JanssenPKCSResource::getJanssenPKCSConfiguration() - Retrieve oxAuth JanssenPKCS configuration");
			JanssenPKCS janssenPKCS = new JanssenPKCS();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			janssenPKCS.setJanssenPKCSGenerateKeyEndpoint(appConfiguration.getOxElevenGenerateKeyEndpoint());
			janssenPKCS.setJanssenPKCSSignEndpoint(appConfiguration.getOxElevenSignEndpoint());
			janssenPKCS.setJanssenPKCSVerifySignatureEndpoint(appConfiguration.getOxElevenVerifySignatureEndpoint());
			janssenPKCS.setJanssenPKCSDeleteKeyEndpoint(appConfiguration.getOxElevenDeleteKeyEndpoint());
			janssenPKCS.setJanssenPKCSTestModeToken(appConfiguration.getOxElevenTestModeToken());			
			
			return Response.ok(janssenPKCS).build();
			
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth JanssenPKCS configuration", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();			
		}
	}

	
	@Path("/updateTest")
	@GET
	@Operation(summary = "Update oxAuth PKCS #11 configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true))),
			@APIResponse(responseCode = "500", description = "Server error") })
	public Response updateJanssenPKCSConfiguration() {
		
		try {
			log.info("JanssenPKCSResource::updateJanssenPKCSConfiguration() - Update oxAuth JanssenPKCS configuration");
			JanssenPKCS janssenPKCS = updateJanssenPKCS(); //For testing only
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			
			appConfiguration.setOxElevenGenerateKeyEndpoint(janssenPKCS.getJanssenPKCSGenerateKeyEndpoint());
			appConfiguration.setOxElevenSignEndpoint(janssenPKCS.getJanssenPKCSSignEndpoint());
			appConfiguration.setOxElevenVerifySignatureEndpoint(janssenPKCS.getJanssenPKCSVerifySignatureEndpoint());
			appConfiguration.setOxElevenDeleteKeyEndpoint(janssenPKCS.getJanssenPKCSDeleteKeyEndpoint());
			appConfiguration.setOxElevenTestModeToken(janssenPKCS.getJanssenPKCSTestModeToken());
			log.info("JanssenPKCSResource::updateJanssenPKCSConfiguration() - Update oxAuth JanssenPKCS configuration");
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			log.info("JanssenPKCSResource::updateJanssenPKCSConfiguration() - Update oxAuth JanssenPKCS configuration");
			
			return Response.ok(ResponseStatus.SUCCESS).build();
			
			
		}catch(Exception ex) {
			log.error("Failed to update oxAuth JanssenPKCS configuration", ex);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();			
		}
		
	}
	
	
	private JanssenPKCS getJanssenPKCS() throws Exception{	
		
		JanssenPKCS janssenPKCS = new JanssenPKCS();
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		
		janssenPKCS.setJanssenPKCSGenerateKeyEndpoint(appConfiguration.getOxElevenGenerateKeyEndpoint());
		janssenPKCS.setJanssenPKCSSignEndpoint(appConfiguration.getOxElevenSignEndpoint());
		janssenPKCS.setJanssenPKCSVerifySignatureEndpoint(appConfiguration.getOxElevenVerifySignatureEndpoint());
		janssenPKCS.setJanssenPKCSDeleteKeyEndpoint(appConfiguration.getOxElevenDeleteKeyEndpoint());
		janssenPKCS.setJanssenPKCSTestModeToken(appConfiguration.getOxElevenTestModeToken());	
		
		return janssenPKCS;
	}
	
	private JanssenPKCS updateJanssenPKCS() throws Exception{
		JanssenPKCS janssenPKCS = new JanssenPKCS();
				
		janssenPKCS.setJanssenPKCSGenerateKeyEndpoint("https://pujavs2.infinity.com/oxeleven/rest/oxeleven/generateKey");
		janssenPKCS.setJanssenPKCSSignEndpoint("https://pujavs2.infinity.com/oxeleven/rest/oxeleven/sign");
		janssenPKCS.setJanssenPKCSVerifySignatureEndpoint("https://pujavs2.infinity.com/oxeleven/rest/oxeleven/verifySignature");
		janssenPKCS.setJanssenPKCSDeleteKeyEndpoint("https://pujavs2.infinity.com/oxeleven/rest/oxeleven/deleteKey");
		
		return janssenPKCS;
	}
	
	
	

}
