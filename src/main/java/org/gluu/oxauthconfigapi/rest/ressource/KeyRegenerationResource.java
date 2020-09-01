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
import org.gluu.oxauthconfigapi.rest.model.KeyRegenerationConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.KEY_REGENERATION)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyRegenerationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getKeyRegenerationConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		KeyRegenerationConfiguration keyRegenerationConfiguration = new KeyRegenerationConfiguration();
		keyRegenerationConfiguration.setKeyRegenerationEnabled(appConfiguration.getKeyRegenerationEnabled());
		keyRegenerationConfiguration.setKeyRegenerationInterval(appConfiguration.getKeyRegenerationInterval());
		keyRegenerationConfiguration.setDefaultSignatureAlgorithm(appConfiguration.getDefaultSignatureAlgorithm());
		return Response.ok(keyRegenerationConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateKeyRegenerationConfiguration(@Valid KeyRegenerationConfiguration keyRegenerationConfiguration)
			throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setKeyRegenerationEnabled(keyRegenerationConfiguration.getKeyRegenerationEnabled());
		appConfiguration.setKeyRegenerationInterval(keyRegenerationConfiguration.getKeyRegenerationInterval());
		appConfiguration.setDefaultSignatureAlgorithm(keyRegenerationConfiguration.getDefaultSignatureAlgorithm());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();

	}

}
