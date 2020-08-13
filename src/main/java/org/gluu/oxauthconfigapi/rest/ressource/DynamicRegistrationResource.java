package org.gluu.oxauthconfigapi.rest.ressource;

import java.util.Set;
import java.util.HashSet;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.common.GrantType;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.DynamicRegistration;
import org.gluu.oxauthconfigapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.DYN_REGISTRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DynamicRegistrationResource extends BaseResource {
	
	@Inject
	Logger log;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	
	@GET
	@Operation(summary = "Retrieve dynamic client registration configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DynamicRegistration.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getDynamicRegistration() {
		try {
			log.debug("DynamicRegistrationResource::getDynamicRegistration() - Retrieve dynamic client registration configuration");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			DynamicRegistration dynamicRegistration = new DynamicRegistration();
			dynamicRegistration.setDynamicRegistrationEnabled(appConfiguration.getDynamicRegistrationEnabled());
			dynamicRegistration.setDynamicRegistrationPasswordGrantTypeEnabled(appConfiguration.getDynamicRegistrationPasswordGrantTypeEnabled());
			dynamicRegistration.setDynamicRegistrationPersistClientAuthorizations(appConfiguration.getDynamicRegistrationPersistClientAuthorizations());
			dynamicRegistration.setDynamicRegistrationScopesParamEnabled(appConfiguration.getDynamicRegistrationScopesParamEnabled());
			dynamicRegistration.setLegacyDynamicRegistrationScopeParam(appConfiguration.getLegacyDynamicRegistrationScopeParam());
			dynamicRegistration.setDynamicRegistrationCustomObjectClass(appConfiguration.getDynamicRegistrationCustomObjectClass());
			dynamicRegistration.setDefaultSubjectType(appConfiguration.getDefaultSubjectType());
			dynamicRegistration.setDynamicRegistrationExpirationTime(appConfiguration.getDynamicRegistrationExpirationTime());
			//dynamicRegistration.setDynamicGrantTypeDefault(appConfiguration.getDynamicGrantTypeDefault());
			dynamicRegistration.setDynamicRegistrationCustomAttributes(appConfiguration.getDynamicRegistrationCustomAttributes());
			if(appConfiguration.getDynamicGrantTypeDefault() != null && !appConfiguration.getDynamicGrantTypeDefault().isEmpty()) {
				Set<String> dynamicGrantTypeDefault = new HashSet<String>();
				for(GrantType grantType : appConfiguration.getDynamicGrantTypeDefault() )
					dynamicGrantTypeDefault.add(grantType.getValue());
				dynamicRegistration.setDynamicGrantTypeDefault(dynamicGrantTypeDefault);
			}
			
        	return Response.ok(dynamicRegistration).build();
						
		}catch(Exception ex) {
			log.error("Failed to retrieve dynamic client registration configuration", ex);
			return getInternalServerError(ex);			
		}
	}
	
	@PUT
	@Operation(summary = "Update dynamic client registration configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateDynamicConfiguration(@Valid DynamicRegistration dynamicRegistration) {
		try {
			log.debug("DynamicConfigurationResource::updateDynamicConfiguration() - Update dynamic client registration configuration");
			
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setDynamicRegistrationEnabled(dynamicRegistration.getDynamicRegistrationEnabled());
			appConfiguration.setDynamicRegistrationPasswordGrantTypeEnabled(dynamicRegistration.getDynamicRegistrationPasswordGrantTypeEnabled());
			appConfiguration.setDynamicRegistrationPersistClientAuthorizations(dynamicRegistration.getDynamicRegistrationPersistClientAuthorizations());
			appConfiguration.setDynamicRegistrationScopesParamEnabled(dynamicRegistration.getDynamicRegistrationScopesParamEnabled());
			appConfiguration.setLegacyDynamicRegistrationScopeParam(dynamicRegistration.getLegacyDynamicRegistrationScopeParam());
			appConfiguration.setDynamicRegistrationCustomObjectClass(dynamicRegistration.getDynamicRegistrationCustomObjectClass());
			appConfiguration.setDefaultSubjectType(dynamicRegistration.getDefaultSubjectType());
			appConfiguration.setDynamicRegistrationExpirationTime(dynamicRegistration.getDynamicRegistrationExpirationTime());
			//appConfiguration.setDynamicGrantTypeDefault(dynamicRegistration.getDynamicGrantTypeDefault());
			appConfiguration.setDynamicRegistrationCustomAttributes(dynamicRegistration.getDynamicRegistrationCustomAttributes());
			if(dynamicRegistration.getDynamicGrantTypeDefault() != null && !dynamicRegistration.getDynamicGrantTypeDefault().isEmpty()) {
				Set<GrantType> dynamicGrantTypeDefault = new HashSet<GrantType>();
				for(String strType : dynamicRegistration.getDynamicGrantTypeDefault() ) {
					GrantType grantType = GrantType.getByValue(strType);
					dynamicGrantTypeDefault.add(grantType);
				}
					
				appConfiguration.setDynamicGrantTypeDefault(dynamicGrantTypeDefault);
			}
			
			//Update
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(dynamicRegistration).build();
			
		}catch(Exception ex) {
			log.error("Failed to update dynamic client registration configuration", ex);
			return getInternalServerError(ex);			
		}
	}
	
}
