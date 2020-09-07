package org.gluu.configapi.rest.resource;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.cache.RedisConfiguration;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.configapi.filters.ProtectedApi;
import org.gluu.configapi.util.ApiConstants;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.CACHE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheConfigurationResource extends BaseResource {
	
	@Inject
	Logger logger;
	
	@Inject
	JsonConfigurationService jsonConfigurationService;
	

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getCacheConfiguration() {
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
				return Response.ok(cacheConfiguration).build();
	}
	
	@GET
	@Path(ApiConstants.REDIS)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getRedisConfiguration() throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		return Response.ok(cacheConfiguration.getRedisConfiguration()).build();
	}
	
	@POST
	@Path(ApiConstants.REDIS)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response addRedisConfiguration(@NotNull RedisConfiguration redisConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setRedisConfiguration(redisConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.status(Response.Status.CREATED).entity(redisConfiguration).build();
	}
	
	@PUT
	@Path(ApiConstants.REDIS)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateRedisConfiguration(@NotNull RedisConfiguration redisConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setRedisConfiguration(redisConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.ok(redisConfiguration).build();
	}
	

}
