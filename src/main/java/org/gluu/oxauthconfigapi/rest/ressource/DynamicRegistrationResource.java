package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.DynamicRegistration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.DYN_REGISTRATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DynamicRegistrationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getDynamicRegistration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		DynamicRegistration dynamicRegistration = new DynamicRegistration();
		dynamicRegistration.setDynamicRegistrationEnabled(appConfiguration.getDynamicRegistrationEnabled());
		dynamicRegistration.setDynamicRegistrationPasswordGrantTypeEnabled(
				appConfiguration.getDynamicRegistrationPasswordGrantTypeEnabled());
		dynamicRegistration.setDynamicRegistrationPersistClientAuthorizations(
				appConfiguration.getDynamicRegistrationPersistClientAuthorizations());
		dynamicRegistration
				.setDynamicRegistrationScopesParamEnabled(appConfiguration.getDynamicRegistrationScopesParamEnabled());
		dynamicRegistration
				.setLegacyDynamicRegistrationScopeParam(appConfiguration.getLegacyDynamicRegistrationScopeParam());
		dynamicRegistration
				.setDynamicRegistrationCustomObjectClass(appConfiguration.getDynamicRegistrationCustomObjectClass());
		dynamicRegistration.setDefaultSubjectType(appConfiguration.getDefaultSubjectType());
		dynamicRegistration
				.setDynamicRegistrationExpirationTime(appConfiguration.getDynamicRegistrationExpirationTime());
		dynamicRegistration.setDynamicGrantTypeDefault(appConfiguration.getDynamicGrantTypeDefault());
		dynamicRegistration
				.setDynamicRegistrationCustomAttributes(appConfiguration.getDynamicRegistrationCustomAttributes());
		dynamicRegistration.setTrustedClientEnabled(appConfiguration.getTrustedClientEnabled());
		dynamicRegistration.setReturnClientSecretOnRead(appConfiguration.getReturnClientSecretOnRead());
		return Response.ok(dynamicRegistration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateDynamicConfiguration(@Valid DynamicRegistration dynamicRegistration) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setDynamicRegistrationEnabled(dynamicRegistration.getDynamicRegistrationEnabled());
		appConfiguration.setDynamicRegistrationPasswordGrantTypeEnabled(
				dynamicRegistration.getDynamicRegistrationPasswordGrantTypeEnabled());
		appConfiguration.setDynamicRegistrationPersistClientAuthorizations(
				dynamicRegistration.getDynamicRegistrationPersistClientAuthorizations());
		appConfiguration.setDynamicRegistrationScopesParamEnabled(
				dynamicRegistration.getDynamicRegistrationScopesParamEnabled());
		appConfiguration
				.setLegacyDynamicRegistrationScopeParam(dynamicRegistration.getLegacyDynamicRegistrationScopeParam());
		appConfiguration
				.setDynamicRegistrationCustomObjectClass(dynamicRegistration.getDynamicRegistrationCustomObjectClass());
		appConfiguration.setDefaultSubjectType(dynamicRegistration.getDefaultSubjectType());
		appConfiguration
				.setDynamicRegistrationExpirationTime(dynamicRegistration.getDynamicRegistrationExpirationTime());
		appConfiguration.setDynamicGrantTypeDefault(dynamicRegistration.getDynamicGrantTypeDefault());
		appConfiguration
				.setDynamicRegistrationCustomAttributes(dynamicRegistration.getDynamicRegistrationCustomAttributes());
		appConfiguration.setTrustedClientEnabled(dynamicRegistration.getTrustedClientEnabled());
		appConfiguration.setReturnClientSecretOnRead(dynamicRegistration.getReturnClientSecretOnRead());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(dynamicRegistration).build();
	}

}
