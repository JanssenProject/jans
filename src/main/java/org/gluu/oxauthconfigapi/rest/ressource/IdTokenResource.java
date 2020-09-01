/**
 * Endpoint to configure id token settings.
 */
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
import org.gluu.oxauthconfigapi.rest.model.IdToken;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.IDTOKEN)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IdTokenResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getIdTokenConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		IdToken idToken = new IdToken();
		idToken.setIdTokenSigningAlgValuesSupported(appConfiguration.getIdTokenSigningAlgValuesSupported());
		idToken.setIdTokenEncryptionAlgValuesSupported(appConfiguration.getIdTokenEncryptionAlgValuesSupported());
		idToken.setIdTokenEncryptionEncValuesSupported(appConfiguration.getIdTokenEncryptionEncValuesSupported());
		return Response.ok(idToken).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateIdTokenConfiguration(@Valid IdToken idToken) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setIdTokenSigningAlgValuesSupported(idToken.getIdTokenSigningAlgValuesSupported());
		appConfiguration.setIdTokenEncryptionAlgValuesSupported(idToken.getIdTokenEncryptionAlgValuesSupported());
		appConfiguration.setIdTokenEncryptionEncValuesSupported(idToken.getIdTokenEncryptionEncValuesSupported());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
