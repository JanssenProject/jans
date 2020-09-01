/**
 * Endpoint to configure oxAuth PKCS #11 configuration.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import com.couchbase.client.core.message.ResponseStatus;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.JanssenPKCS;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.JANSSENPKCS)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JanssenPKCSResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getJanssenPKCSConfiguration() throws IOException {
			JanssenPKCS janssenPKCS = new JanssenPKCS();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			janssenPKCS.setJanssenPKCSGenerateKeyEndpoint(appConfiguration.getOxElevenGenerateKeyEndpoint());
			janssenPKCS.setJanssenPKCSSignEndpoint(appConfiguration.getOxElevenSignEndpoint());
			janssenPKCS.setJanssenPKCSVerifySignatureEndpoint(appConfiguration.getOxElevenVerifySignatureEndpoint());
			janssenPKCS.setJanssenPKCSDeleteKeyEndpoint(appConfiguration.getOxElevenDeleteKeyEndpoint());
			janssenPKCS.setJanssenPKCSTestModeToken(appConfiguration.getOxElevenTestModeToken());
			return Response.ok(janssenPKCS).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateJanssenPKCSConfiguration(@Valid JanssenPKCS janssenPKCS) throws IOException {
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setOxElevenGenerateKeyEndpoint(janssenPKCS.getJanssenPKCSGenerateKeyEndpoint());
			appConfiguration.setOxElevenSignEndpoint(janssenPKCS.getJanssenPKCSSignEndpoint());
			appConfiguration.setOxElevenVerifySignatureEndpoint(janssenPKCS.getJanssenPKCSVerifySignatureEndpoint());
			appConfiguration.setOxElevenDeleteKeyEndpoint(janssenPKCS.getJanssenPKCSDeleteKeyEndpoint());
			appConfiguration.setOxElevenTestModeToken(janssenPKCS.getJanssenPKCSTestModeToken());
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			return Response.ok(ResponseStatus.SUCCESS).build();
	}
}
