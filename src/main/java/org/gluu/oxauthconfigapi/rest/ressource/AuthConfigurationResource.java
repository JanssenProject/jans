package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.StringReader;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import org.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.patch.PatchOperation;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getAppConfiguration() {
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			JSONObject json = new JSONObject(appConfiguration);
			return Response.ok(json).build();

		} catch (Exception ex) {
			log.error("Failed to retrieve Auth application configuration.", ex);
			return getInternalServerError(ex);
		}
	}


	@PATCH
	@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	@ProtectedApi(scopes = { WRITE_ACCESS }) 
	public Response patchAppConfigurationProperty(@NotNull JsonPatch jsonPatch) { 
		log.info( "=======================================================================");
		log.info("\n\n jsonPatch = "+jsonPatch+"\n\n"); 
		try {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration(); 
			JSONObject json = new JSONObject(appConfiguration);
			log.info("\n\n appConfiguration_before = "+json+"\n\n"); 
			appConfiguration = applyPatchToConfig(jsonPatch,appConfiguration); 
			json = new JSONObject(appConfiguration);
			log.info("\n\n appConfiguration_after = "+json+"\n\n");
			
		
			log.info("=======================================================================");
			
			
			//Update App Configuration
			//this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration)
			
			return Response.ok(json).build(); 
		}catch(Exception ex) {
			log.error("Failed to update Auth application configuration property.", ex);
			return getInternalServerError(ex);
		
		} 
	}

	
	
	/*
	 * @PATCH
	 * 
	 * @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
	 * // @Produces(MediaType.APPLICATION_JSON)
	 * 
	 * @ProtectedApi(scopes = { WRITE_ACCESS }) public Response
	 * patchAppConfigurationProperty(@NotNull PatchOperation patchOperation) {
	 * log.info(
	 * "=======================================================================");
	 * log.info("\n\n patchOperation = " + patchOperation + "\n\n"); try {
	 * 
	 * AppConfiguration appConfiguration =
	 * this.jsonConfigurationService.getOxauthAppConfiguration(); JSONObject json =
	 * new JSONObject(appConfiguration); log.info("\n\n appConfiguration_before = "
	 * + json + "\n\n");
	 * 
	 * appConfiguration = applyPatchToConfig(jsonPatch, appConfiguration); json =
	 * new JSONObject(appConfiguration); log.info("\n\n appConfiguration_after = " +
	 * json + "\n\n");
	 * 
	 * 
	 * 
	 * PatchOperation patchOperation = new PatchOperation(); AuthConfiguration
	 * authConfiguration = (AuthConfiguration) appConfiguration; json = new
	 * JSONObject(authConfiguration);
	 * log.info("\n\n authConfiguration_before = "+json+"\n\n"); authConfiguration =
	 * patchOperation.applyPatchToConfig(jsonPatch,authConfiguration);
	 * appConfiguration = (AppConfiguration) authConfiguration; json = new
	 * JSONObject(appConfiguration);
	 * log.info("\n\n appConfiguration_after = "+json+"\n\n");
	 * 
	 * 
	 * log.info(
	 * "=======================================================================");
	 * 
	 * // Update App Configuration //
	 * this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration)
	 * 
	 * return Response.ok(json).build(); } catch (Exception ex) {
	 * log.error("Failed to update Auth application configuration property.", ex);
	 * return getInternalServerError(ex);
	 * 
	 * } }
	 */
	
	/*
	 * PatchOperation patchOperation = new PatchOperation(); AuthConfiguration
	 * authConfiguration = (AuthConfiguration) appConfiguration; json = new
	 * JSONObject(authConfiguration);
	 * log.info("\n\n authConfiguration_before = "+json+"\n\n"); authConfiguration =
	 * patchOperation.applyPatchToConfig(jsonPatch,authConfiguration);
	 * appConfiguration = (AppConfiguration) authConfiguration; json = new
	 * JSONObject(appConfiguration);
	 * log.info("\n\n appConfiguration_after = "+json+"\n\n");
	 */
	

	private AppConfiguration applyPatchToConfig(JsonPatch jsonPatch, AppConfiguration appConfiguration)
			throws JsonPatchException, JsonProcessingException, Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode patched = jsonPatch.apply(objectMapper.convertValue(appConfiguration, JsonNode.class));
		return objectMapper.treeToValue(patched, AppConfiguration.class);
	}

}
