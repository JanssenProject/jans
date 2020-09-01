/**
 *  Request object supported endpoint.
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
import org.gluu.oxauthconfigapi.rest.model.RequestObject;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.REQUEST_OBJECT)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RequestObjectResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getRequestObjectConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		RequestObject requestObject = new RequestObject();
		requestObject.setRequestObjectSigningAlgValuesSupported(
				appConfiguration.getRequestObjectSigningAlgValuesSupported());
		requestObject.setRequestObjectEncryptionAlgValuesSupported(
				appConfiguration.getRequestObjectEncryptionAlgValuesSupported());
		requestObject.setRequestObjectEncryptionEncValuesSupported(
				appConfiguration.getRequestObjectEncryptionEncValuesSupported());
		return Response.ok(requestObject).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateRequestObjectConfiguration(@Valid RequestObject requestObject) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration
				.setRequestObjectSigningAlgValuesSupported(requestObject.getRequestObjectSigningAlgValuesSupported());
		appConfiguration.setRequestObjectEncryptionAlgValuesSupported(
				requestObject.getRequestObjectEncryptionAlgValuesSupported());
		appConfiguration.setRequestObjectEncryptionEncValuesSupported(
				requestObject.getRequestObjectEncryptionEncValuesSupported());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}
}
