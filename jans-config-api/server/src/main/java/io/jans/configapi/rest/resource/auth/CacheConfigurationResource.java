/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cache.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.function.Function;

import org.slf4j.Logger;

@Path(ApiConstants.CONFIG + ApiConstants.CACHE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheConfigurationResource extends BaseResource {

    private static final String ERROR_MSG = "Unable to apply patch.";

    @Inject
    Logger logger;

    @Inject
    ConfigurationService configurationService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    private CacheConfiguration loadCacheConfiguration() {
        return configurationService.findGluuConfiguration().getCacheConfiguration();
    }

    private CacheConfiguration mergeModifiedCache(Function<CacheConfiguration, CacheConfiguration> function) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        final CacheConfiguration modifiedCache = function.apply(gluuConfiguration.getCacheConfiguration());
        gluuConfiguration.setCacheConfiguration(modifiedCache);

        persistenceManager.merge(gluuConfiguration);
        return modifiedCache;
    }

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS })
    public Response getCacheConfiguration() {
        return Response.ok(loadCacheConfiguration()).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response patchCacheConfiguration(@NotNull String requestString) {
        logger.debug(" CACHE details to patch - requestString:{}", requestString);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            try {
                return Jackson.applyPatch(requestString, cache);
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException(ERROR_MSG, e);
            }
        });
        return Response.ok(modifiedCache).build();
    }

    @GET
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS })
    public Response getRedisConfiguration() {
        return Response.ok(loadCacheConfiguration().getRedisConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response updateRedisConfiguration(@NotNull RedisConfiguration redisConfiguration) {
        logger.debug("REDIS CACHE details to update - redisConfiguration:{}", redisConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setRedisConfiguration(redisConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getRedisConfiguration()).build();
    }

    @PATCH
    @Path(ApiConstants.REDIS)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response patchRedisConfiguration(@NotNull String requestString) {
        logger.debug("REDIS CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setRedisConfiguration(
                        Jackson.applyPatch(requestString, loadCacheConfiguration().getRedisConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getRedisConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS })
    public Response getInMemoryConfiguration() {
        return Response.ok(loadCacheConfiguration().getInMemoryConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response updateInMemoryConfiguration(@NotNull InMemoryConfiguration inMemoryConfiguration) {
        logger.debug("IN_MEMORY CACHE details to update - inMemoryConfiguration:{}", inMemoryConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setInMemoryConfiguration(inMemoryConfiguration);
            return cache;
        });

        return Response.ok(modifiedCache.getInMemoryConfiguration()).build();
    }

    @PATCH
    @Path(ApiConstants.IN_MEMORY)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response patchInMemoryConfiguration(@NotNull String requestString) {
        logger.debug("IN_MEMORY CACHE details to patch - requestString:{}", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setInMemoryConfiguration(Jackson.applyPatch(requestString, cache.getInMemoryConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getInMemoryConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS })
    public Response getNativePersistenceConfiguration() {
        return Response.ok(loadCacheConfiguration().getNativePersistenceConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response updateNativePersistenceConfiguration(
            @NotNull NativePersistenceConfiguration nativePersistenceConfiguration) {
        logger.debug("NATIVE_PERSISTENCE CACHE details to update - nativePersistenceConfiguration:{}",
                nativePersistenceConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setNativePersistenceConfiguration(nativePersistenceConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getNativePersistenceConfiguration()).build();
    }

    @PATCH
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response patchNativePersistenceConfiguration(@NotNull String requestString) {
        logger.debug("NATIVE_PERSISTENCE CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setNativePersistenceConfiguration(
                        Jackson.applyPatch(requestString, cache.getNativePersistenceConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getNativePersistenceConfiguration()).build();
    }

    @GET
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS })
    public Response getMemcachedConfiguration() {
        return Response.ok(loadCacheConfiguration().getMemcachedConfiguration()).build();
    }

    @PUT
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response updateMemcachedConfiguration(@NotNull MemcachedConfiguration memcachedConfiguration) {
        logger.debug("MEMCACHED CACHE details to update - memcachedConfiguration:{} ", memcachedConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setMemcachedConfiguration(memcachedConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getMemcachedConfiguration()).build();
    }

    @PATCH
    @Path(ApiConstants.MEMCACHED)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS })
    public Response patchMemcachedConfiguration(@NotNull String requestString) {
        logger.debug("MEMCACHED CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setMemcachedConfiguration(Jackson.applyPatch(requestString, cache.getMemcachedConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new RuntimeException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getMemcachedConfiguration()).build();
    }

}
