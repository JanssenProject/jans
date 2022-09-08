/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Joiner;
import io.jans.orm.sql.model.SqlConnectionConfiguration;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.service.auth.SqlConfService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.core.util.Jackson;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import org.slf4j.Logger;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.SQL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SqlConfigurationResource extends ConfigBaseResource {

    @Inject
    Logger log;

    @Inject
    SqlConfService sqlConfService;

    @Operation(summary = "Gets list of existing sql configurations", description = "Gets list of existing sql configurations", operationId = "get-config-database-sql", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = SqlConnectionConfiguration.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response get() {
        return Response.ok(this.sqlConfService.findAll()).build();
    }

    @Operation(summary = "Gets a Sql configurations by name", description = "Gets a Sql configurations by name", operationId = "get-config-database-sql-by-name", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_READ_ACCESS }))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response getWithName(@PathParam(ApiConstants.NAME) String name) {
        log.debug("SqlConfigurationResource  -  name:{}",name);
        return Response.ok(findByName(name)).build();
    }

    @Operation(summary = "Adds a new Sql configuration", description = "Adds a new Sql configuration", operationId = "post-config-database-sql", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS }))
    @RequestBody(description = "SqlConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response add(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL details to be added - conf:{} ", conf);
        sqlConfService.save(conf);
        conf = findByName(conf.getConfigId());
        return Response.status(Response.Status.CREATED).entity(conf).build();
    }

    @Operation(summary = "Updates Sql configuration", description = "Updates Sql configuration", operationId = "put-config-database-sql", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS }))
    @RequestBody(description = "SqlConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response update(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL details to be updated - conf:{}", conf);
        findByName(conf.getConfigId());
        sqlConfService.save(conf);
        return Response.ok(conf).build();
    }

    @Operation(summary = "Updates Sql configuration", description = "Updates Sql configuration", operationId = "delete-config-database-sql-by-name", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_DELETE_ACCESS}))
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.NAME) String name) {
        log.debug("SQL to be deleted - name:{} ", name);
        findByName(name);
        log.trace("Delete configuration by name:{}", name);
        this.sqlConfService.remove(name);
        return Response.noContent().build();
    }

    @Operation(summary = "Patch Sql configuration", description = "Patch Sql configuration", operationId = "patch-config-database-sql-by-name", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS }))
    @RequestBody(description = "String representing patch-document.", content = @Content(mediaType = MediaType.APPLICATION_JSON_PATCH_JSON, array = @ArraySchema(schema = @Schema(implementation = JsonPatch.class)), examples = {
            @ExampleObject(value = "[ {op:replace, path: maxConnections, value: 8 } ]") }))
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response patch(@PathParam(ApiConstants.NAME) String name, @NotNull String requestString) throws JsonPatchException, IOException {
        log.debug("SQL to be patched - name :{}, requestString:{} ", name, requestString);
        SqlConnectionConfiguration conf = findByName(name);
        log.info("Patch configuration by name:{} ", name);
        conf = Jackson.applyPatch(requestString, conf);
        sqlConfService.save(conf);
        return Response.ok(conf).build();
    }

    @Operation(summary = "Tests a Sql configuration", description = "Tests a Sql configuration", operationId = "post-config-database-sql-test", tags = {
            "Database - Sql configuration" }, security = @SecurityRequirement(name = "oauth2", scopes = {
                    ApiAccessConstants.DATABASE_SQL_READ_ACCESS}))
    @RequestBody(description = "SqlConnectionConfiguration object", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SqlConnectionConfiguration.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test status", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(name = "status", type = "boolean", description = "boolean value true if successful"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "InternalServerError") })
    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response test(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL to be tested - conf :{}", conf);
        Properties properties = new Properties();

        properties.put("sql.db.schema.name", conf.getSchemaName());
        properties.put("sql.connection.uri", Joiner.on(",").join(conf.getConnectionUri()));

        properties.put("sql.connection.driver-property.serverTimezone", conf.getServerTimezone());
        properties.put("sql.connection.pool.max-total", conf.getConnectionPoolMaxTotal());
        properties.put("sql.connection.pool.max-idle", conf.getConnectionPoolMaxIdle());

        properties.put("sql.auth.userName", conf.getUserName());
        properties.put("sql.auth.userPassword", conf.getUserPassword());

        // Password hash method
        properties.put("sql.password.encryption.method", conf.getPasswordEncryptionMethod());

        // Max time needed to create connection pool in milliseconds
        properties.put("sql.connection.pool.create-max-wait-time-millis", conf.getCreateMaxWaitTimeMillis());

        // Max wait 20 seconds
        properties.put("sql.connection.pool.max-wait-time-millis", conf.getMaxWaitTimeMillis());

        // Allow to evict connection in pool after 30 minutes
        properties.put("sql.connection.pool.min-evictable-idle-time-millis", conf.getMinEvictableIdleTimeMillis());

        properties.put("sql.binaryAttributes", Joiner.on(",").join(conf.getBinaryAttributes()));
        properties.put("sql.certificateAttributes", Joiner.on(",").join(conf.getCertificateAttributes()));

        SqlConnectionProvider connectionProvider = new SqlConnectionProvider(properties);
        return Response.ok(connectionProvider.isConnected()).build();
    }

    private SqlConnectionConfiguration findByName(String name) {
        final Optional<SqlConnectionConfiguration> optional = this.sqlConfService.findByName(name);
        if (!optional.isPresent()) {
            log.trace("Could not find configuration by name:{}", name );
            throw new NotFoundException(getNotFoundError("Configuration - '" + name + "'"));
        }
        return optional.get();
    }
}
