/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.function.Function;

@Path(ApiConstants.CONFIG + ApiConstants.CACHE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CacheConfigurationResource extends ConfigBaseResource {

    private static final String ERROR_MSG = "Unable to apply patch.";

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

    @Operation(summary = "Returns cache configuration.", description = "Returns cache configuration.", operationId = "get-config-cache", tags = {
            "Cache Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CacheConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getCacheConfiguration() {
        return Response.ok(loadCacheConfiguration()).build();
    }

    @Operation(summary = "Patch cache configuration.", description = "Patch cache configuration", operationId = "patch-config-cache", tags = {
            "Cache Configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CacheConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchCacheConfiguration(@NotNull String requestString) {
        logger.debug(" CACHE details to patch - requestString:{}", requestString);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            try {
                return Jackson.applyPatch(requestString, cache);
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(modifiedCache).build();
    }

    @Operation(summary = "Returns Redis cache configuration.", description = "Returns Redis cache configuration", operationId = "get-config-cache-redis", tags = {
            "Cache Configuration – Redis" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getRedisConfiguration() {
        return Response.ok(loadCacheConfiguration().getRedisConfiguration()).build();
    }

    @Operation(summary = "Updates Redis cache configuration.", description = "Updates Redis cache configuration", operationId = "put-config-cache-redis", tags = {
            "Cache Configuration – Redis" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "RedisConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-redis.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateRedisConfiguration(@NotNull RedisConfiguration redisConfiguration) {
        logger.debug("REDIS CACHE details to update - redisConfiguration:{}", redisConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setRedisConfiguration(redisConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getRedisConfiguration()).build();
    }

    @Operation(summary = "Patch Redis cache configuration.", description = "Patch Redis cache configuration", operationId = "patch-config-cache-redis", tags = {
            "Cache Configuration – Redis" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-redis-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.REDIS)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchRedisConfiguration(@NotNull String requestString) {
        logger.debug("REDIS CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setRedisConfiguration(
                        Jackson.applyPatch(requestString, loadCacheConfiguration().getRedisConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getRedisConfiguration()).build();
    }

    @Operation(summary = "Returns in-Memory cache configuration.", description = "Returns in-Memory cache configuration.", operationId = "get-config-cache-in-memory", tags = {
            "Cache Configuration – in-Memory" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "In-Memory configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InMemoryConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-in-memory.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getInMemoryConfiguration() {
        return Response.ok(loadCacheConfiguration().getInMemoryConfiguration()).build();
    }

    @Operation(summary = "Updates in-Memory cache configuration.", description = "Updates in-Memory cache configuration", operationId = "put-config-cache-in-memory", tags = {
            "Cache Configuration – in-Memory" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "inMemoryConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InMemoryConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-in-memory.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "In-Memory cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InMemoryConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-in-memory.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.IN_MEMORY)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateInMemoryConfiguration(@NotNull InMemoryConfiguration inMemoryConfiguration) {
        logger.debug("IN_MEMORY CACHE details to update - inMemoryConfiguration:{}", inMemoryConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setInMemoryConfiguration(inMemoryConfiguration);
            return cache;
        });

        return Response.ok(modifiedCache.getInMemoryConfiguration()).build();
    }

    @Operation(summary = "Patch In-Memory cache configuration.", description = "Patch In-Memory cache configuration", operationId = "patch-config-cache-in-memory", tags = {
            "Cache Configuration – in-Memory" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-in-memory-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "In-Memory cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InMemoryConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-in-memory.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.IN_MEMORY)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchInMemoryConfiguration(@NotNull String requestString) {
        logger.debug("IN_MEMORY CACHE details to patch - requestString:{}", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setInMemoryConfiguration(Jackson.applyPatch(requestString, cache.getInMemoryConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getInMemoryConfiguration()).build();
    }

    @Operation(summary = "Returns native persistence cache configuration.", description = "Returns native persistence cache configuration.", operationId = "get-config-cache-native-persistence", tags = {
            "Cache Configuration – Native-Persistence" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NativePersistenceConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-native-persistence.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getNativePersistenceConfiguration() {
        return Response.ok(loadCacheConfiguration().getNativePersistenceConfiguration()).build();
    }

    @Operation(summary = "Updates native persistence cache configuration.", description = "Updates native persistence cache configuration", operationId = "put-config-cache-native-persistence", tags = {
            "Cache Configuration – Native-Persistence" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "NativePersistenceConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NativePersistenceConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-native-persistence.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NativePersistenceConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-native-persistence.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
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

    @Operation(summary = "Patch native persistence cache configuration.", description = "Patch native persistence cache configuration", operationId = "patch-config-cache-native-persistence", tags = {
            "Cache Configuration – Native-Persistence" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-native-persistence-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NativePersistenceConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-native-persistence.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.NATIVE_PERSISTENCE)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchNativePersistenceConfiguration(@NotNull String requestString) {
        logger.debug("NATIVE_PERSISTENCE CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setNativePersistenceConfiguration(
                        Jackson.applyPatch(requestString, cache.getNativePersistenceConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getNativePersistenceConfiguration()).build();
    }

    @Operation(summary = "Returns memcached cache configuration.", description = "Returns memcached cache configuration.", operationId = "get-config-cache-memcached", tags = {
            "Cache Configuration – Memcached" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Memcached configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MemcachedConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-memcached.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.CACHE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS })
    public Response getMemcachedConfiguration() {
        return Response.ok(loadCacheConfiguration().getMemcachedConfiguration()).build();
    }

    @Operation(summary = "Updates memcached cache configuration.", description = "Updates memcached cache configuration", operationId = "put-config-cache-memcached", tags = {
            "Cache Configuration – Memcached" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "Memcached Configuration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MemcachedConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-memcached.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MemcachedConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-memcached.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.MEMCACHED)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateMemcachedConfiguration(@NotNull MemcachedConfiguration memcachedConfiguration) {
        logger.debug("MEMCACHED CACHE details to update - memcachedConfiguration:{} ", memcachedConfiguration);
        final CacheConfiguration modifiedCache = mergeModifiedCache(cache -> {
            cache.setMemcachedConfiguration(memcachedConfiguration);
            return cache;
        });
        return Response.ok(modifiedCache.getMemcachedConfiguration()).build();
    }

    @Operation(summary = "Patch memcached cache configuration.", description = "Patch memcached cache configuration", operationId = "patch-config-cache-memcached", tags = {
            "Cache Configuration – Memcached" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.CACHE_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/cache/cache-memcached-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Memcached cache configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MemcachedConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/cache/cache-memcached.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.MEMCACHED)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.CACHE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchMemcachedConfiguration(@NotNull String requestString) {
        logger.debug("MEMCACHED CACHE details to patch - requestString:{} ", requestString);
        mergeModifiedCache(cache -> {
            try {
                cache.setMemcachedConfiguration(Jackson.applyPatch(requestString, cache.getMemcachedConfiguration()));
                return cache;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadCacheConfiguration().getMemcachedConfiguration()).build();
    }

}
