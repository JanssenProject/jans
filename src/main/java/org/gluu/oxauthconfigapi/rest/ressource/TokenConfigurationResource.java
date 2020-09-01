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
import org.gluu.oxauthconfigapi.rest.model.TokenConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.TOKEN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TokenConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getTokenConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		TokenConfiguration tokenConfiguration = new TokenConfiguration();
		tokenConfiguration.setPersistRefreshTokenInLdap(appConfiguration.getPersistRefreshTokenInLdap());
		tokenConfiguration.setAuthorizationCodeLifetime(appConfiguration.getAuthorizationCodeLifetime());
		tokenConfiguration.setRefreshTokenLifetime(appConfiguration.getRefreshTokenLifetime());
		tokenConfiguration.setAccessTokenLifetime(appConfiguration.getAccessTokenLifetime());
		return Response.ok(tokenConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateTokenConfiguration(@Valid TokenConfiguration tokenConfiguration) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setPersistRefreshTokenInLdap(tokenConfiguration.getPersistRefreshTokenInLdap());
		appConfiguration.setAuthorizationCodeLifetime(tokenConfiguration.getAuthorizationCodeLifetime());
		appConfiguration.setRefreshTokenLifetime(tokenConfiguration.getRefreshTokenLifetime());
		appConfiguration.setAccessTokenLifetime(tokenConfiguration.getAccessTokenLifetime());
		// Update
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
