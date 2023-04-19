package io.jans.cacherefresh.api.impl;

import org.slf4j.Logger;
import io.jans.cacherefresh.util.ApiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.jans.cacherefresh.model.config.AppConfiguration;
import io.jans.cacherefresh.service.CacheRefrshConfigurationService;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIGURATION)
@ApplicationScoped
public class CacheRefreshResources  extends BaseWebResource{
	
	@Inject
	private CacheRefrshConfigurationService CacheRefrshConfigurationService;
	
	@Inject
	private Logger logger;
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getCacheRefreshConfigs() {
		log("Get cache Refresh configurations");
		try {

			return Response.ok(CacheRefrshConfigurationService.getCacheRefreshConfiguration()).build();

		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PUT
	public Response updateCacheRefreshConfigs(AppConfiguration CacheRefreshConfiguration) {
		CacheRefrshConfigurationService.updateConfiguration(CacheRefreshConfiguration);
		return Response.ok("success").build();
	}
	
	private void log(String message) {
		logger.debug("################# Request: " + message);
	}
	

}
