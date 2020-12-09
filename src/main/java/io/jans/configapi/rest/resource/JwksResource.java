/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.as.model.config.Conf;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 */
@Path(ApiConstants.CONFIG + ApiConstants.JWKS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JwksResource extends BaseResource {

    @Inject
    ConfigurationService configurationService;

    @GET
    @ProtectedApi(scopes = {ApiAccessConstants.JWKS_READ_ACCESS})
    public Response get() {
        final String json = configurationService.findConf().getWebKeys().toString();
        return Response.ok(json).build();
    }

    @PUT
    @ProtectedApi(scopes = {ApiAccessConstants.JWKS_WRITE_ACCESS})
    public Response put(WebKeysConfiguration webkeys) {
        final Conf conf = configurationService.findConf();
        conf.setWebKeys(webkeys);
        configurationService.merge(conf);
        return Response.ok().build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = {ApiAccessConstants.JWKS_WRITE_ACCESS})
    public Response patch(String requestString) throws JsonPatchException, IOException {
        final Conf conf = configurationService.findConf();
        WebKeysConfiguration webKeys = conf.getWebKeys();
        webKeys = Jackson.applyPatch(requestString, webKeys);
        conf.setWebKeys(webKeys);
        configurationService.merge(conf);
        return Response.ok(webKeys.toString()).build();
    }
}
