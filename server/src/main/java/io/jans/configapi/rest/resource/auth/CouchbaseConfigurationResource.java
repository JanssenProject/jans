/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.google.common.base.Joiner;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.CouchbaseConfService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;
import io.jans.orm.couchbase.model.CouchbaseConnectionConfiguration;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Properties;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.COUCHBASE)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CouchbaseConfigurationResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    CouchbaseConfService couchbaseConfService;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response get() {
        return Response.ok(this.couchbaseConfService.findAll()).build();
    }

    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response getWithName(@PathParam(ApiConstants.NAME) String name) {
        log.debug("CouchbaseConfigurationResource::getWithName() -  name = "+name+"\n\n");
        return Response.ok(findByName(name)).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response add(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        log.debug("COUCHBASE details to be added - conf = "+conf);
        couchbaseConfService.save(conf);
        conf = findByName(conf.getConfigId());
        return Response.status(Response.Status.CREATED).entity(conf).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response update(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        log.debug("COUCHBASE details to be updated - conf = "+conf);
        findByName(conf.getConfigId());
        couchbaseConfService.save(conf);
        return Response.ok(conf).build();
    }

    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_DELETE_ACCESS })
    public Response delete(@PathParam(ApiConstants.NAME) String name) {
        log.debug("COUCHBASE to be deleted - name = "+name);
        findByName(name);
        log.trace("Delete configuration by name " + name);
        this.couchbaseConfService.remove(name);
        return Response.noContent().build();
    }

    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_WRITE_ACCESS })
    public Response patch(@PathParam(ApiConstants.NAME) String name, @NotNull String requestString) throws Exception {
        log.debug("COUCHBASE to be patched - name = "+name+" , requestString = "+requestString);
        CouchbaseConnectionConfiguration conf = findByName(name);
        log.info("Patch configuration by name " + name);
        conf = Jackson.applyPatch(requestString, conf);
        couchbaseConfService.save(conf);
        return Response.ok(conf).build();
    }

    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_COUCHBASE_READ_ACCESS })
    public Response test(@Valid @NotNull CouchbaseConnectionConfiguration conf) {
        log.debug("COUCHBASE to be tested - conf = "+conf);
        Properties properties = new Properties();

        properties.put("couchbase.servers", Joiner.on(",").join(conf.getServers()));
        properties.put("couchbase.auth.userName", conf.getUserName());
        properties.put("couchbase.auth.userPassword", conf.getUserPassword());
        properties.put("couchbase.auth.buckets", Joiner.on(",").join(conf.getBuckets()));
        properties.put("couchbase.bucket.default", conf.getDefaultBucket());
        properties.put("couchbase.password.encryption.method", conf.getPasswordEncryptionMethod());

        CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider(properties,
                DefaultCouchbaseEnvironment.create());
        return Response.ok(connectionProvider.isConnected()).build();
    }

    private CouchbaseConnectionConfiguration findByName(String name) {
        final Optional<CouchbaseConnectionConfiguration> optional = this.couchbaseConfService.findByName(name);
        if (optional.isEmpty()) {
            log.trace("Could not find configuration by name '" + name + "'");
            throw new NotFoundException(getNotFoundError("Configuration - '" + name + "'"));
        }
        return optional.get();
    }
}
