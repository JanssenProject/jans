/**
 * Endpoint to configure metrics configuration.
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.ApiError;
import org.gluu.oxauthconfigapi.rest.model.Metrics;
import org.gluu.oxauthconfigapi.util.ApiConstants;

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
	@Operation(summary = "Retrieve oxAuth metric configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Metrics.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getMetricsConfiguration() {
		try {
			log.debug("MetricsResource::getMetricsConfiguration() - Retrieve oxAuth metric configuration");
			Metrics metrics = new Metrics();
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			if (appConfiguration != null) {
				metrics.setMetricReporterEnabled(appConfiguration.getMetricReporterEnabled());
				metrics.setMetricReporterKeepDataDays(appConfiguration.getMetricReporterKeepDataDays());
				metrics.setMetricReporterInterval(appConfiguration.getMetricReporterInterval());
			}
			
			return Response.ok(metrics).build();
		}catch(Exception ex) {
			log.error("Failed to retrieve oxAuth metric configuration", ex);
			return getInternalServerError(ex);		
		}
	}
	
	@PUT
	@Operation(summary = "Update oxAuth metric configuration")
	@APIResponses(value = {
			@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Response.class, required = true, description = "Success"))),
			@APIResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ApiError.class, required = false)) , description = "Unauthorized"),
			@APIResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ApiError.class)), description = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateMetricsConfiguration(@Valid Metrics metrics) {		
		try {
			log.debug("MetricsResource::updateMetricsConfiguration() - Update oxAuth metric configuration");
			AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
			appConfiguration.setMetricReporterEnabled(metrics.getMetricReporterEnabled());
			appConfiguration.setMetricReporterKeepDataDays(metrics.getMetricReporterKeepDataDays());
			appConfiguration.setMetricReporterInterval(metrics.getMetricReporterInterval());
			
			this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
			
			return Response.ok(ResponseStatus.SUCCESS).build();
			
		}catch(Exception ex) {
			log.error("Failed to update oxAuth metric  configuration", ex);
			return getInternalServerError(ex);		
		}
	}
	
	

}
