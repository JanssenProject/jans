/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.couchbase.client.java.env.ClusterEnvironment;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Joiner;

import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.CouchbaseConfService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.couchbase.model.CouchbaseConnectionConfiguration;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.COUCHBASE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CouchbaseConfigurationResource extends ConfigBaseResource {

    @Inject
    CouchbaseConfService couchbaseConfService;

    @Operation(summary = "Gets list of existing Couchbase configurations", description = "Gets list of existing Couchbase configurations", operationId = "get-config-database-couchbase", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS  }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = CouchbaseConnectionConfiguration.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response get() {
        return Response.ok(this.couchbaseConfService.findAll()).build();
    }

    @Operation(summary = "Gets a Couchbase configurations by name", description = "Gets a Couchbase configurations by name", operationId = "get-config-database-couchbase-by-name", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CouchbaseConnectionConfiguration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response getWithName(@PathParam(ApiConstants.NAME) String name) {
        logger.debug("CouchbaseConfigurationResource::getWithName() -  name:{}", name);
        return Response.ok(findByName(name)).build();
    }

    @Operation(summary = "Adds a new Couchbase configuration", description = "Adds a new Couchbase configuration", operationId = "post-config-database-couchbase", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS }))
    @RequestBody(description = "CouchbaseConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response add(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        logger.debug("COUCHBASE details to be added - conf:{}", conf);
        couchbaseConfService.save(conf);
        conf = findByName(conf.getConfigId());
        return Response.status(Response.Status.CREATED).entity(conf).build();
    }

    @Operation(summary = "Updates Couchbase configuration", description = "Updates Couchbase configuration", operationId = "put-config-database-couchbase", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS }))
    @RequestBody(description = "CouchbaseConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response update(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        logger.debug("COUCHBASE details to be updated - conf:{}", conf);
        findByName(conf.getConfigId());
        couchbaseConfService.save(conf);
        return Response.ok(conf).build();
    }

    @Operation(summary = "Deletes a Couchbase configurations by name", description = "Deletes a Couchbase configurations by name", operationId = "delete-config-database-couchbase-by-name", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_DELETE_ACCESS }))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.NAME) String name) {
        logger.debug("COUCHBASE to be deleted - name:{}", name);
        findByName(name);
        logger.trace("Delete configuration by name:{} ", name);
        this.couchbaseConfService.remove(name);
        return Response.noContent().build();
    }

    @Operation(summary = "Patches a Couchbase configurations by name", description = "Patches a Couchbase configurations by name", operationId = "patch-config-database-couchbase-by-name", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS  }))
    @RequestBody(description = "JsonPatch object", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response patch(@PathParam(ApiConstants.NAME) String name, @NotNull String jsonPatchString)
            throws JsonPatchException, IOException {
        logger.debug("COUCHBASE to be patched - name:{}, jsonPatchString:{}", name, jsonPatchString);
        CouchbaseConnectionConfiguration conf = findByName(name);
        logger.info("Patch configuration by name:{} ", name);
        conf = Jackson.applyPatch(jsonPatchString, conf);
        couchbaseConfService.save(conf);
        return Response.ok(conf).build();
    }

    @Operation(summary = "Tests a Couchbase configuration", description = "Tests a Couchbase configuration", operationId = "post-config-database-couchbase-test", tags = {
            "Database - Couchbase configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS  }))
    @RequestBody(description = "CouchbaseConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CouchbaseConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(name = "status", type = "boolean", description = "boolean value true if successful"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response test(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        logger.debug("COUCHBASE to be tested - conf:{}", conf);
        Properties properties = new Properties();

        properties.put("couchbase.servers", Joiner.on(",").join(conf.getServers()));
        properties.put("couchbase.auth.userName", conf.getUserName());
        properties.put("couchbase.auth.userPassword", conf.getUserPassword());
        properties.put("couchbase.auth.buckets", Joiner.on(",").join(conf.getBuckets()));
        properties.put("couchbase.bucket.default", conf.getDefaultBucket());
        properties.put("couchbase.password.encryption.method", conf.getPasswordEncryptionMethod());

        ClusterEnvironment.Builder clusterEnvironmentBuilder = ClusterEnvironment.builder();
        ClusterEnvironment clusterEnvironment = clusterEnvironmentBuilder.build();
        logger.error("clusterEnvironment:{}", clusterEnvironment);
        CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(properties,
                clusterEnvironment);
        return Response.ok(connectionProvider.isConnected()).build();
    }

    private CouchbaseConnectionConfiguration findByName(String name) {
        final Optional<CouchbaseConnectionConfiguration> optional = this.couchbaseConfService.findByName(name);
        if (optional.isEmpty()) {
            logger.trace("Could not find configuration by name:{}", name);
            throw new NotFoundException(getNotFoundError("Configuration - '" + name + "'"));
        }
        return optional.get();
    }
}
