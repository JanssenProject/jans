package io.jans.cacherefresh.api.impl;

import java.util.concurrent.atomic.AtomicBoolean;

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
import io.jans.cacherefresh.timer.CacheRefreshTimer;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIGURATION)
@ApplicationScoped
public class CacheRefreshResources  extends BaseWebResource{
	
	@Inject
	private CacheRefrshConfigurationService cacheRefrshConfigurationService;
	
	@Inject
	private CacheRefreshTimer cacheRefreshTimer;
	
	@Inject
	private Logger logger;
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getCacheRefreshConfigs() {
		log("Get cache Refresh configurations");
		try {

			return Response.ok(cacheRefrshConfigurationService.getCacheRefreshConfiguration()).build();

		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/trigger")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response triggerCacheRefreshConfigs() {
		log("Get cache Refresh configurations");
		try {
			cacheRefreshTimer.setIsActive(new AtomicBoolean(true));
			cacheRefreshTimer.processInt();

			return Response.ok("{\r\n"
					+ "    \"message\": \"success\"\r\n"
					+ "}").build();

		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}finally {
			cacheRefreshTimer.setIsActive(new AtomicBoolean(false));
		}
	}
	
	@PUT
	public Response updateCacheRefreshConfigs(AppConfiguration CacheRefreshConfiguration) {
		cacheRefrshConfigurationService.updateConfiguration(CacheRefreshConfiguration);
		return Response.ok("success").build();
	}
	
	private void log(String message) {
		logger.debug("################# Request: " + message);
	}
	

}
