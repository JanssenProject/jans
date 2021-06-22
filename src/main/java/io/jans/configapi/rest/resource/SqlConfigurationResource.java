/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import com.google.common.base.Joiner;
import io.jans.orm.sql.model.SqlConnectionConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.SqlConfService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Properties;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.SQL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SqlConfigurationResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    SqlConfService sqlConfService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response get() {
        return Response.ok(this.sqlConfService.findAll()).build();
    }

    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response getWithName(@PathParam(ApiConstants.NAME) String name) {
        log.debug("SqlConfigurationResource::getWithName() -  name = "+name+"\n\n");
        return Response.ok(findByName(name)).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response add(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL details to be added - conf = "+conf);
        sqlConfService.save(conf);
        conf = findByName(conf.getConfigId());
        return Response.status(Response.Status.CREATED).entity(conf).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response update(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL details to be updated - conf = "+conf);
        findByName(conf.getConfigId());
        sqlConfService.save(conf);
        return Response.ok(conf).build();
    }

    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.NAME) String name) {
        log.debug("SQL to be deleted - name = "+name);
        findByName(name);
        log.trace("Delete configuration by name " + name);
        this.sqlConfService.remove(name);
        return Response.noContent().build();
    }

    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_WRITE_ACCESS })
    public Response patch(@PathParam(ApiConstants.NAME) String name, @NotNull String requestString) throws Exception {
        log.debug("SQL to be patched - name = "+name+" , requestString = "+requestString);
        SqlConnectionConfiguration conf = findByName(name);
        log.info("Patch configuration by name " + name);
        conf = Jackson.applyPatch(requestString, conf);
        sqlConfService.save(conf);
        return Response.ok(conf).build();
    }

    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_SQL_READ_ACCESS })
    public Response test(@Valid @NotNull SqlConnectionConfiguration conf) {
        log.debug("SQL to be tested - conf = "+conf);
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
            log.trace("Could not find configuration by name '" + name + "'");
            throw new NotFoundException(getNotFoundError("Configuration - '" + name + "'"));
        }
        return optional.get();
    }
}
