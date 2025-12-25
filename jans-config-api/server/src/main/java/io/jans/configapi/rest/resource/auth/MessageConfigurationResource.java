/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import java.io.IOException;
import java.util.function.Function;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.config.GluuConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.core.util.Jackson;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.message.model.config.MessageConfiguration;
import io.jans.service.message.model.config.PostgresMessageConfiguration;
import io.jans.service.message.model.config.RedisMessageConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Configuration endpoints for messages
 *
 * @author Yuriy Movchan Date: 07/12/2023
 */
@Path(ApiConstants.CONFIG + ApiConstants.MESSAGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MessageConfigurationResource extends ConfigBaseResource {

    private static final String ERROR_MSG = "Unable to apply patch.";

    @Inject
    ConfigurationService configurationService;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    private MessageConfiguration loadMessageConfiguration() {
        return configurationService.findGluuConfiguration().getMessageConfiguration();
    }

    /**
     * Apply a transformation to the current MessageConfiguration and persist the updated configuration.
     *
     * @param function a function that receives the current MessageConfiguration and returns the modified MessageConfiguration
     * @return the modified MessageConfiguration
     */
    private MessageConfiguration mergeModifiedMessage(Function<MessageConfiguration, MessageConfiguration> function) {
        final GluuConfiguration gluuConfiguration = configurationService.findGluuConfiguration();

        final MessageConfiguration modifiedMessage = function.apply(gluuConfiguration.getMessageConfiguration());
        gluuConfiguration.setMessageConfiguration(modifiedMessage);

        persistenceManager.merge(gluuConfiguration);
        return modifiedMessage;
    }

    /**
     * Retrieve the current global MessageConfiguration.
     *
     * @return the current MessageConfiguration in the response body (HTTP 200)
     */
    @Operation(summary = "Returns message configuration.", description = "Returns message configuration.", operationId = "get-config-message", tags = {
            "Message Configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.MESSAGE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getMessageConfiguration() {
        return Response.ok(loadMessageConfiguration()).build();
    }

    /**
     * Applies a JSON Patch document to the global MessageConfiguration and persists the change.
     *
     * @param requestString a JSON Patch document (application/json-patch+json) as a string
     * @return a JAX-RS Response containing the updated MessageConfiguration
     * @throws InternalServerErrorException if the patch cannot be applied or an I/O error occurs
     */
    @Operation(summary = "Patch message configuration.", description = "Patch message configuration", operationId = "patch-config-message", tags = {
            "Message Configuration" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }), })
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/message/message-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.MESSAGE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchMessageConfiguration(@NotNull String requestString) {
        logger.debug(" MESSAGE details to patch - requestString:{}", requestString);
        final MessageConfiguration modifiedMessage = mergeModifiedMessage(message -> {
            try {
                return Jackson.applyPatch(requestString, message);
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(modifiedMessage).build();
    }

    /**
     * Retrieve the Redis section of the global MessageConfiguration.
     *
     * @return a Response whose entity is the current RedisMessageConfiguration.
     */
    @Operation(summary = "Returns Redis message configuration.", description = "Returns Redis message configuration", operationId = "get-config-message-redis", tags = {
            "Message Configuration – Redis" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.MESSAGE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getRedisMessageConfiguration() {
        return Response.ok(loadMessageConfiguration().getRedisConfiguration()).build();
    }

    /**
     * Update the global Redis message configuration used by the application.
     *
     * @param redisConfiguration the RedisMessageConfiguration to apply to the global MessageConfiguration
     * @return the updated RedisMessageConfiguration
     */
    @Operation(summary = "Updates Redis message configuration.", description = "Updates Redis message configuration", operationId = "put-config-message-redis", tags = {
            "Message Configuration – Redis" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }), })
    @RequestBody(description = "RedisMessageConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisMessageConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/message/message-redis.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.REDIS)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.MESSAGE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updateRedisMessageConfiguration(@NotNull RedisMessageConfiguration redisConfiguration) {
        logger.debug("REDIS MESSAGE details to update - redisConfiguration:{}", redisConfiguration);
        final MessageConfiguration modifiedMessage = mergeModifiedMessage(message -> {
            message.setRedisConfiguration(redisConfiguration);
            return message;
        });
        return Response.ok(modifiedMessage.getRedisConfiguration()).build();
    }

    /**
     * Applies a JSON Patch to the stored RedisMessageConfiguration and returns the updated configuration.
     *
     * @param requestString JSON Patch document as a string (media type application/json-patch+json) describing modifications to apply
     * @return the updated RedisMessageConfiguration after applying the patch
     */
    @Operation(summary = "Patch Redis message configuration.", description = "Patch Redis message configuration", operationId = "patch-config-message-redis", tags = {
            "Message Configuration – Redis" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }), })
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/message/message-redis-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Redis message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RedisMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.REDIS)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.MESSAGE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchRedisMessageConfiguration(@NotNull String requestString) {
        logger.debug("REDIS MESSAGE details to patch - requestString:{} ", requestString);
        mergeModifiedMessage(message -> {
            try {
                message.setRedisConfiguration(
                        Jackson.applyPatch(requestString, loadMessageConfiguration().getRedisConfiguration()));
                return message;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadMessageConfiguration().getRedisConfiguration()).build();
    }

    /**
     * Retrieve the Postgres message configuration.
     *
     * @return the current PostgresMessageConfiguration.
     */
    @Operation(summary = "Returns Postgres message configuration.", description = "Returns Postgres message configuration.", operationId = "get-config-message-postgres", tags = {
            "Message Configuration – Postgres" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PostgresMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.POSTGRES)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_READ_ACCESS }, groupScopes = {
            ApiAccessConstants.MESSAGE_WRITE_ACCESS }, superScopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS,
                    ApiAccessConstants.SUPER_ADMIN_READ_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response getPostgresConfiguration() {
        return Response.ok(loadMessageConfiguration().getPostgresConfiguration()).build();
    }

    /**
     * Replace the Postgres message configuration with the provided configuration.
     *
     * Persists the supplied PostgresMessageConfiguration into the global MessageConfiguration and returns the stored configuration.
     *
     * @param postgresMessageConfiguration the new PostgresMessageConfiguration to store; must not be null
     * @return the persisted PostgresMessageConfiguration
     */
    @Operation(summary = "Updates Postgres message configuration.", description = "Updates Postgres message configuration", operationId = "put-config-message-postgres", tags = {
            "Message Configuration – Postgres" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }), })
    @RequestBody(description = "PostgresMessageConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PostgresMessageConfiguration.class), examples = @ExampleObject(name = "Request json example", value = "example/message/message-redis.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PostgresMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @Path(ApiConstants.POSTGRES)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.MESSAGE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response updatePostgresMessageConfiguration(
            @NotNull PostgresMessageConfiguration postgresMessageConfiguration) {
        logger.debug("POSTGRES_PERSISTENCE MESSAGE details to update - postgresMessageConfiguration:{}",
                postgresMessageConfiguration);
        final MessageConfiguration modifiedMessage = mergeModifiedMessage(message -> {
            message.setPostgresConfiguration(postgresMessageConfiguration);
            return message;
        });
        return Response.ok(modifiedMessage.getPostgresConfiguration()).build();
    }

    /**
     * Apply a JSON Patch to the Postgres message configuration.
     *
     * @param requestString JSON Patch document (application/json-patch+json) as a string.
     * @return the updated PostgresMessageConfiguration.
     * @throws InternalServerErrorException if the patch cannot be parsed or applied.
     */
    @Operation(summary = "Patch Postgres message configuration.", description = "Patch Postgres message configuration", operationId = "patch-config-message-postgres", tags = {
            "Message Configuration – Postgres" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.MESSAGE_ADMIN_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS }), })
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = @ExampleObject(name = "Request json example", value = "example/message/message-redis-patch.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Native persistence message configuration details", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PostgresMessageConfiguration.class), examples = @ExampleObject(name = "Response json example", value = "example/message/message-redis.json"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.POSTGRES)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.MESSAGE_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            ApiAccessConstants.MESSAGE_ADMIN_ACCESS, ApiAccessConstants.SUPER_ADMIN_WRITE_ACCESS })
    public Response patchPostgresMessageConfiguration(@NotNull String requestString) {
        logger.debug("POSTGRES_PERSISTENCE MESSAGE details to patch - requestString:{} ", requestString);
        mergeModifiedMessage(message -> {
            try {
                message.setPostgresConfiguration(
                        Jackson.applyPatch(requestString, message.getPostgresConfiguration()));
                return message;
            } catch (IOException | JsonPatchException e) {
                throw new InternalServerErrorException(ERROR_MSG, e);
            }
        });
        return Response.ok(loadMessageConfiguration().getPostgresConfiguration()).build();
    }

}