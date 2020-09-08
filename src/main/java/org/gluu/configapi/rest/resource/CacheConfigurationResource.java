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
import org.gluu.service.cache.InMemoryConfiguration;
import org.gluu.service.cache.NativePersistenceConfiguration;
import org.gluu.service.cache.MemcachedConfiguration;
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
	
	@GET
	@Path(ApiConstants.IN_MEMORY)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getInMemoryConfiguration() throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		return Response.ok(cacheConfiguration.getInMemoryConfiguration()).build();
	}

	@POST
	@Path(ApiConstants.IN_MEMORY)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response addInMemoryConfiguration(@NotNull InMemoryConfiguration inMemoryConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setInMemoryConfiguration(inMemoryConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.status(Response.Status.CREATED).entity(inMemoryConfiguration).build();
	}
	
	@PUT
	@Path(ApiConstants.IN_MEMORY)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateInMemoryConfiguration(@NotNull InMemoryConfiguration inMemoryConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setInMemoryConfiguration(inMemoryConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.ok(inMemoryConfiguration).build();
	}
	
	@GET
	@Path(ApiConstants.NATIVE_PERSISTENCE)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getNativePersistenceConfiguration() throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		return Response.ok(cacheConfiguration.getNativePersistenceConfiguration()).build();
	}
	
	@POST
	@Path(ApiConstants.NATIVE_PERSISTENCE)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response addNativePersistenceConfiguration(@NotNull NativePersistenceConfiguration nativePersistenceConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setNativePersistenceConfiguration(nativePersistenceConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.status(Response.Status.CREATED).entity(nativePersistenceConfiguration).build();
	}
	
	@PUT
	@Path(ApiConstants.NATIVE_PERSISTENCE)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateNativePersistenceConfiguration(@NotNull NativePersistenceConfiguration nativePersistenceConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setNativePersistenceConfiguration(nativePersistenceConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.ok(nativePersistenceConfiguration).build();
	}
	
	@GET
	@Path(ApiConstants.MEMCACHED)
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getMemcachedConfiguration() throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		return Response.ok(cacheConfiguration.getMemcachedConfiguration()).build();
	}	
	
	@POST
	@Path(ApiConstants.MEMCACHED)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response addMemcachedConfiguration(@NotNull MemcachedConfiguration memcachedConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setMemcachedConfiguration(memcachedConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.status(Response.Status.CREATED).entity(memcachedConfiguration).build();
	}
		
	@PUT
	@Path(ApiConstants.MEMCACHED)
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateMemcachedConfiguration(@NotNull MemcachedConfiguration memcachedConfiguration) throws Exception{
		CacheConfiguration cacheConfiguration = this.jsonConfigurationService.getOxMemCacheConfiguration();
		cacheConfiguration.setMemcachedConfiguration(memcachedConfiguration);
		this.jsonConfigurationService.saveOxMemCacheConfiguration(cacheConfiguration);
		return Response.ok(memcachedConfiguration).build();
	}
	
}
