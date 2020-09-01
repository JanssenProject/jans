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
import org.gluu.oxauthconfigapi.rest.model.CleanupConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */
@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.SERVER_CLEANUP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CleanupConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getServerCleanupConfiguration() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		CleanupConfiguration cleanupConfiguration = new CleanupConfiguration();
		cleanupConfiguration.setCleanServiceInterval(appConfiguration.getCleanServiceInterval());
		cleanupConfiguration.setCleanServiceBatchChunkSize(appConfiguration.getCleanServiceBatchChunkSize());
		cleanupConfiguration.setCleanServiceBaseDns(appConfiguration.getCleanServiceBaseDns());
		return Response.ok(cleanupConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateServerCleanupConfiguration(@Valid CleanupConfiguration cleanupConfiguration)
			throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setCleanServiceInterval(cleanupConfiguration.getCleanServiceInterval());
		appConfiguration.setCleanServiceBatchChunkSize(cleanupConfiguration.getCleanServiceBatchChunkSize());
		appConfiguration.setCleanServiceBaseDns(cleanupConfiguration.getCleanServiceBaseDns());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
