/**
 * Endpoint to configure metrics configuration.
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
import org.gluu.oxauthconfigapi.rest.model.Metrics;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.METRICS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getMetricsConfiguration() throws IOException {
		Metrics metrics = new Metrics();
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		if (appConfiguration != null) {
			metrics.setMetricReporterEnabled(appConfiguration.getMetricReporterEnabled());
			metrics.setMetricReporterKeepDataDays(appConfiguration.getMetricReporterKeepDataDays());
			metrics.setMetricReporterInterval(appConfiguration.getMetricReporterInterval());
		}
		return Response.ok(metrics).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateMetricsConfiguration(@Valid Metrics metrics) throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setMetricReporterEnabled(metrics.getMetricReporterEnabled());
		appConfiguration.setMetricReporterKeepDataDays(metrics.getMetricReporterKeepDataDays());
		appConfiguration.setMetricReporterInterval(metrics.getMetricReporterInterval());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}
