/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.config.oxtrust.DbApplicationConfiguration;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.Fido2Service;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Path(ApiConstants.FIDO2 + ApiConstants.CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Fido2ConfigResource extends BaseResource {

    private static final String FIDO2_CONFIGURATION = "fido2Configuration";
    
    @Inject
    Logger log;

    @Inject
    Fido2Service fido2Service;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.FIDO2_CONFIG_READ_ACCESS })
    public Response getFido2Configuration() throws Exception {
        DbApplicationConfiguration dbApplicationConfiguration = this.fido2Service.find();
        return Response.ok(Jackson.asJsonNode(dbApplicationConfiguration.getDynamicConf())).build();
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.FIDO2_CONFIG_WRITE_ACCESS })
    public Response updateFido2Configuration(@NotNull String fido2ConfigJson) {
        log.debug("FIDO2 details to be updated - fido2ConfigJson = "+fido2ConfigJson);
        checkResourceNotNull(fido2ConfigJson, FIDO2_CONFIGURATION);
        this.fido2Service.merge(fido2ConfigJson);
        return Response.ok(fido2ConfigJson).build();
    }

}