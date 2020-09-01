/**
 * Endpoint to configure Response Mode values that the OP supports.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.common.ResponseMode;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/*
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.RESPONSES_MODES)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ResponseModeResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSupportedResponseMode() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		Set<ResponseMode> responseModesSupported = appConfiguration.getResponseModesSupported();
		return Response.ok(responseModesSupported).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSupportedResponseMode(@Valid Set<ResponseMode> responseModes) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		Set<ResponseMode> responseModesSupported = responseModes;
		appConfiguration.setResponseModesSupported(responseModesSupported);
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
